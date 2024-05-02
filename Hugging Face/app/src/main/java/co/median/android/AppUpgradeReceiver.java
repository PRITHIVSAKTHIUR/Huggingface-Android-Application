package co.median.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import co.median.median_core.AppConfig;

public class AppUpgradeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // This will be executed when a new version of the app has been installed over an existing one
        // Does not work in debug mode.
        if (context == null || intent == null) return;
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            AppConfig appConfig = AppConfig.getInstance(context);
            appConfig.deletePersistentConfigFiles();
        }
    }
}
