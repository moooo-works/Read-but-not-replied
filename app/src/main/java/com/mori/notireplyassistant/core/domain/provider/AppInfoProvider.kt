package com.mori.notireplyassistant.core.domain.provider

import com.mori.notireplyassistant.core.domain.model.AppInfo

interface AppInfoProvider {
    fun getInstalledApps(): List<AppInfo>
}
