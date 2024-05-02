package co.median.android;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.median.median_core.GoNativeActivity;
import co.median.median_core.LeanUtils;

public class MedianEventsManager {

    private static final String TAG = MedianEventsManager.class.getName();
    private final GoNativeActivity activity;
    private final Map<String, JSONObject> eventQueue = new HashMap<>();
    private final List<String> subscriptions = new ArrayList<>();

    public MedianEventsManager(GoNativeActivity activity) {
        this.activity = activity;
    }

    public synchronized void invokeCallback(String callbackName, JSONObject data) {
        if (TextUtils.isEmpty(callbackName)) return;
        if (subscriptions.contains(callbackName)) {
            // launch the callback event
            launchCallbackEvent(callbackName, data);
        } else {
            // hold event
            eventQueue.put(callbackName, data);
        }
    }

    public synchronized void subscribe(String eventName) {
        if (TextUtils.isEmpty(eventName)) return;
        subscriptions.add(eventName);

        // dispatch any queued events for this callback
        if (eventQueue.containsKey(eventName)) {
            JSONObject queuedData = eventQueue.remove(eventName);
            launchCallbackEvent(eventName, queuedData);
        }
    }

    public void unsubscribe(String eventName) {
        subscriptions.remove(eventName);
    }

    private void launchCallbackEvent(String callbackName, JSONObject data) {
        if (activity == null) return;

        try {
            activity.runJavascript(LeanUtils.createJsForCallback(callbackName, data));
        } catch (Exception ex) {
            Log.d(TAG, "launchCallbackEvent: ", ex);
        }
    }
}