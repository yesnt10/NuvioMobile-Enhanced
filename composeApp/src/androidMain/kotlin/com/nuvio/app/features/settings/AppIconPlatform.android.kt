package com.nuvio.app.features.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

internal object AppIconPlatform {
    private const val aliasPackageName = "com.nuvio.enhanced"
    private const val defaultAlias = "$aliasPackageName.IconDefault"
    private val launcherComponents = listOf(
        NuvioAppIconOption.Default.id to defaultAlias,
        NuvioAppIconOption.Enhanced.id to "$aliasPackageName.IconEnhanced",
        NuvioAppIconOption.Monochrome.id to "$aliasPackageName.IconMonochrome",
        NuvioAppIconOption.Neon.id to "$aliasPackageName.IconNeon",
        NuvioAppIconOption.Gear.id to "$aliasPackageName.IconGear",
        NuvioAppIconOption.Chrome.id to "$aliasPackageName.IconChrome",
        NuvioAppIconOption.Aurora.id to "$aliasPackageName.IconAurora",
        NuvioAppIconOption.Emerald.id to "$aliasPackageName.IconEmerald",
    )

    private var context: Context? = null

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        this.context = appContext
        restoreDefaultIfNeeded(appContext)
    }

    fun currentIconName(): String? {
        val appContext = context ?: return null
        return currentIconName(appContext)
    }

    fun currentLauncherIconResource(context: Context): Int {
        return com.nuvio.app.R.mipmap.ic_launcher
    }

    fun currentLauncherComponent(context: Context): ComponentName {
        val currentName = currentIconName(context) ?: NuvioAppIconOption.Default.id
        val className = launcherComponents.firstOrNull { it.first == currentName }?.second ?: defaultAlias
        return component(context, className)
    }

    private fun currentIconName(context: Context): String? {
        val packageManager = context.packageManager
        val explicitlyEnabled = launcherComponents.firstOrNull { (_, className) ->
            packageManager.getComponentEnabledSetting(component(context, className)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        return explicitlyEnabled?.first
    }

    private fun restoreDefaultIfNeeded(context: Context) {
        val packageManager = context.packageManager
        val hasEnabledComponent = launcherComponents.any { (_, className) ->
            packageManager.getComponentEnabledSetting(component(context, className)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        if (hasEnabledComponent) return

        val defaultClass = defaultAlias
        if (
            packageManager.getComponentEnabledSetting(component(context, defaultClass)) !=
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        ) {
            return
        }
        packageManager.setComponentEnabledSetting(
            component(context, defaultClass),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    suspend fun activateIcon(name: String?): Boolean {
        val appContext = context ?: return false
        val selectedClass = launcherComponents.firstOrNull { it.first == name }?.second ?: defaultAlias
        val packageManager = appContext.packageManager

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.setComponentEnabledSettings(
                    launcherComponents.map { (_, className) ->
                        PackageManager.ComponentEnabledSetting(
                            component(appContext, className),
                            if (className == selectedClass) {
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            } else {
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                            },
                            PackageManager.DONT_KILL_APP,
                        )
                    },
                )
            } else {
                launcherComponents.forEach { (_, className) ->
                    packageManager.setComponentEnabledSetting(
                        component(appContext, className),
                        if (className == selectedClass) {
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        } else {
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        },
                        PackageManager.DONT_KILL_APP,
                    )
                }
            }
        }.isSuccess
    }

    private fun component(context: Context, className: String): ComponentName =
        ComponentName(context.packageName, className)
}
