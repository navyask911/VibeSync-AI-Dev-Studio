package com.vibesync.promptstudio.presentation.devoptions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Centralized, best-effort routing to public Android settings activities.
 *
 * Android does not expose separate public intents for every developer toggle, so USB and
 * wireless debugging intentionally open the native Developer options page.
 */
object DevSettingsHelper {
    fun isDeveloperModeEnabled(context: Context): Boolean =
        readGlobalFlag(context, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED)

    fun isAdbEnabled(context: Context): Boolean =
        readGlobalFlag(context, Settings.Global.ADB_ENABLED)

    private fun readGlobalFlag(context: Context, key: String): Boolean =
        runCatching { Settings.Global.getInt(context.contentResolver, key, 0) == 1 }
            .getOrDefault(false)

    fun openDeveloperOptions(context: Context): Boolean =
        launch(context, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))

    fun openUsbDebugging(context: Context): Boolean =
        openDeveloperOptions(context)

    fun openWirelessDebugging(context: Context): Boolean =
        openDeveloperOptions(context)

    fun openAppStandby(context: Context): Boolean =
        launch(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
        )

    fun openDefaultApps(context: Context): Boolean =
        launch(context, Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))

    fun openOverlayPermission(context: Context): Boolean =
        launch(
            context,
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                .setData(Uri.parse("package:${context.packageName}"))
        )

    fun openBatteryOptimizationList(context: Context): Boolean =
        launch(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

    fun openAllAppNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            launch(context, Intent(Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS))
        } else {
            launch(
                context,
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:${context.packageName}"))
            )
        }

    fun openAccessibility(context: Context): Boolean =
        launch(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    private fun launch(context: Context, intent: Intent): Boolean = runCatching {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
