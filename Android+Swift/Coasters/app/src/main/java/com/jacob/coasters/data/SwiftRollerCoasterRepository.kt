package com.jacob.coasters.data

import com.jacob.coasters.model.RollerCoaster
import com.jacob.coasters.model.RollerCoasterDetail
import com.jacob.core.RollerCoasterCatalog
import com.jacob.core.RollerCoasterCategory
import com.jacob.core.RollerCoasterService
import com.jacob.core.RollerCoasterServiceFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.swift.swiftkit.core.SwiftArena
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

interface RollerCoasterRepository {
    suspend fun fetchAll(): List<RollerCoaster>
    suspend fun search(query: String): List<RollerCoaster>
    suspend fun loadDetail(slug: String): RollerCoasterDetail?
}

class SwiftRollerCoasterRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val serviceArena: SwiftArena = SwiftArena.ofAuto(),
    private val service: RollerCoasterService = RollerCoasterServiceFactory.make(
        Optional.empty(),
        serviceArena
    )
) : RollerCoasterRepository {

    private val cache = ConcurrentHashMap<String, RollerCoaster>()

    override suspend fun fetchAll(): List<RollerCoaster> = withContext(dispatcher) {
        fetchCatalogAsync { arena -> service.fetchAll(arena) }
    }

    override suspend fun search(query: String): List<RollerCoaster> = withContext(dispatcher) {
        if (query.isBlank()) {
            fetchAll()
        } else {
            fetchCatalogAsync { arena -> service.search(query, arena) }
        }
    }

    override suspend fun loadDetail(slug: String): RollerCoasterDetail? = withContext(dispatcher) {
        cache[slug]?.let { RollerCoasterDetail(it) }
            ?: fetchAll().firstOrNull { it.slug == slug }?.let { RollerCoasterDetail(it) }
    }

    private suspend fun fetchCatalogAsync(
        fetch: (SwiftArena) -> CompletableFuture<RollerCoasterCatalog>
    ): List<RollerCoaster> {
        val arena = SwiftArena.ofAuto()
        val catalog = fetch(arena).await()
        val items = catalog.toRollerCoasters(arena)
        updateCache(items)
        return items
    }

    private fun updateCache(items: Collection<RollerCoaster>) {
        items.forEach { cache[it.slug] = it }
    }

    private fun RollerCoasterCatalog.toRollerCoasters(arena: SwiftArena): List<RollerCoaster> {
        val total = categoriesCount()
        if (total == 0) return emptyList()
        return buildList(total) {
            repeat(total) { index ->
                add(category(index, arena).toRollerCoaster())
            }
        }
    }

    private fun RollerCoasterCategory.toRollerCoaster(): RollerCoaster {
        val prebuiltCount = prebuiltDesignCount()
        val designs = if (prebuiltCount == 0) {
            emptyList()
        } else {
            buildList(prebuiltCount) {
                repeat(prebuiltCount) { index ->
                    add(prebuiltDesign(index))
                }
            }
        }
        return RollerCoaster(
            slug = getSlug(),
            name = getName(),
            construction = getConstruction(),
            prebuiltDesigns = designs,
            sourceUrl = resolveUrl(sourceURLString()),
            imageUrl = imageSourceString(),
            imageSource = resolveUrl(imageSourceString())
        )
    }

    private fun resolveUrl(raw: String): String {
        return raw
    }
}
