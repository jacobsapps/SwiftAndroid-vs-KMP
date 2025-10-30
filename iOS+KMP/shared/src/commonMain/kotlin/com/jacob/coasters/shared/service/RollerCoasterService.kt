package com.jacob.coasters.shared.service

import com.jacob.coasters.shared.model.RollerCoasterCatalog
import com.jacob.coasters.shared.model.RollerCoasterCategory
import com.jacob.coasters.shared.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlin.jvm.JvmStatic

class RollerCoasterService(
    private val baseUrl: String = "http://127.0.0.1:3000",
    private val client: HttpClient = createHttpClient()
) {
    companion object {
        @JvmStatic
        fun createDefault(): RollerCoasterService = RollerCoasterService()
    }

    suspend fun fetchAll(): List<RollerCoasterCategory> =
        client.get("$baseUrl/roller-coasters").body<RollerCoasterCatalog>().categories

    suspend fun search(name: String): List<RollerCoasterCategory> {
        if (name.isBlank()) return fetchAll()
        return client.get("$baseUrl/roller-coasters/search") {
            parameter("name", name)
        }.body<RollerCoasterCatalog>().categories
    }

    suspend fun detail(slug: String): RollerCoasterCategory? =
        fetchAll().firstOrNull { it.slug == slug }
}

fun createDefaultRollerCoasterService(): RollerCoasterService = RollerCoasterService()
