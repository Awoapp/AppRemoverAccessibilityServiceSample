

/*
 * Created by Muhsin Ilhan for AccesibiliyService project at 2.02.2024 23:13.
 * For questions and assistance requests,
 * Email: c.awoapp@gmail.com
 * GitHub: https://github.com/Awoapp
 * Fiverr: https://www.fiverr.com/awoapp
 *
 */

package com.android.accesibiliyservice.Models;

import android.graphics.drawable.Drawable;

public class AppModel {
    String appName;
    String appPackageName;
    long appInstalDate;
    String appSize;
    Drawable icon;
    public AppModel() {
    }

    public AppModel(String appName, String appPackageName, long appInstalDate, String appSize,
                    Drawable icon) {
        this.appName = appName;
        this.appPackageName = appPackageName;
        this.appInstalDate = appInstalDate;
        this.appSize = appSize;
        this.icon = icon;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppPackageName() {
        return appPackageName;
    }

    public void setAppPackageName(String appPackageName) {
        this.appPackageName = appPackageName;
    }

    public long getAppInstalDate() {
        return appInstalDate;
    }

    public void setAppInstalDate(long appInstalDate) {
        this.appInstalDate = appInstalDate;
    }

    public String getAppSize() {
        return appSize;
    }

    public void setAppSize(String appSize) {
        this.appSize = appSize;
    }
}
