package com.mori.notireplyassistant.core.data.provider

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.mori.notireplyassistant.core.domain.model.AppInfo
import com.mori.notireplyassistant.core.domain.provider.AppInfoProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAppInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : AppInfoProvider {

    override fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        return resolveInfos.mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            val label = info.loadLabel(pm).toString()
            if (pkg != null) {
                AppInfo(packageName = pkg, name = label)
            } else null
        }.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
    }
}
