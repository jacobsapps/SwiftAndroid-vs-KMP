package com.jacob.coasters.data

import com.jacob.coasters.BuildConfig
import com.jacob.coasters.model.RollerCoaster
import com.jacob.core.RollerCoasterService
import com.jacob.coasters.model.RollerCoasterDetail
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.swift.swiftkit.core.SwiftArena
import java.util.concurrent.ConcurrentHashMap

interface RollerCoasterRepository {
    suspend fun fetchAll(): List<RollerCoaster>
    suspend fun search(query: String): List<RollerCoaster>
    suspend fun loadDetail(slug: String): RollerCoasterDetail?
}

class SwiftRollerCoasterRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val service: RollerCoasterService = RollerCoasterService.init(
        SwiftArena.ofAuto()
    )
) : RollerCoasterRepository {

    private val cache = ConcurrentHashMap<String, RollerCoaster>()
    private val apiBase = BuildConfig.ROLLER_COASTER_BASE_URL.trimEnd('/')

    override suspend fun fetchAll(): List<RollerCoaster> = withContext(dispatcher) {
        parse(service.fetchAllJSON()).also { updateCache(it) }
    }

    override suspend fun search(query: String): List<RollerCoaster> = withContext(dispatcher) {
        if (query.isBlank()) {
            fetchAll()
        } else {
            parse(service.searchJSON(query)).also { updateCache(it) }
        }
    }

    override suspend fun loadDetail(slug: String): RollerCoasterDetail? = withContext(dispatcher) {
        cache[slug]?.let { RollerCoasterDetail(it) }
            ?: fetchAll().firstOrNull { it.slug == slug }?.let { RollerCoasterDetail(it) }
    }

    private fun updateCache(items: List<RollerCoaster>) {
        items.forEach { cache[it.slug] = it }
    }

    private fun parse(json: String): List<RollerCoaster> {
        return try {
            val root = JSONObject(json)
            val categories = root.optJSONArray("categories") ?: return emptyList()
            List(categories.length()) { index ->
                categories.optJSONObject(index)?.let { node ->
                    RollerCoaster(
                        slug = node.optString("slug"),
                        name = node.optString("name"),
                        construction = node.optString("construction"),
                        prebuiltDesigns = node.optJSONArray("prebuilt_designs").toList(),
                        sourceUrl = resolveUrl(node.optString("source_url")),
                        imageSource = resolveUrl(node.optString("image_source"))
                    )
                }
            }.filterNotNull()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun resolveUrl(raw: String): String {
        if (raw.isBlank()) return raw
        return if (raw.startsWith("http", ignoreCase = true)) {
            raw
        } else {
            "$apiBase/${raw.removePrefix("/")}"
        }
    }

    private fun JSONArray?.toList(): List<String> {
        if (this == null) return emptyList()
        val items = mutableListOf<String>()
        for (i in 0 until length()) {
            items += optString(i)
        }
        return items
    }
}
