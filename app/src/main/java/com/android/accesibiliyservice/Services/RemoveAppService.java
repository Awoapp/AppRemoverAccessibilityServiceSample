/*
 * Created by Muhsin Ilhan for AccesibiliyService project at 2.02.2024 23:13.
 * For questions and assistance requests,
 * Email: c.awoapp@gmail.com
 * GitHub: https://github.com/Awoapp
 * Fiverr: https://www.fiverr.com/awoapp
 *
 */

package com.android.accesibiliyservice.Services;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;
public class RemoveAppService extends AccessibilityService {

    private ArrayList<String> appQueue = new ArrayList<>();
    private boolean isUninstalling = false;

    // Broadcast Receiver to listen for uninstall requests
    private BroadcastReceiver appListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.android.accesibiliyservice.ACTION_UNINSTALL_APPS".equals(intent.getAction())) {
                Log.i("MARDUK","Data received");
                ArrayList<String> packageNames = intent.getStringArrayListExtra("packageNames");
                if (packageNames != null) {
                    uninstallAppsSequentially(packageNames);
                }
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        // Register the BroadcastReceiver to listen for uninstall actions
        IntentFilter filter = new IntentFilter("com.android.accesibiliyservice.ACTION_UNINSTALL_APPS");
        Log.i("MARDUK","Broadcast receiver created");
        registerReceiver(appListReceiver, filter);
    }

    @Override
    public void onDestroy() {
        // Unregister the BroadcastReceiver when the service is destroyed
        unregisterReceiver(appListReceiver);
        Log.i("MARDUK","Broadcast receiver destroyed");
        super.onDestroy();
    }

    // Handles the queue of apps to uninstall sequentially
    private void uninstallAppsSequentially(ArrayList<String> packageNames) {
        appQueue.addAll(packageNames);
        processNextUninstall();
    }

    // Processes the next app in the queue to uninstall
    private void processNextUninstall() {
        if (!isUninstalling && !appQueue.isEmpty()) {
            String packageName = appQueue.remove(0);
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            isUninstalling = true;
            Log.i("MARDUK","Uninstall request sent for: "+packageName);
        }else {
            Intent intent = new Intent("CLEAR_REMOVE_LIST");
            intent.putExtra("clearRemoveList", true);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.i("MARDUK","Window state changed");
            // Get all views in the dialog
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                // Check the dialog content
                List<AccessibilityNodeInfo> uninstallMessages =
                        rootNode.findAccessibilityNodeInfosByText("Do you want to uninstall this app?");
                if (!uninstallMessages.isEmpty()) {
                    // Find and click the "OK" or "Uninstall" button
                    List<AccessibilityNodeInfo> confirmButtons = rootNode.findAccessibilityNodeInfosByText("OK");
                    if (confirmButtons.isEmpty()) {
                        confirmButtons = rootNode.findAccessibilityNodeInfosByText("Uninstall");
                    }
                    for (AccessibilityNodeInfo button : confirmButtons) {
                        if (button.isClickable()) {
                            button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            isUninstalling = false;
                            Log.i("MARDUK","Clicking on the button");
                            processNextUninstall();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        // This method is called when the service is interrupted
    }

    // Other necessary methods and functionalities
}
