package com.jacob.coasters.data

import com.jacob.coasters.BuildConfig
import com.jacob.coasters.model.RollerCoaster
import com.jacob.core.RollerCoasterCatalogHandle
import com.jacob.core.RollerCoasterCategoryHandle
import com.jacob.core.RollerCoasterService
import com.jacob.coasters.model.RollerCoasterDetail
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.swift.swiftkit.core.SwiftArena
import java.util.concurrent.ConcurrentHashMap
import java.util.Optional

interface RollerCoasterRepository {
    suspend fun fetchAll(): List<RollerCoaster>
    suspend fun search(query: String): List<RollerCoaster>
    suspend fun loadDetail(slug: String): RollerCoasterDetail?
}

class SwiftRollerCoasterRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val service: RollerCoasterService = RollerCoasterService.init(
        Optional.ofNullable(
            BuildConfig.ROLLER_COASTER_BASE_URL.takeUnless { it.isBlank() }
        ),
        SwiftArena.ofAuto()
    )
) : RollerCoasterRepository {

    private val cache = ConcurrentHashMap<String, RollerCoaster>()
    private val apiBase = BuildConfig.ROLLER_COASTER_BASE_URL.trimEnd('/')

    override suspend fun fetchAll(): List<RollerCoaster> = withContext(dispatcher) {
        SwiftArena.ofConfined().use { arena ->
            val catalog = service.fetchAllHandle(arena)
            val items = catalog.toRollerCoasters(arena)
            updateCache(items)
            items
        }
    }

    override suspend fun search(query: String): List<RollerCoaster> = withContext(dispatcher) {
        if (query.isBlank()) {
            fetchAll()
        } else {
            SwiftArena.ofConfined().use { arena ->
                val catalog = service.searchHandle(query, arena)
                val items = catalog.toRollerCoasters(arena)
                updateCache(items)
                items
            }
        }
    }

    override suspend fun loadDetail(slug: String): RollerCoasterDetail? = withContext(dispatcher) {
        cache[slug]?.let { RollerCoasterDetail(it) }
            ?: fetchAll().firstOrNull { it.slug == slug }?.let { RollerCoasterDetail(it) }
    }

    private fun updateCache(items: List<RollerCoaster>) {
        items.forEach { cache[it.slug] = it }
    }

    private fun RollerCoasterCatalogHandle.toRollerCoasters(arena: SwiftArena): List<RollerCoaster> {
        val total = count()
        if (total == 0) return emptyList()
        return buildList(total) { repeat(total) { index ->
            add(category(index, arena).toRollerCoaster())
        } }
    }

    private fun RollerCoasterCategoryHandle.toRollerCoaster(): RollerCoaster {
        val prebuiltCount = prebuiltDesignCount()
        val designs = if (prebuiltCount == 0) {
            emptyList()
        } else {
            buildList(prebuiltCount) { repeat(prebuiltCount) { index ->
                add(prebuiltDesign(index))
            } }
        }
        return RollerCoaster(
            slug = slug(),
            name = name(),
            construction = construction(),
            prebuiltDesigns = designs,
            sourceUrl = resolveUrl(sourceURLString()),
            imageSource = resolveUrl(imageSourceString())
        )
    }

    private fun resolveUrl(raw: String): String {
        if (raw.isBlank()) return raw
        return if (raw.startsWith("http", ignoreCase = true)) {
            raw
        } else {
            "$apiBase/${raw.removePrefix("/")}"
        }
    }
}
