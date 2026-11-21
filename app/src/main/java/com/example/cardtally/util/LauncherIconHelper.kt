package com.example.cardtally.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

/** Only launcher aliases are toggled; MainActivity itself always remains enabled. */
object LauncherIconHelper {
    private const val PREFS = "theme_prefs"
    private const val KEY = "launcher_icon_mode"

    fun getMode(context: Context): LauncherIconMode {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all[KEY] as? Int
        if (stored != 2 && stored != 3) return LauncherIconMode.fromValue(stored ?: 0)
        // Old follow choices become a fixed choice matching the existing launcher entry.
        // LauncherDark is disabled by default in the manifest, so only an explicit enable is dark.
        val dark = ComponentName(context.packageName, "${context.packageName}.LauncherDark")
        val darkIconEnabled = context.packageManager.getComponentEnabledSetting(dark) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        return LauncherIconPolicy.resolveMode(stored, darkIconEnabled).also { saveMode(context, it) }
    }

    fun saveMode(context: Context, mode: LauncherIconMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, mode.value).apply()
    }

    /** Apply only when leaving the Activity, avoiding foreground alias replacement. */
    fun sync(context: Context): Boolean = try {
        apply(context)
    } catch (error: RuntimeException) {
        Log.w("LauncherIcon", "Unable to resolve launcher icon", error)
        false
    }

    private fun apply(context: Context): Boolean {
        val pm = context.packageManager
        val white = ComponentName(context.packageName, "${context.packageName}.LauncherWhite")
        val dark = ComponentName(context.packageName, "${context.packageName}.LauncherDark")
        val desired = if (getMode(context) == LauncherIconMode.DARK) dark else white
        val other = if (desired == white) dark else white
        val old = listOf(white, dark).associateWith(pm::getComponentEnabledSetting)
        fun isEnabled(component: ComponentName): Boolean = when (old.getValue(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> component == white
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            else -> false
        }
        if (isEnabled(desired) && !isEnabled(other)) return true
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.setComponentEnabledSettings(listOf(
                    PackageManager.ComponentEnabledSetting(desired, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP),
                    PackageManager.ComponentEnabledSetting(other, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                ))
            } else {
                // Never leave a gap with no launcher entry on older Android versions.
                pm.setComponentEnabledSetting(desired, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                pm.setComponentEnabledSetting(other, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
            true
        } catch (error: RuntimeException) {
            // Keep an enabled entry even if a launcher/package-manager rejects the change.
            old.entries.sortedBy { if (isEnabled(it.key)) 0 else 1 }.forEach { (component, state) ->
                runCatching { pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP) }
            }
            Log.w("LauncherIcon", "Unable to update launcher icon", error)
            false
        }
    }
}
