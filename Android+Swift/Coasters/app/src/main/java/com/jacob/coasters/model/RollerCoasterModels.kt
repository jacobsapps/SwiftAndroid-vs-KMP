package com.jacob.coasters.model

data class RollerCoaster(
    val slug: String,
    val name: String,
    val construction: String,
    val prebuiltDesigns: List<String>,
    val sourceUrl: String,
    val imageUrl: String,
    val imageSource: String
)

data class RollerCoasterListItem(
    val coaster: RollerCoaster
)

data class RollerCoasterDetail(
    val coaster: RollerCoaster
)
