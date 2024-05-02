package co.median.android

import android.Manifest
import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import org.json.JSONArray

@SuppressLint("InlinedApi")
private val permissionsMap = mapOf(
    "Notifications" to permission.POST_NOTIFICATIONS,
    "Camera" to permission.CAMERA,
    "Contacts" to permission.READ_CONTACTS,
    "Microphone" to permission.RECORD_AUDIO,
    "LocationWhenInUse" to permission.ACCESS_FINE_LOCATION,
    "LocationAlways" to permission.ACCESS_BACKGROUND_LOCATION
)

@JvmOverloads
fun checkPermissionStatus(
    activity: Activity,
    permissionsJson: JSONArray? = null
): Map<String, Boolean> {
    val permissionsToCheck = permissionsJson?.toListOfString()
    val statusMap = mutableMapOf<String, Boolean>()

    if (permissionsToCheck.isNullOrEmpty()) {
        permissionsMap.forEach { (key, value) ->
            statusMap[key] = permissionGranted(activity, value)
        }
    } else {
        for (permissionKey in permissionsToCheck) {
            val permissionCode = permissionsMap[permissionKey]
            if (!permissionCode.isNullOrEmpty()) {
                statusMap[permissionKey] = permissionGranted(activity, permissionCode)
            }
        }
    }

    return statusMap
}

private fun permissionGranted(activity: Activity, permission: String): Boolean {
    if (permission == Manifest.permission.POST_NOTIFICATIONS && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }

    if (permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return true
    }

    return ActivityCompat.checkSelfPermission(
        activity,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

private fun JSONArray.toListOfString(): List<String> {
    return List(length()) { index -> getString(index) }
}