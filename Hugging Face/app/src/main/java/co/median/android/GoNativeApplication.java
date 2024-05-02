package co.median.android;

import android.content.ComponentCallbacks2;
import android.os.Build;
import android.os.Message;
import android.util.Base64;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import co.median.median_core.AppConfig;
import co.median.median_core.Bridge;
import co.median.median_core.BridgeModule;
import co.median.median_core.GNLog;

/**
 * Created by weiyin on 9/2/15.
 * Copyright 2014 GoNative.io LLC
 */
public class GoNativeApplication extends MultiDexApplication {

    private final String CUSTOM_CSS_FILE = "customCSS.css";
    private final String CUSTOM_JS_FILE = "customJS.js";
    private final String ANDROID_CUSTOM_CSS_FILE = "androidCustomCSS.css";
    private final String ANDROID_CUSTOM_JS_FILE = "androidCustomJS.js";

    private LoginManager loginManager;
    private RegistrationManager registrationManager;
    private WebViewPool webViewPool;
    private Message webviewMessage;
    private GoNativeWindowManager goNativeWindowManager;
    private List<BridgeModule> plugins;
    private boolean ignoreInitialWebViewSetup = false;

    private final static String TAG = GoNativeApplication.class.getSimpleName();
    public final Bridge mBridge = new Bridge(this) {
        @Override
        protected List<BridgeModule> getPlugins() {
            if (GoNativeApplication.this.plugins == null) {
                GoNativeApplication.this.plugins = new PackageList(GoNativeApplication.this).getPackages();
            }

            return  GoNativeApplication.this.plugins;
        }
    };

    private boolean appBackgrounded = false;

    private String customCss;
    private String customJs;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // App theme setup for API 31 and above
            setupAppTheme();
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        mBridge.onApplicationCreate(this);

        AppConfig appConfig = AppConfig.getInstance(this);
        if (appConfig.configError != null) {
            Toast.makeText(this, "Invalid appConfig json", Toast.LENGTH_LONG).show();
            GNLog.getInstance().logError(TAG, "AppConfig error", appConfig.configError);
        }

        this.loginManager = new LoginManager(this);

        if (appConfig.registrationEndpoints != null) {
            this.registrationManager = new RegistrationManager(this);
            registrationManager.processConfig(appConfig.registrationEndpoints);
        }

        // some global webview setup
        WebViewSetup.setupWebviewGlobals(this);

        webViewPool = new WebViewPool();

        goNativeWindowManager = new GoNativeWindowManager();

        // load custom CSS and JS files
        loadCustomCssFiles(appConfig);
        loadCustomJSFiles(appConfig);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void setupAppTheme() {
        // Note: Devices running on API 31 and above use UiModeManager#setApplicationNightMode to set the theme.
        // This setting remains persistent and does not need to be applied every time the app starts.

        String configAppTheme = ThemeUtils.getConfigAppTheme(this);
        String currentAppTheme = ThemeUtils.getCurrentApplicationTheme(this);

        if (configAppTheme.equals(currentAppTheme)) return; // Theme already set, do nothing

        // If androidTheme is `auto`, set the app theme directly as this does not cause the MainActivity to recreate.
        if ("auto".equals(configAppTheme)) {
            ThemeUtils.setAppThemeApi31AndAbove(this, configAppTheme);
            return;
        }

        // If androidTheme differs from the system/device theme at initial app start, setting the theme causes MainActivity to recreate,
        // which leads to a WebView bug displaying a blank screen. To prevent this, set a flag to ignore the first WebView setup during MainActivity.onCreate().
        if (("light".equals(configAppTheme) && ThemeUtils.isDarkThemeEnabled(this)) ||
                ("dark".equals(configAppTheme) && !ThemeUtils.isDarkThemeEnabled(this))
        ) {
            ignoreInitialWebViewSetup = true;
        }

        ThemeUtils.setAppThemeApi31AndAbove(this, configAppTheme);
    }

    public boolean ignoreInitialWebViewSetup() {
        return ignoreInitialWebViewSetup;
    }

    public void ignoreInitialWebViewSetupConsumed() {
        this.ignoreInitialWebViewSetup = false;
    }

    public LoginManager getLoginManager() {
        return loginManager;
    }

    public RegistrationManager getRegistrationManager() {
        return registrationManager;
    }

    public WebViewPool getWebViewPool() {
        return webViewPool;
    }

    public Message getWebviewMessage() {
        return webviewMessage;
    }

    public void setWebviewMessage(Message webviewMessage) {
        this.webviewMessage = webviewMessage;
    }

    public Map<String, Object> getAnalyticsProviderInfo() {
        return mBridge.getAnalyticsProviderInfo();
    }

    public GoNativeWindowManager getWindowManager() {
        return goNativeWindowManager;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // App has gone into the background
            setAppBackgrounded(true);
        }
    }

    public boolean isAppBackgrounded() {
        return appBackgrounded;
    }

    public void setAppBackgrounded(boolean appBackgrounded) {
        this.appBackgrounded = appBackgrounded;
    }

    private void loadCustomCssFiles(AppConfig appConfig) {
        if (!appConfig.hasCustomCSS && !appConfig.hasAndroidCustomCSS) return;

        List<String> filePaths = new ArrayList<>();
        // read customCSS.css file
        if(appConfig.hasCustomCSS) {
            filePaths.add(CUSTOM_CSS_FILE);
        }
        // read android customCSS.css file
        if(appConfig.hasAndroidCustomCSS){
            filePaths.add(ANDROID_CUSTOM_CSS_FILE);
        }
        if(filePaths.size() == 0) return;
        try {
            this.customCss = Base64.encodeToString(readAssetsToString(filePaths).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        } catch (Exception ex) {
            GNLog.getInstance().logError(TAG, "Error loading custom CSS files", ex);
        }
    }

    private void loadCustomJSFiles(AppConfig appConfig) {
        if (!appConfig.hasCustomJS && !appConfig.hasAndroidCustomJS) return;

        List<String> filePaths = new ArrayList<>();
        // read customJS file
        if(appConfig.hasCustomJS){
            filePaths.add(CUSTOM_JS_FILE);
        }
        // read android customJS file
        if(appConfig.hasAndroidCustomJS){
            filePaths.add(ANDROID_CUSTOM_JS_FILE);
        }
        if(filePaths.size() == 0) return;
        try {
            this.customJs = Base64.encodeToString(readAssetsToString(filePaths).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        } catch (Exception ex) {
            GNLog.getInstance().logError(TAG, "Error loading custom JS files", ex);
        }
    }

    public String getCustomCss() {
        return customCss;
    }

    public String getCustomJs() {
        return customJs;
    }

    private String readAssetsToString(List<String> paths) {
        StringBuilder builder = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (String path : paths) {
            try {
                co.median.median_core.IOUtils.copy(new BufferedInputStream(this.getAssets().open(path)), baos);
                builder.append(baos);
                baos.reset();
            } catch (IOException ioe) {
                GNLog.getInstance().logError(TAG, "Error reading " + path, ioe);
            }
        }
        co.median.median_core.IOUtils.close(baos);
        return builder.toString();
    }
}
