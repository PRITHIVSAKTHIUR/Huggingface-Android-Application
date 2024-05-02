// this function accepts a callback function as params.callback that will be called with the command results
// if a callback is not provided it returns a promise that will resolve with the command results
function addCommandCallback(command, params, persistCallback) {
    if(params?.callback || params?.callbackFunction || params?.statuscallback){
        // execute command with provided callback function
        addCommand(command, params, persistCallback);
    } else {
        // create a temporary function and return a promise that executes command
        var tempFunctionName = '_median_temp_' + Math.random().toString(36).slice(2);
        if(!params) params = {};
        params.callback = tempFunctionName;
        return new Promise(function(resolve, reject) {
            // declare a temporary function
            window[tempFunctionName] = function(data) {
                resolve(data);
                delete window[tempFunctionName];
            }
            // execute command
            addCommand(command, params);
        });
    }
}

function addCallbackFunction(callbackFunction, persistCallback){
    var callbackName;
    if(typeof callbackFunction === 'string'){
        callbackName = callbackFunction;
    } else {
        callbackName = '_median_temp_' + Math.random().toString(36).slice(2);
        window[callbackName] = function(...args) {
            callbackFunction.apply(null, args);
            if(!persistCallback){ // if callback is used just once
                delete window[callbackName];
            }
        }
    }
    return callbackName;
}

function addCommand(command, params, persistCallback){
    var data = undefined;
    if(params) {
        var commandObject = {};
        if(params.callback && typeof params.callback === 'function'){
            params.callback = addCallbackFunction(params.callback, persistCallback);
        }
        if(params.callbackFunction && typeof params.callbackFunction === 'function'){
            params.callbackFunction = addCallbackFunction(params.callbackFunction, persistCallback);
        }
        if(params.statuscallback && typeof params.statuscallback === 'function'){
            params.statuscallback = addCallbackFunction(params.statuscallback, persistCallback);
        }
        commandObject.medianCommand = command;
        commandObject.data = params;
        data = JSON.stringify(commandObject);
    } else data = command;

    JSBridge.postMessage(data);
}

///////////////////////////////
////    General Commands   ////
///////////////////////////////

var median = {};

// backward compatibility for GoNative
var gonative = median;

// to be modified as required
median.nativebridge = {
    custom: function (params){
        addCommand("median://nativebridge/custom", params);
    },
    multi: function (params){
        addCommand("median://nativebridge/multi", params);
    }
};

median.registration = {
    send: function(customData){
        var params = {customData: customData};
        addCommand("median://registration/send", params);
    }
};

median.sidebar = {
    setItems: function (params){
        addCommand("median://sidebar/setItems", params);
    },
    getItems: function (params){
        return addCommandCallback("median://sidebar/getItems", params);
    }
};

median.tabNavigation = {
    selectTab: function (tabIndex){
        addCommand("median://tabs/select/" + tabIndex);
    },
    deselect: function (){
        addCommand("median://tabs/deselect");
    },
    setTabs: function (tabsObject){
        var params = {tabs: tabsObject};
        addCommand("median://tabs/setTabs", params);
    }
};

median.share = {
    sharePage: function (params){
        addCommand("median://share/sharePage", params);
    },
    downloadFile: function (params){
        addCommand("median://share/downloadFile", params);
    },
    downloadImage: function(params){
        addCommand("median://share/downloadImage", params);
    }
};

median.open = {
    appSettings: function (){
        addCommand("median://open/app-settings");
    }
};

median.webview = {
    clearCache: function(){
        addCommand("median://webview/clearCache");
    },
    clearCookies: function(){
        addCommand("median://webview/clearCookies");
    },
    reload: function (){
        addCommand("median://webview/reload");
    }
};

median.config = {
    set: function(params){
        addCommand("median://config/set", params);
    }
};

median.navigationTitles = {
    set: function (parameters){
        var params = {
            persist: parameters.persist,
            data: parameters
        };
        addCommand("median://navigationTitles/set", params);
    },
    setCurrent: function (params){
        addCommand("median://navigationTitles/setCurrent", params);
    },
    revert: function(){
        addCommand("median://navigationTitles/set?persist=true");
    }
};

median.navigationLevels = {
    set: function (parameters){
        var params = {
            persist: parameters.persist,
            data: parameters
        };
        addCommand("median://navigationLevels/set", params);
    },
    setCurrent: function(params){
        addCommand("median://navigationLevels/set", params);
    },
    revert: function(){
        addCommand("median://navigationLevels/set?persist=true");
    }
};

median.statusbar = {
    set: function (params){
        addCommand("median://statusbar/set", params);
    }
};

median.screen = {
    setBrightness: function(data){
        var params = data;
        if(typeof params === 'number'){
            params = {brightness: data};
        }
        addCommand("median://screen/setBrightness", params);
    },
    setColorScheme: function(mode) {
        var params = {mode: mode};
        addCommand("median://screen/setColorScheme", params);
    },
    resetColorScheme: function() {
        addCommand("median://screen/resetColorScheme");
    },
    setMode: function(params) {
        if (params.mode) {
            addCommand("median://screen/setMode", params);
        }
    },
    keepScreenOn: function(params){
        addCommand("median://screen/keepScreenOn", params);
    },
    keepScreenNormal: function(){
        addCommand("median://screen/keepScreenNormal");
    }
};

median.navigationMaxWindows = {
    set: function (maxWindows, autoClose){
        var params = {
            data: maxWindows,
            autoClose: autoClose,
            persist: true
        };
        addCommand("median://navigationMaxWindows/set", params);
    },
    setCurrent: function(maxWindows, autoClose){
        var params = {data: maxWindows, autoClose: autoClose};
        addCommand("median://navigationMaxWindows/set", params);
    }
}

median.window = {
    open: function (urlString, mode) {
        var params = {url: urlString, mode};
        addCommand("median://window/open", params);
    },
    close: function () {
        addCommand("median://window/close");
    }
}

median.connectivity = {
    get: function (params){
        return addCommandCallback("median://connectivity/get", params);
    },
    subscribe: function (params){
        return addCommandCallback("median://connectivity/subscribe", params, true);
    },
    unsubscribe: function (){
        addCommand("median://connectivity/unsubscribe");
    }
};

median.run = {
    deviceInfo: function(){
        addCommand("median://run/median_device_info");
    }
};

median.deviceInfo = function(params){
    return addCommandCallback("median://run/median_device_info", params, true);
};

median.internalExternal = {
    set: function(params){
        addCommand("median://internalExternal/set", params);
    }
};

median.clipboard = {
    set: function(params){
        addCommand("median://clipboard/set", params);
    },
    get: function(params){
        return addCommandCallback("median://clipboard/get", params);
    }
};

median.keyboard = {
    info: function(params){
        return addCommandCallback("median://keyboard/info", params);
    },
    listen: function(callback){
        var params = {callback};
        addCommand("median://keyboard/listen", params);
    }
};

median.events = {
    subscribe: function(callbackName){
        var params = {callbackName};
        addCommand("median://events/subscribe", params);
    }
};

median.permissions = {
    status: function (permissions) {
        return addCommandCallback("median://permissions/status", { permissions });
    }
};

median.contextMenu = {
    setEnabled: function(enabled) {
        addCommand("median://contextMenu/setEnabled", {enabled});
    },
    setLinkActions: function(linkActions) {
        addCommand("median://contextMenu/setLinkActions", {linkActions});
    }
}

//////////////////////////////////////
////   Webpage Helper Functions   ////
//////////////////////////////////////

function median_match_statusbar_to_body_background_color() {
    let rgb = window.getComputedStyle(document.body, null).getPropertyValue('background-color');
    let sep = rgb.indexOf(",") > -1 ? "," : " ";
    rgb = rgb.substring(rgb.indexOf('(')+1).split(")")[0].split(sep).map(function(x) { return x * 1; });
    if(rgb.length === 4){
        rgb = rgb.map(function(x){ return parseInt(x * rgb[3]); })
    }
    let hex = '#' + rgb[0].toString(16).padStart(2,'0') + rgb[1].toString(16).padStart(2,'0') + rgb[2].toString(16).padStart(2,'0');
    let luma = 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]; // per ITU-R BT.709
    if(luma > 40){
        median.statusbar.set({'style': 'dark', 'color': hex});
    }
    else{
        median.statusbar.set({'style': 'light', 'color': hex});
    }
}

function gonative_match_statusbar_to_body_background_color() {
    median_match_statusbar_to_body_background_color();
}

///////////////////////////////
////   Android Exclusive   ////
///////////////////////////////

median.android = {};

median.android.geoLocation = {
    promptLocationServices: function(){
        addCommand("median://geoLocation/promptLocationServices");
    },
    isLocationServicesEnabled: function(params) {
        return addCommandCallback("median://geoLocation/isLocationServicesEnabled", params);
    }
};

median.android.screen = {
    fullscreen: function(){
        addCommand("median://screen/fullscreen");
    },
    normal: function(){
        addCommand("median://screen/normal");
    },
    keepScreenOn: function(){
        addCommand("median://screen/keepScreenOn");
    },
    keepScreenNormal: function(){
        addCommand("median://screen/keepScreenNormal");
    }
};

median.android.audio = {
    requestFocus: function(enabled){
        var params = {enabled: enabled};
        addCommand("median://audio/requestFocus", params);
    }
};

median.android.swipeGestures = {
    enable: function() {
        addCommand("median://swipeGestures/enable");
    },
    disable: function() {
        addCommand("median://swipeGestures/disable");
    }
}
