package com.mori.notireplyassistant.core.domain.model

import android.graphics.drawable.Drawable

data class InstalledAppUiModel(
    val name: String,
    val packageName: String,
    val icon: Drawable?
)
