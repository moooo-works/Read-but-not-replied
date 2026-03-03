package com.mori.notireplyassistant.core.domain.model

data class AppInfo(
    val packageName: String,
    val name: String,
    // Icon is handled separately to avoid keeping Drawables in Domain layer
)
