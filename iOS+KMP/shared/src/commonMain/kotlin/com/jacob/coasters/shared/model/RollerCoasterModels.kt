package com.jacob.coasters.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RollerCoasterCategory(
    val slug: String,
    val name: String,
    @SerialName("source_url") val sourceUrl: String,
    val construction: String,
    @SerialName("prebuilt_designs") val prebuiltDesigns: List<String> = emptyList(),
    @SerialName("image_source") val imageSource: String
)

@Serializable
data class RollerCoasterCatalog(
    val categories: List<RollerCoasterCategory> = emptyList()
)
