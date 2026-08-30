package com.nuvio.app.features.home

import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.catalog.fetchCatalogPage
import com.nuvio.app.features.collection.Collection
import com.nuvio.app.features.collection.CollectionRepository
import com.nuvio.app.features.collection.CollectionSource
import com.nuvio.app.features.collection.TmdbCollectionSourceResolver
import com.nuvio.app.features.collection.catalogRouteKey
import com.nuvio.app.features.collection.findCollectionCatalog
import com.nuvio.app.features.cloudstream.CloudStreamPluginItem
import com.nuvio.app.features.cloudstream.CloudStreamRepository
import com.nuvio.app.features.cloudstream.toMetaPreview
import com.nuvio.app.features.tmdb.TmdbMetadataService
import com.nuvio.app.features.tmdb.TmdbSettings
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.tmdb.normalizeTmdbLanguage
import com.nuvio.app.features.trakt.TraktPublicListSourceResolver
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.absoluteValue
import kotlin.random.Random

private data class CloudHomeSectionsResult(
    val sections: List<HomeCatalogSection>,
    val errorMessage: String?,
)

object HomeRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var activeRequestKey: String? = null
    private var currentRequestKey: String? = null
    private var currentDefinitions: List<HomeCatalogDefinition> = emptyList()
    private var cachedSections: Map<String, HomeCatalogSection> = emptyMap()
    private var cachedCloudSections: List<HomeCatalogSection> = emptyList()
    private var cachedCollectionHeroItems: List<MetaPreview> = emptyList()
    private val localizedHeroArtworkCache = mutableMapOf<String, MetaPreview>()
    private var localizedHeroArtworkJob: Job? = null
    private var localizedHeroArtworkRequestKey: String? = null
    private var collectionHeroJob: Job? = null
    private var collectionHeroRequestKey: String? = null
    private var lastPublishedCatalogHeroEmpty: Boolean = true
    private var lastErrorMessage: String? = null

    fun refresh(addons: List<ManagedAddon>, force: Boolean = false) {
        CloudStreamRepository.initialize()
        val cloudState = CloudStreamRepository.uiState.value
        val cloudPlugins = cloudState.plugins.filter(CloudStreamPluginItem::isRunnable)
        val activeAddons = addons.enabledAddons()
        val requests = buildHomeCatalogDefinitions(activeAddons)
        currentDefinitions = requests
        val requestCacheKeys = requests.mapTo(mutableSetOf(), HomeCatalogDefinition::cacheKey)
        cachedSections = cachedSections.filterKeys(requestCacheKeys::contains)
        val requestKey = buildString {
            append(requests.joinToString(separator = "|", transform = HomeCatalogDefinition::cacheKey))
            append("|cloudstream=")
            append(cloudState.registryRevision)
            append(':')
            append(cloudPlugins.joinToString(separator = ",") { it.metadata.id.value })
        }

        if (!force && activeRequestKey == requestKey && _uiState.value.isLoading) return

        if (
            !force &&
            requestKey == completedRequestKey &&
            requestCacheKeys.all(cachedSections::containsKey) &&
            (requestCacheKeys.any(::hasRenderableCachedSection) || cachedCloudSections.any { it.items.isNotEmpty() })
        ) {
            if (_uiState.value.sections.isEmpty() || _uiState.value.heroItems.isEmpty()) {
                applyCurrentSettings()
            }
            return
        }
        activeRequestKey = requestKey

        if (requests.isEmpty() && cloudPlugins.isEmpty()) {
            activeJob?.cancel()
            activeJob = null
            activeRequestKey = null
            cachedSections = emptyMap()
            cachedCloudSections = emptyList()
            lastErrorMessage = null
            publishCurrentState(
                isLoading = false,
                requestKey = requestKey,
            )
            ensureCollectionHeroFallback(
                addons = activeAddons,
                forceRefresh = force,
                refreshSources = true,
                requestKey = requestKey,
            )
            return
        }

        activeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        activeJob = scope.launch {
            val cloudResult = loadCloudSections(cloudPlugins)
            if (activeRequestKey != requestKey) return@launch
            cachedCloudSections = cloudResult.sections
            val prioritizedRequests = prioritizeDefinitions(
                definitions = requests,
                snapshot = HomeCatalogSettingsRepository.snapshot(),
            )
            val pendingRequests = prioritizedRequests.filter { definition ->
                force || cachedSections[definition.cacheKey] == null
            }
            if (pendingRequests.isEmpty()) {
                lastErrorMessage = cloudResult.errorMessage
                completedRequestKey = requestKey.takeIf { cachedCloudSections.isNotEmpty() || cachedSections.isNotEmpty() }
                activeRequestKey = null
                publishCurrentState(
                    isLoading = false,
                    requestKey = requestKey,
                )
                return@launch
            }
            val loadedSections = linkedMapOf<String, HomeCatalogSection>().apply {
                putAll(cachedSections)
            }
            var firstErrorMessage: String? = cloudResult.errorMessage
            var batchIndex = 0

            prioritizedRequests.chunked(HOME_CATALOG_FETCH_BATCH_SIZE).forEach { batch ->
                if (activeRequestKey != requestKey) return@launch
                val results = batch.map { request ->
                    async {
                        request to runCatching {
                            withTimeoutOrNull(HOME_CATALOG_REQUEST_TIMEOUT_MS) {
                                request.toSection()
                            } ?: error("${request.defaultTitle} timed out")
                        }
                    }
                }.awaitAll()

                if (activeRequestKey != requestKey) return@launch

                results.mapNotNull { (request, result) ->
                    result.getOrNull()?.let { section -> request.cacheKey to section }
                }.forEach { (cacheKey, section) ->
                    loadedSections[cacheKey] = section
                }
                if (firstErrorMessage == null) {
                    firstErrorMessage = results.firstNotNullOfOrNull { (_, result) ->
                        result.exceptionOrNull()?.message
                    }
                }
                cachedSections = loadedSections.toMap()
                lastErrorMessage = firstErrorMessage
                if (batchIndex == 0 || (batchIndex + 1) % HOME_CATALOG_PUBLISH_INTERVAL == 0) {
                    publishCurrentState(
                        isLoading = true,
                        requestKey = requestKey,
                    )
                }
                batchIndex++
            }

            if (activeRequestKey != requestKey) return@launch

            cachedSections = loadedSections.toMap()
            lastErrorMessage = firstErrorMessage
            if (cachedSections.values.any { section -> section.items.isNotEmpty() } || cachedCloudSections.isNotEmpty()) {
                completedRequestKey = requestKey
            }
            activeRequestKey = null
            publishCurrentState(
                isLoading = false,
                requestKey = requestKey,
            )
            ensureCollectionHeroFallback(
                addons = activeAddons,
                forceRefresh = force,
                refreshSources = true,
                requestKey = requestKey,
            )
        }
    }

    fun applyCurrentSettings() {
        publishCurrentState(
            isLoading = _uiState.value.isLoading,
            requestKey = currentRequestKey,
        )
        ensureCollectionHeroFallback(
            addons = AddonRepository.uiState.value.addons.enabledAddons(),
            forceRefresh = false,
            refreshSources = false,
            requestKey = currentRequestKey,
        )
    }

    fun clear() {
        activeJob?.cancel()
        activeJob = null
        activeRequestKey = null
        currentRequestKey = null
        currentDefinitions = emptyList()
        cachedSections = emptyMap()
        cachedCloudSections = emptyList()
        cachedCollectionHeroItems = emptyList()
        localizedHeroArtworkCache.clear()
        localizedHeroArtworkJob?.cancel()
        localizedHeroArtworkJob = null
        localizedHeroArtworkRequestKey = null
        collectionHeroJob?.cancel()
        collectionHeroJob = null
        collectionHeroRequestKey = null
        lastPublishedCatalogHeroEmpty = true
        lastErrorMessage = null
        _uiState.value = HomeUiState()
    }

    private fun publishCurrentState(
        isLoading: Boolean,
        requestKey: String?,
    ) {
        val snapshot = HomeCatalogSettingsRepository.snapshot()
        val preferences = snapshot.preferences
        val todayIsoDate = if (snapshot.hideUnreleasedContent) CurrentDateProvider.todayIsoDate() else null
        fun HomeCatalogSection.withReleaseFilter(): HomeCatalogSection =
            if (todayIsoDate == null) this else filterReleasedItems(todayIsoDate)

        val sections = currentDefinitions
            .sortedBy { definition -> preferences[definition.key]?.order ?: Int.MAX_VALUE }
            .mapNotNull { definition ->
                val preference = preferences[definition.key]
                if (preference?.enabled == false) return@mapNotNull null

                val section = cachedSections[definition.cacheKey]?.withReleaseFilter() ?: return@mapNotNull null
                if (section.items.isEmpty()) return@mapNotNull null
                val customTitle = preference?.customTitle.orEmpty()
                section.copy(
                    title = customTitle.ifBlank { definition.titleFor(snapshot.showCatalogType) },
                )
            } + cachedCloudSections

        val catalogHeroItems = if (snapshot.heroEnabled) {
            val heroRandom = Random((requestKey?.hashCode() ?: 0).absoluteValue + 1)
            (currentDefinitions
                .filter { definition -> preferences[definition.key]?.heroSourceEnabled != false }
                .mapNotNull { definition -> cachedSections[definition.cacheKey] }
                .map { section -> section.withReleaseFilter() }
                .flatMap { section -> section.items })
                .distinctBy { item -> "${item.type}:${item.id}" }
                .shuffled(heroRandom)
                .take(HOME_HERO_ITEM_LIMIT)
        } else {
            emptyList()
        }
        lastPublishedCatalogHeroEmpty = snapshot.heroEnabled && catalogHeroItems.isEmpty()
        val resolvedHeroItems = if (snapshot.heroEnabled) {
            catalogHeroItems.ifEmpty { cachedCollectionHeroItems }
        } else {
            emptyList()
        }
        val heroItems = when {
            !snapshot.heroEnabled -> emptyList()
            else -> resolveLocalizedHeroArtwork(
                items = resolvedHeroItems,
                requestKey = requestKey,
            )
        }

        _uiState.value = HomeUiState(
            isLoading = isLoading,
            heroItems = heroItems,
            sections = sections,
            errorMessage = if (sections.isEmpty()) lastErrorMessage else null,
        )
    }

    private fun resolveLocalizedHeroArtwork(
        items: List<MetaPreview>,
        requestKey: String?,
    ): List<MetaPreview> {
        if (items.isEmpty()) return emptyList()
        val settings = TmdbSettingsRepository.snapshot()
        if (!settings.shouldLocalizeHeroArtwork()) return items

        val localizedItems = items.mapNotNull { item ->
            localizedHeroArtworkCache[localizedHeroArtworkCacheKey(item, settings)]
        }
        val missingItems = items.filterNot { item ->
            localizedHeroArtworkCache.containsKey(localizedHeroArtworkCacheKey(item, settings))
        }
        if (missingItems.isNotEmpty()) {
            ensureLocalizedHeroArtwork(
                items = items,
                settings = settings,
                requestKey = requestKey,
            )
        }
        return localizedItems
    }

    private fun ensureLocalizedHeroArtwork(
        items: List<MetaPreview>,
        settings: TmdbSettings,
        requestKey: String?,
    ) {
        val normalizedLanguage = normalizeTmdbLanguage(settings.language)
        val nextRequestKey = buildString {
            append(requestKey.orEmpty())
            append("|tmdbHeroArtwork=")
            append(normalizedLanguage)
            append("|items=")
            items.forEach { item ->
                append(item.stableKey())
                append(';')
            }
        }
        if (localizedHeroArtworkRequestKey == nextRequestKey) return

        localizedHeroArtworkJob?.cancel()
        localizedHeroArtworkRequestKey = nextRequestKey
        localizedHeroArtworkJob = scope.launch {
            val distinctItems = items.distinctBy(MetaPreview::stableKey)
            distinctItems
                .chunked(HOME_HERO_LOCALIZED_ARTWORK_BATCH_SIZE)
                .forEachIndexed { batchIndex, batch ->
                    if (localizedHeroArtworkRequestKey != nextRequestKey) return@launch
                    val timeoutMs = if (batchIndex == 0) {
                        HOME_HERO_INITIAL_LOCALIZED_ARTWORK_TIMEOUT_MS
                    } else {
                        HOME_HERO_LOCALIZED_ARTWORK_TIMEOUT_MS
                    }
                    val results = batch.map { item ->
                        async {
                            val localized = try {
                                withTimeoutOrNull(timeoutMs) {
                                    TmdbMetadataService.localizePreviewArtwork(
                                        item = item,
                                        settings = settings,
                                    )
                                }
                            } catch (error: Throwable) {
                                if (error is CancellationException) throw error
                                null
                            }
                            localizedHeroArtworkCacheKey(item, settings) to (localized ?: item)
                        }
                    }.awaitAll()

                    if (localizedHeroArtworkRequestKey != nextRequestKey) return@launch
                    results.forEach { (cacheKey, item) ->
                        localizedHeroArtworkCache[cacheKey] = item
                    }
                    publishCurrentState(
                        isLoading = _uiState.value.isLoading,
                        requestKey = requestKey,
                    )
                }
            if (localizedHeroArtworkRequestKey != nextRequestKey) {
                return@launch
            }
            localizedHeroArtworkRequestKey = null
            publishCurrentState(
                isLoading = _uiState.value.isLoading,
                requestKey = requestKey,
            )
        }
    }

    private fun TmdbSettings.shouldLocalizeHeroArtwork(): Boolean =
        enabled && hasApiKey && useArtwork

    private fun localizedHeroArtworkCacheKey(item: MetaPreview, settings: TmdbSettings): String =
        "${item.stableKey()}:${normalizeTmdbLanguage(settings.language)}"

    private suspend fun HomeCatalogDefinition.toSection(): HomeCatalogSection {
        val page = fetchCatalogPage(
            manifestUrl = manifestUrl,
            type = type,
            catalogId = catalogId,
            maxItems = HOME_CATALOG_PREVIEW_FETCH_LIMIT,
            forceRefresh = forceRefresh,
        )
        val items = page.items
        if (items.isEmpty()) {
            return HomeCatalogSection(
                key = key,
                title = defaultTitle,
                subtitle = addonName,
                addonName = addonName,
                target = CatalogTarget.Addon(
                    manifestUrl = manifestUrl,
                    contentType = type,
                    catalogId = catalogId,
                    supportsPagination = supportsPagination,
                ),
                items = emptyList(),
                availableItemCount = 0,
                hasMore = false,
            )
        }

        return HomeCatalogSection(
            key = key,
            title = defaultTitle,
            subtitle = addonName,
            addonName = addonName,
            target = CatalogTarget.Addon(
                manifestUrl = manifestUrl,
                contentType = type,
                catalogId = catalogId,
                supportsPagination = supportsPagination,
            ),
            items = items,
            availableItemCount = page.rawItemCount,
            hasMore = supportsPagination && page.nextSkip != null,
        )
    }

    private suspend fun loadCloudSections(
        plugins: List<CloudStreamPluginItem>,
    ): CloudHomeSectionsResult {
        val sections = mutableListOf<HomeCatalogSection>()
        var firstError: String? = null
        withTimeoutOrNull(HOME_CLOUDSTREAM_TOTAL_PREVIEW_TIMEOUT_MS) {
            for (plugin in plugins.take(HOME_CLOUDSTREAM_PROVIDER_SCAN_LIMIT)) {
                if (sections.size >= HOME_CLOUDSTREAM_SECTION_PREVIEW_LIMIT) break
                val result = withTimeoutOrNull(HOME_CLOUDSTREAM_PROVIDER_TIMEOUT_MS) {
                    CloudStreamRepository.getMainPage(plugin.metadata.id.value, page = 1)
                }
                if (result == null) {
                    if (firstError == null) firstError = "${plugin.metadata.name} zaman aşımına uğradı"
                    continue
                }
                result.fold(
                    onSuccess = { categories ->
                        categories.forEach { (categoryName, items) ->
                            if (sections.size >= HOME_CLOUDSTREAM_SECTION_PREVIEW_LIMIT) return@forEach
                            if (items.isEmpty()) return@forEach
                            val previews = items.take(HOME_CATALOG_PREVIEW_FETCH_LIMIT).map { it.toMetaPreview() }
                            sections += HomeCatalogSection(
                                key = "cloudstream:${plugin.metadata.id.storageKey}:${categoryName.hashCode()}",
                                title = categoryName,
                                subtitle = "${plugin.metadata.name} · CloudStream",
                                addonName = plugin.metadata.name,
                                target = CatalogTarget.CloudStream(
                                    providerId = plugin.metadata.id.value,
                                    categoryName = categoryName,
                                    contentType = items.first().type.nuvioType,
                                    supportsPagination = false,
                                ),
                                items = previews,
                                availableItemCount = items.size,
                                hasMore = items.size > previews.size,
                            )
                        }
                    },
                    onFailure = { error ->
                        if (firstError == null) firstError = error.message
                    },
                )
            }
        }
        return CloudHomeSectionsResult(sections = sections, errorMessage = firstError)
    }

    private fun ensureCollectionHeroFallback(
        addons: List<ManagedAddon>,
        forceRefresh: Boolean,
        refreshSources: Boolean,
        requestKey: String?,
    ) {
        if (!lastPublishedCatalogHeroEmpty) return
        val snapshot = HomeCatalogSettingsRepository.snapshot()
        if (!snapshot.heroEnabled) return
        val collections = enabledCollectionsForHero(snapshot)
        if (collections.isEmpty()) {
            cachedCollectionHeroItems = emptyList()
            collectionHeroRequestKey = null
            return
        }

        val nextRequestKey = collectionHeroRequestKey(
            collections = collections,
            addons = addons,
            snapshot = snapshot,
            requestKey = requestKey,
        )
        if (!refreshSources && collectionHeroRequestKey == nextRequestKey) return

        collectionHeroJob?.cancel()
        collectionHeroRequestKey = nextRequestKey
        cachedCollectionHeroItems = emptyList()
        publishCurrentState(
            isLoading = _uiState.value.isLoading,
            requestKey = requestKey,
        )

        collectionHeroJob = scope.launch {
            val sources = collectionHeroSources(collections)
            val sourceResults = sources.map { source ->
                async {
                    try {
                        withTimeoutOrNull(HOME_COLLECTION_HERO_SOURCE_TIMEOUT_MS) {
                            source.resolveCollectionHeroItems(addons)
                        }.orEmpty()
                    } catch (error: Throwable) {
                        if (error is CancellationException) throw error
                        emptyList()
                    }
                }
            }.awaitAll()
            val random = Random((nextRequestKey.hashCode()).absoluteValue + 7)
            cachedCollectionHeroItems = roundRobinCollectionHeroItems(sourceResults)
                .distinctBy { item -> item.stableKey() }
                .shuffled(random)
                .take(HOME_HERO_ITEM_LIMIT)
            publishCurrentState(
                isLoading = _uiState.value.isLoading,
                requestKey = requestKey,
            )
        }
    }

    private fun enabledCollectionsForHero(snapshot: HomeCatalogSettingsSnapshot): List<Collection> {
        val preferences = snapshot.preferences
        return CollectionRepository.collections.value
            .filter { collection ->
                collection.folders.isNotEmpty() &&
                    preferences["collection_${collection.id}"]?.enabled != false
            }
            .sortedBy { collection ->
                preferences["collection_${collection.id}"]?.order ?: Int.MAX_VALUE
            }
    }

    private fun collectionHeroSources(collections: List<Collection>): List<CollectionSource> =
        collections
            .flatMap { collection -> collection.folders }
            .flatMap { folder -> folder.resolvedSources }
            .take(HOME_COLLECTION_HERO_SOURCE_LIMIT)

    private suspend fun CollectionSource.resolveCollectionHeroItems(
        addons: List<ManagedAddon>,
        forceRefresh: Boolean,
    ): List<MetaPreview> {
        val page = when {
            isTmdb -> TmdbCollectionSourceResolver.resolveOrEmpty(source = this, page = 1)
            isTrakt -> TraktPublicListSourceResolver.resolveOrEmpty(source = this, page = 1)
            else -> {
                val catalogSource = addonCatalogSource() ?: return emptyList()
                val resolvedCatalog = addons.findCollectionCatalog(catalogSource) ?: return emptyList()
                fetchCatalogPage(
                    manifestUrl = resolvedCatalog.addon.manifestUrl,
                    type = catalogSource.type,
                    catalogId = catalogSource.catalogId,
                    genre = catalogSource.genre,
                    maxItems = HOME_COLLECTION_HERO_SOURCE_ITEM_LIMIT,
                    forceRefresh = forceRefresh,
                )
            }
        }
        val items = page.items
        return if (HomeCatalogSettingsRepository.snapshot().hideUnreleasedContent) {
            items.filterReleasedItems(CurrentDateProvider.todayIsoDate())
        } else {
            items
        }
    }

    private fun roundRobinCollectionHeroItems(sourceResults: List<List<MetaPreview>>): List<MetaPreview> {
        val iterators = sourceResults.filter { it.isNotEmpty() }.map { it.iterator() }
        if (iterators.isEmpty()) return emptyList()
        val merged = mutableListOf<MetaPreview>()
        var hasMore = true
        while (hasMore && merged.size < HOME_COLLECTION_HERO_SOURCE_LIMIT * HOME_COLLECTION_HERO_SOURCE_ITEM_LIMIT) {
            hasMore = false
            iterators.forEach { iterator ->
                if (iterator.hasNext()) {
                    merged.add(iterator.next())
                    hasMore = true
                }
            }
        }
        return merged
    }

    private fun collectionHeroRequestKey(
        collections: List<Collection>,
        addons: List<ManagedAddon>,
        snapshot: HomeCatalogSettingsSnapshot,
        requestKey: String?,
    ): String = buildString {
        append(requestKey.orEmpty())
        append("|hideUnreleased=")
        append(snapshot.hideUnreleasedContent)
        append("|collections=")
        collections.forEach { collection ->
            val preference = snapshot.preferences["collection_${collection.id}"]
            append(collection.id)
            append(":")
            append(preference?.order ?: Int.MAX_VALUE)
            append(":")
            collection.folders.forEach { folder ->
                append(folder.id)
                append("[")
                folder.resolvedSources.forEach { source ->
                    append(collectionSourceKey(source))
                    append(",")
                }
                append("]")
            }
            append(";")
        }
        append("|addons=")
        addons.forEach { addon ->
            append(addon.manifest?.id.orEmpty())
            append(":")
            append(addon.manifestUrl)
            append(":")
            append(addon.manifest?.catalogs?.size ?: 0)
            append(";")
        }
    }

    private fun collectionSourceKey(source: CollectionSource): String =
        source.catalogRouteKey()
}

private const val HOME_HERO_ITEM_LIMIT = 8
private const val HOME_COLLECTION_HERO_SOURCE_LIMIT = 6
private const val HOME_COLLECTION_HERO_SOURCE_ITEM_LIMIT = 8
private const val HOME_CATALOG_FETCH_BATCH_SIZE = 4
private const val HOME_CATALOG_PREVIEW_FETCH_LIMIT = 18
private const val HOME_CATALOG_PUBLISH_INTERVAL = 2
private const val HOME_CLOUDSTREAM_PROVIDER_SCAN_LIMIT = 18
private const val HOME_CLOUDSTREAM_SECTION_PREVIEW_LIMIT = 8
private const val HOME_CLOUDSTREAM_PROVIDER_TIMEOUT_MS = 5_000L
private const val HOME_CLOUDSTREAM_TOTAL_PREVIEW_TIMEOUT_MS = 15_000L
private const val HOME_CATALOG_REQUEST_TIMEOUT_MS = 12_000L
private const val HOME_COLLECTION_HERO_SOURCE_TIMEOUT_MS = 10_000L
private const val HOME_HERO_LOCALIZED_ARTWORK_BATCH_SIZE = 3
private const val HOME_HERO_INITIAL_LOCALIZED_ARTWORK_TIMEOUT_MS = 1_500L
private const val HOME_HERO_LOCALIZED_ARTWORK_TIMEOUT_MS = 4_000L

private fun prioritizeDefinitions(
    definitions: List<HomeCatalogDefinition>,
    snapshot: HomeCatalogSettingsSnapshot,
): List<HomeCatalogDefinition> {
    val orderedDefinitions = definitions.sortedBy { definition ->
        snapshot.preferences[definition.key]?.order ?: Int.MAX_VALUE
    }
    val (priority, remainder) = orderedDefinitions.partition { definition ->
        val preference = snapshot.preferences[definition.key]
        if (preference == null) {
            true
        } else {
            preference.enabled || (snapshot.heroEnabled && preference.heroSourceEnabled)
        }
    }
    return priority + remainder
}
