package co.median.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import co.median.median_core.AppConfig;
import co.median.median_core.GNLog;
import co.median.median_core.LeanUtils;
import io.gonative.android.icons.Icon;

/**
 * Created by Weiyin He on 9/22/14.
 * Copyright 2014 GoNative.io LLC
 */
public class TabManager implements NavigationBarView.OnItemSelectedListener {
    private static final String TAG = TabManager.class.getName();
    private static final int maxTabs = 5;

    private final MainActivity mainActivity;
    private final BottomNavigationView bottomNav;
    private final AppConfig appConfig;

    private String currentMenuId;
    private String currentUrl;
    private JSONArray tabs;
    private Map<String, TabMenu> tabMenus;

    private final int iconSize;
    private final int iconColor;

    private final Map<JSONObject, List<Pattern>> tabRegexCache = new HashMap<>(); // regex for each tab to auto-select
    private boolean useJavascript; // do not use tabs from config

    private boolean performAction = true;
    private BroadcastReceiver broadcastReceiver;

    TabManager(MainActivity mainActivity, BottomNavigationView bottomNav) {
        this.mainActivity = mainActivity;
        this.bottomNav = bottomNav;
        this.bottomNav.setOnItemSelectedListener(this);
        this.appConfig = AppConfig.getInstance(this.mainActivity);

        iconSize = this.mainActivity.getResources().getInteger(R.integer.tabbar_icon_size);
        iconColor = mainActivity.getResources().getColor(R.color.tabBarTextColor);

        bottomNav.setBackgroundColor(mainActivity.getResources().getColor(R.color.tabBarBackground));

        ColorStateList iconColorStates = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_checked},
                        new int[]{android.R.attr.state_checked}
                },
                new int[]{
                        mainActivity.getResources().getColor(R.color.tabBarTextColor),
                        mainActivity.getResources().getColor(R.color.tabBarIndicator)
                });

        bottomNav.setItemIconTintList(iconColorStates);
        bottomNav.setItemTextColor(iconColorStates);

        this.broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals(AppConfig.PROCESSED_TAB_NAVIGATION_MESSAGE)) {
                    currentMenuId = null;
                    initializeTabMenus();
                    checkTabs(currentUrl);
                }
            }
        };
        LocalBroadcastManager.getInstance(this.mainActivity)
                .registerReceiver(broadcastReceiver,
                        new IntentFilter(AppConfig.PROCESSED_TAB_NAVIGATION_MESSAGE));

        initializeTabMenus();
    }

    private void initializeTabMenus(){
        ArrayList<Pattern> regexes = appConfig.tabMenuRegexes;
        ArrayList<String> ids = appConfig.tabMenuIDs;

        if (regexes == null || ids == null) {
            return;
        }

        tabMenus = new HashMap<>();
        Map<String, Pattern> tabSelectionConfig = new HashMap<>();

        for (int i = 0; i < ids.size(); i++) {
            tabSelectionConfig.put(ids.get(i), regexes.get(i));
        }

        for (Map.Entry<String, JSONArray> tabMenu : appConfig.tabMenus.entrySet()) {
            TabMenu item = new TabMenu();
            item.tabs = tabMenu.getValue();
            item.urlRegex = tabSelectionConfig.get(tabMenu.getKey());
            tabMenus.put(tabMenu.getKey(), item);
        }
    }

    public void checkTabs(String url) {
        this.currentUrl = url;

        if (this.mainActivity == null || url == null) {
            return;
        }

        if (this.useJavascript) {
            autoSelectTab(url);
            return;
        }

        ArrayList<Pattern> regexes = appConfig.tabMenuRegexes;
        ArrayList<String> ids = appConfig.tabMenuIDs;
        if (regexes == null || ids == null) {
            showTabs(false);
            return;
        }

        String menuId = null;

        for (int i = 0; i < regexes.size(); i++) {
            Pattern regex = regexes.get(i);
            if (regex.matcher(url).matches()) {
                menuId = ids.get(i);
                break;
            }
        }

        setMenuID(menuId);

        if (menuId != null) autoSelectTab(url);
    }



    private void setMenuID(String id){
        if (id == null) {
            this.currentMenuId = null;
            showTabs(false);
        }
        else if (this.currentMenuId == null || !this.currentMenuId.equals(id)) {
            this.currentMenuId = id;
            JSONArray tabs = AppConfig.getInstance(this.mainActivity).tabMenus.get(id);
            setTabs(tabs);
            showTabs(bottomNav.getMenu().size() != 0);
        }
    }

    private void setTabs(JSONArray tabs) {
        this.tabs = tabs;

        int selectedNumber = -1;
        bottomNav.getMenu().clear();
        if(tabs == null) return;
    
        for (int i = 0; i < tabs.length(); i++) {
            if(i > (maxTabs-1)){
                GNLog.getInstance().logError(TAG, "Tab menu items list should not have more than 5 items");
                break;
            }

            JSONObject item = tabs.optJSONObject(i);
            if (item == null) continue;

            String label = item.optString("label");
            String icon = item.optString("icon");

            // if no label, icon and url is provided, do not include
            if(label.isEmpty() && icon.isEmpty() && item.optString("url").isEmpty()){
                continue;
            }

            // set default drawable "Question Mark" when no icon provided
            if (icon.isEmpty()) {
                icon = "faw_question";
                GNLog.getInstance().logError(TAG, "All tabs must have icons.");
            }

            bottomNav.getMenu().add(Menu.NONE, i, Menu.NONE, label).setIcon(new Icon(mainActivity, icon, iconSize, iconColor).getDrawable());

            if (item.optBoolean("selected")) {
                selectedNumber = i;
            }
        }

        if (selectedNumber > -1) {
            selectTabNumber(selectedNumber, true);
        }
    }

    // regex used for auto tab selection
    private List<Pattern> getRegexForTab(JSONObject tabConfig) {
        if (tabConfig == null) return null;

        Object regex = tabConfig.opt("regex");
        if (regex == null) return null;

        return LeanUtils.createRegexArrayFromStrings(regex);
    }

    private List<Pattern> getCachedRegexForTab(int position) {
        if (tabs == null || position < 0 || position >= tabs.length()) return null;

        JSONObject tabConfig = tabs.optJSONObject(position);
        if (tabConfig == null) return null;

        if (tabRegexCache.containsKey(tabConfig)) {
            return tabRegexCache.get(tabConfig);
        } else {
            List<Pattern> regex = getRegexForTab(tabConfig);
            tabRegexCache.put(tabConfig, regex);
            return regex;
        }
    }

    public void autoSelectTab(String url) {
        if (tabs == null) return;

        for (int i = 0; i < tabs.length(); i++) {
            List<Pattern> patternList = getCachedRegexForTab(i);
            if (patternList == null) continue;

            for(Pattern regex : patternList) {
                if (regex.matcher(url).matches()) {
                    bottomNav.setSelectedItemId(i);
                    return;
                }
            }
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean selectTab(String url, String javascript) {
        if (url == null) return false;

        if (javascript == null) javascript = "";

        if (this.tabs != null) {
            for (int i = 0; i < this.tabs.length(); i++) {
                JSONObject entry = this.tabs.optJSONObject(i);
                if (entry != null) {
                    String entryUrl = entry.optString("url");
                    String entryJs = entry.optString("javascript");

                    if (url.equals(entryUrl) && javascript.equals(entryJs)) {
                        if (this.bottomNav != null) {
                            this.bottomNav.setSelectedItemId(i);
                            return true;
                        }
                    }

                }
            }
        }

        return false;
    }

    public void setTabsWithJson(JSONObject tabsJson, int tabMenuId) {
        if(tabsJson == null) return;

        this.useJavascript = true;

        JSONArray tabs = tabsJson.optJSONArray("items");
        if (tabs != null) setTabs(tabs);

        if(tabMenuId != -1){
            TabMenu tabMenu = tabMenus.get(Integer.toString(tabMenuId));
            if(tabMenu == null || tabs != null) return;
            setTabs(tabMenu.tabs);
        }

        Object enabled = tabsJson.opt("enabled");
        if (enabled instanceof Boolean) {
            showTabs((Boolean) enabled);
        }
    }

    public void selectTabNumber(int tabNumber, boolean performAction) {
        if (tabNumber < 0 || tabNumber >= bottomNav.getMenu().size()) {
            return;
        }
        this.performAction = performAction;
        this.bottomNav.setSelectedItemId(tabNumber);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (this.tabs != null) {
            JSONObject entry = this.tabs.optJSONObject(item.getItemId());

            String url = entry.optString("url");
            String javascript = entry.optString("javascript");

            if (!performAction) {
                performAction = true;
                return true;
            }

            if (!TextUtils.isEmpty(url)) {
                if (!TextUtils.isEmpty(javascript)) mainActivity.loadUrlAndJavascript(url, javascript, true);
                else mainActivity.loadUrl(url, true);
            }
        }
        return true;
    }

    public void showTabs(boolean show) {
        mainActivity.runOnUiThread(() -> {
            if (show) this.bottomNav.setVisibility(View.VISIBLE);
            else this.bottomNav.setVisibility(View.GONE);
        });
    }

    public void deselectTabs() {
        Menu menu = bottomNav.getMenu();
        menu.setGroupCheckable(0, true, false);
        for(int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setChecked(false);
        }
        menu.setGroupCheckable(0, true, true);
    }

    private static class TabMenu {
        Pattern urlRegex;
        JSONArray tabs;
    }

    public void unregisterReceiver() {
        if (this.broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this.mainActivity).unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }
}
