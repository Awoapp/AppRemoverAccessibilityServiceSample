/*
 * Created by Muhsin Ilhan for AccesibiliyService project at 2.02.2024 23:13.
 * For questions and assistance requests,
 * Email: c.awoapp@gmail.com
 * GitHub: https://github.com/Awoapp
 * Fiverr: https://www.fiverr.com/awoapp
 *
 */

package com.android.accesibiliyservice.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.accesibiliyservice.Models.AppModel;
import com.android.accesibiliyservice.R;
import com.android.accesibiliyservice.Interfaces.SendAppList;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
// Adapter class for displaying a list of user-installed apps in a RecyclerView
public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    Context context;
    ArrayList<AppModel> appList; // List of apps to display
    ArrayList<String> removeAppList; // List of apps selected by the user for removal
    SimpleDateFormat dateFormat; // Formatter for displaying installation dates
    SendAppList sendAppList; // Interface to communicate selected apps for removal

    // Constructor
    public AppListAdapter(Context context, ArrayList<AppModel> appList, SendAppList sendAppList) {
        this.context = context;
        this.appList = appList;
        this.sendAppList = sendAppList;
        removeAppList = new ArrayList<>();
        dateFormat = new SimpleDateFormat("d MMMM yyyy HH:mm", Locale.getDefault());
    }

    // Update and refresh the list of apps
    public void sortArrayList(ArrayList<AppModel> appList) {
        this.appList = appList;
        notifyDataSetChanged(); // Refresh the RecyclerView
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_recyclerview, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppModel app = appList.get(position);

        // Set app details in the view
        holder.appName.setText(app.getAppName());
        holder.packageName.setText("Package: " + app.getAppPackageName());
        holder.installDate.setText("Install Date: " + dateFormat.format(new Date(app.getAppInstalDate())));
        holder.size.setText("Size: " + app.getAppSize());
        holder.icon.setImageDrawable(app.getIcon());

        // Log for debugging
        Log.d("MARDUK", "Displaying app: " + app.getAppName());

        // Handle app selection for removal
        holder.appName.setOnClickListener(view -> {
            boolean isChecked = holder.appName.isChecked();
            if (!isChecked) {
                removeAppList.remove(app.getAppPackageName()); // Remove from list if unchecked
            } else {
                removeAppList.add(app.getAppPackageName()); // Add to list if checked
            }
            sendAppList.sendRemoveAppList(removeAppList); // Communicate selection
            // Log for debugging
            Log.d("MARDUK", "App selection updated for: " + app.getAppName());
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    // ViewHolder class for app items
    public static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        CheckBox appName;
        TextView packageName, installDate, size;
        View view;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views
            icon = itemView.findViewById(R.id.imgIcon);
            appName = itemView.findViewById(R.id.checkBoxAppName);
            packageName = itemView.findViewById(R.id.tvPackageName);
            installDate = itemView.findViewById(R.id.tvInstallDate);
            size = itemView.findViewById(R.id.tvSize);
            view = itemView;
        }
    }
}
