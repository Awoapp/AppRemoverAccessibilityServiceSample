/*
 * Created by Muhsin Ilhan for AccesibiliyService project at 2.02.2024 23:13.
 * For questions and assistance requests,
 * Email: c.awoapp@gmail.com
 * GitHub: https://github.com/Awoapp
 * Fiverr: https://www.fiverr.com/awoapp
 *
 */

package com.android.accesibiliyservice.Activities;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AppOpsManager;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.android.accesibiliyservice.Adapters.AppListAdapter;
import com.android.accesibiliyservice.Models.AppModel;
import com.android.accesibiliyservice.R;
import com.android.accesibiliyservice.Services.RemoveAppService;
import com.android.accesibiliyservice.Interfaces.SendAppList;
import com.android.accesibiliyservice.databinding.LayoutMainBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// MainActivity class that manages app list and interacts with the user for app removal
public class MainActivity extends AppCompatActivity implements SendAppList {
    LayoutMainBinding binding; // Binding for interacting with the layout
    ArrayList<AppModel> appModelList; // List of all user-installed apps
    ArrayList<AppModel> currentAppModelList; // Currently displayed list of apps
    ArrayList<String> removedAppList; // List of apps selected for removal
    AppListAdapter appListAdapter; // Adapter for displaying apps in RecyclerView

    // Creates options menu from menu resource
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    // Handles selection of menu items for sorting the app list
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.fromOldToNew) {
            // Sorts apps from oldest to newest based on install date
            ArrayList<AppModel> fromOldToNewList = new ArrayList<>(appModelList);
            Collections.sort(fromOldToNewList, Comparator.comparingLong(AppModel::getAppInstalDate));
            appListAdapter.sortArrayList(fromOldToNewList);
            currentAppModelList = fromOldToNewList;
            return true;
        } else if (itemId == R.id.fromNewToOld) {
            // Sorts apps from newest to oldest based on install date
            ArrayList<AppModel> fromNewToOldList = new ArrayList<>(appModelList);
            Collections.sort(fromNewToOldList, (app1, app2) -> Long.compare(app2.getAppInstalDate(), app1.getAppInstalDate()));
            appListAdapter.sortArrayList(fromNewToOldList);
            currentAppModelList = fromNewToOldList;
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    // Checks if the app has usage stats permission and if the accessibility service is enabled
    @Override
    protected void onResume() {
        super.onResume();
        if (hasUsageStatsPermission()) {
            if (!isAccessibilityServiceEnabled()) {
                showAccessibilityServicePrompt(); // Prompt user to enable accessibility service
            } else {
                // Load and display the list of user-installed apps
                appModelList = getInstalledApps();
                currentAppModelList = appModelList;
                appListAdapter = new AppListAdapter(MainActivity.this, appModelList, this);
                binding.recyclerView.setAdapter(appListAdapter);
                binding.recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
            }
        } else {
            requestUsageStatsPermission(); // Request usage stats permission if not already granted
        }
    }

    // Broadcast receiver for clearing the list of apps marked for removal
    private BroadcastReceiver removeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean clearRemoveList = intent.getBooleanExtra("clearRemoveList", false);
            if (clearRemoveList) {
                removedAppList.clear(); // Clear the list based on the broadcast
            }
        }
    };

    // Register and unregister the broadcast receiver
    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(removeReceiver, new IntentFilter("CLEAR_REMOVE_LIST"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(removeReceiver);
    }

    // Initializes the activity and sets up the remove apps button
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = LayoutMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle remove apps button click
        binding.btnRemoveApps.setOnClickListener(view -> {
            if (removedAppList == null || removedAppList.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please select apps!", Toast.LENGTH_SHORT).show();
                return;
            }
            for (String packageName : removedAppList) {
                Log.i("MARDUK", "Removing app: " + packageName); // Log the package name of the app being removed
            }
            sendAppListToAccessibilityService(removedAppList); // Send the list of apps to be removed to the accessibility service
            removeMatchingApps(currentAppModelList, removedAppList); // Remove the selected apps from the current list
            appListAdapter.sortArrayList(currentAppModelList); // Refresh the displayed list
        });
    }

    // Removes apps from the list that match the given package names
    public void removeMatchingApps(ArrayList<AppModel> appModels, ArrayList<String> packageNamesToRemove) {
        appModels.removeIf(appModel -> packageNamesToRemove.contains(appModel.getAppPackageName()));
        // Log for debugging
        Log.d("MARDUK", "Apps matching package names removed from the list");
    }

    // Sends the list of apps to be removed to the accessibility service
    public void sendAppListToAccessibilityService(ArrayList<String> packageNames) {
        Intent intent = new Intent("com.android.accesibiliyservice.ACTION_UNINSTALL_APPS");
        intent.putStringArrayListExtra("packageNames", packageNames);
        sendBroadcast(intent);
        // Log for debugging
        Log.d("MARDUK", "Sending app list to accessibility service for removal");
    }

    // Checks if the application has permission to access usage stats.
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        // Returns true if permission is granted.
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    // Requests permission from the user to access usage stats.
    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
    }

    // Retrieves a list of installed applications, excluding system apps.
    private ArrayList<AppModel> getInstalledApps() {
        ArrayList<AppModel> apps = new ArrayList<>();
        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            // Skip system applications.
            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String appName = packageManager.getApplicationLabel(packageInfo).toString();
                String packageName = packageInfo.packageName;
                // Log application name and flags for debugging purposes.
                Log.i("MARDUK", appName + " :" + packageInfo.flags);
                // Retrieve and format the installation date.
                long installDate = getInstallDate(packageManager, packageName);
                // Retrieve the application size.
                String size = getAppSize(packageManager, packageInfo);
                Drawable icon = packageManager.getApplicationIcon(packageInfo);
                // Create a new AppModel instance with the retrieved data.
                AppModel appModel = new AppModel(appName, packageName, installDate, size, icon);
                // Add the new instance to the list of applications.
                apps.add(appModel);
            }
        }
        return apps;
    }
    // Retrieves the installation date of an application.
    private Long getInstallDate(PackageManager packageManager, String packageName) {
        long installDate = 0;
        try {
            // Fetch package info for the given package name.
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            // Retrieve the first installation time from the package info.
            installDate = packageInfo.firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            // Exception handling if the package name is not found.
            // Here you might want to handle the exception or log it.
        }
        return installDate;
    }

    // Calculates the size of an application for API level 26 and above.
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getAppSize(PackageManager packageManager, ApplicationInfo packageInfo) {
        long size = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Get the StorageStatsManager instance for accessing storage stats.
            StorageStatsManager storageStatsManager = (StorageStatsManager) getSystemService(Context.STORAGE_STATS_SERVICE);
            try {
                // Query storage stats for the application using its UID.
                StorageStats storageStats = storageStatsManager.queryStatsForUid(packageInfo.storageUuid, packageInfo.uid);
                // Aggregate app size, data size, and cache size.
                size = storageStats.getAppBytes() + storageStats.getDataBytes() + storageStats.getCacheBytes();
            } catch (IOException e) {
                // Handle IOException if there's a problem accessing storage stats.
                e.printStackTrace();
            }
        }
        // Format the size in a human-readable format and return.
        return Formatter.formatFileSize(this, size);
    }

    // Checks if the accessibility service is enabled by comparing the service ID with the enabled services.
    private boolean isAccessibilityServiceEnabled() {
        // Construct the service ID using the package name and the class name of the accessibility service.
        String service = getPackageName() + "/.Services." + RemoveAppService.class.getSimpleName();
        // Log the service identifier for debugging purposes.
        Log.d("AccessibilityCheck", "Service Identifier: " + service);
        // Get the AccessibilityManager to access system accessibility services.
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        // Retrieve a list of all currently enabled accessibility services.
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        // Iterate through the list of enabled services and check if our service is enabled.
        for (AccessibilityServiceInfo enabledService : enabledServices) {
            // Log the ID of each enabled service for debugging.
            Log.d("AccessibilityCheck", "Enabled Service: " + enabledService.getId());
            // If the service ID matches our service, return true.
            if (service.equals(enabledService.getId())) {
                return true;
            }
        }
        // Return false if our service is not found in the list of enabled services.
        return false;
    }

    // Shows a prompt to the user to enable the accessibility service.
    private void showAccessibilityServicePrompt() {
        new AlertDialog.Builder(this)
                .setTitle("Accessibility Service Required")
                .setMessage("This app requires the accessibility service to be enabled for full functionality.")
                .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Direct the user to accessibility settings.
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Receives the list of apps to be removed from the accessibility service and stores it.
    @Override
    public void sendRemoveAppList(ArrayList<String> removedAppList) {
        // Store the received list of apps to be removed.
        this.removedAppList = removedAppList;
        // Additional logic to process the list can be added here.
    }

}