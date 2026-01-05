package com.github.ptube.ui.models

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ptube.api.MediaServiceRepository
import com.github.ptube.api.PlaylistsHelper
import com.github.ptube.api.SubscriptionHelper
import com.github.ptube.api.TrendingCategory
import com.github.ptube.api.obj.Playlists
import com.github.ptube.api.obj.StreamItem
import com.github.ptube.constants.PreferenceKeys
import com.github.ptube.db.DatabaseHelper
import com.github.ptube.db.DatabaseHolder
import com.github.ptube.db.obj.PlaylistBookmark
import com.github.ptube.extensions.runSafely
import com.github.ptube.extensions.updateIfChanged
import com.github.ptube.helpers.PlayerHelper
import com.github.ptube.helpers.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {
    private val hideWatched get() = PreferenceHelper.getBoolean(PreferenceKeys.HIDE_WATCHED_FROM_FEED, false)
    private val showUpcoming get() = PreferenceHelper.getBoolean(PreferenceKeys.SHOW_UPCOMING_IN_FEED, true)

    val trending: MutableLiveData<Pair<TrendingCategory, TrendsViewModel.TrendingStreams>> =
        MutableLiveData(null)
    val feed: MutableLiveData<List<StreamItem>> = MutableLiveData(null)
    val shorts: MutableLiveData<List<StreamItem>> = MutableLiveData(null)
    val bookmarks: MutableLiveData<List<PlaylistBookmark>> = MutableLiveData(null)
    val playlists: MutableLiveData<List<Playlists>> = MutableLiveData(null)
    val continueWatching: MutableLiveData<List<StreamItem>> = MutableLiveData(null)
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(true)
    val loadedSuccessfully: MutableLiveData<Boolean> = MutableLiveData(false)

    private val sections get() = listOf(trending, feed, shorts, bookmarks, playlists, continueWatching)

    private var loadHomeJob: Job? = null

    fun loadHomeFeed(
        context: Context,
        subscriptionsViewModel: SubscriptionsViewModel,
        visibleItems: Set<String>,
        onUnusualLoadTime: () -> Unit
    ) {
        isLoading.value = true

        loadHomeJob?.cancel()
        loadHomeJob = viewModelScope.launch {
            val result = async {
                awaitAll(
                    async { if (visibleItems.contains(TRENDING)) loadTrending(context) },
                    async { if (visibleItems.contains(FEATURED)) loadFeed(subscriptionsViewModel) },
                    async { if (visibleItems.contains(BOOKMARKS)) loadBookmarks() },
                    async { if (visibleItems.contains(PLAYLISTS)) loadPlaylists() },
                    async { if (visibleItems.contains(WATCHING)) loadVideosToContinueWatching() }
                )
                
                // Load Shorts independently so it doesn't block the main feed if it takes longer
                launch { loadShorts() }
                
                loadedSuccessfully.value = sections.filter { it != shorts }.any { it.value != null }
                isLoading.value = false
            }

            withContext(Dispatchers.IO) {
                delay(UNUSUAL_LOAD_TIME_MS)
                if (result.isActive) {
                    onUnusualLoadTime.invoke()
                }
            }
        }
    }
    private suspend fun loadTrending(context: Context) {
        val region = PreferenceHelper.getTrendingRegion(context)
        val category = PreferenceHelper.getString(
            PreferenceKeys.TRENDING_CATEGORY,
            TrendingCategory.ALL.name
        ).let { TrendingCategory.valueOf(it) }

        runSafely(
            onSuccess = { videos ->
                trending.updateIfChanged(
                    Pair(
                        category,
                        TrendsViewModel.TrendingStreams(region, videos)
                    )
                )
            },
            ioBlock = {
                MediaServiceRepository.instance.getTrending(region, category)
            }
        )
    }

    private suspend fun loadFeed(subscriptionsViewModel: SubscriptionsViewModel) {
        runSafely(
            onSuccess = { videos -> feed.updateIfChanged(videos) },
            ioBlock = { tryLoadFeed(subscriptionsViewModel) }
        )
    }

    suspend fun loadShorts(append: Boolean = false) {
        runSafely(
            onSuccess = { items -> 
                if (append) {
                    val current = shorts.value.orEmpty().toMutableList()
                    val newItems = items.filter { newItem -> current.none { it.url == newItem.url } }
                    current.addAll(newItems)
                    shorts.updateIfChanged(current)
                } else {
                    shorts.updateIfChanged(items) 
                }
            },
            ioBlock = { com.github.ptube.api.ShortsRepository.getShorts(append) }
        )
    }

    fun refreshShorts() {
        viewModelScope.launch {
            loadShorts(append = false)
        }
    }
    
    fun loadMoreShorts() {
        if (isLoading.value == true) return
        viewModelScope.launch {
            // Check avoiding duplicate parallel loads could be good, but single threaded viewmodel scope helps
             loadShorts(append = true)
        }
    }

    private suspend fun loadBookmarks() {
        runSafely(
            onSuccess = { newBookmarks -> bookmarks.updateIfChanged(newBookmarks) },
            ioBlock = { DatabaseHolder.Database.playlistBookmarkDao().getAll() }
        )
    }

    private suspend fun loadPlaylists() {
        runSafely(
            onSuccess = { newPlaylists -> playlists.updateIfChanged(newPlaylists) },
            ioBlock = { PlaylistsHelper.getPlaylists() }
        )
    }

    private suspend fun loadVideosToContinueWatching() {
        if (!PlayerHelper.watchHistoryEnabled) return
        runSafely(
            onSuccess = { videos -> continueWatching.updateIfChanged(videos) },
            ioBlock = ::loadWatchingFromDB
        )
    }

    private suspend fun loadWatchingFromDB(): List<StreamItem> {
        val videos = DatabaseHelper.getWatchHistoryPage(1, 20)

        return DatabaseHelper
            .filterUnwatched(videos.map { it.toStreamItem() })
    }

    private suspend fun tryLoadFeed(subscriptionsViewModel: SubscriptionsViewModel): List<StreamItem> {
        subscriptionsViewModel.videoFeed.value?.let { return it }

        val feed = SubscriptionHelper.getFeed(forceRefresh = false)
        subscriptionsViewModel.videoFeed.postValue(feed)

        return DatabaseHelper.filterByStreamTypeAndWatchPosition(feed, hideWatched, showUpcoming)
    }

    fun loadMore(subscriptionsViewModel: SubscriptionsViewModel) {
        if (isLoading.value == true) return

        viewModelScope.launch {
            // Fetch more Shorts
            val newShorts = withContext(Dispatchers.IO) {
                try {
                    com.github.ptube.api.ShortsRepository.getShorts(append = true)
                } catch (e: Exception) {
                    emptyList()
                }
            }
            if (newShorts.isNotEmpty()) {
                val current = shorts.value.orEmpty()
                shorts.postValue((current + newShorts).distinctBy { it.url })
            }

            // Fetch more Videos (Trending/Feed)
            val newVideos = withContext(Dispatchers.IO) {
                try {
                    tryLoadFeed(subscriptionsViewModel)
                } catch (e: Exception) {
                    emptyList()
                }
            }
            if (newVideos.isNotEmpty()) {
                val current = feed.value.orEmpty()
                feed.postValue((current + newVideos).distinctBy { it.url })
            }
        }
    }

    companion object {
        private const val UNUSUAL_LOAD_TIME_MS = 10000L
        private const val FEATURED = "featured"
        private const val WATCHING = "watching"
        private const val TRENDING = "trending"
        private const val BOOKMARKS = "bookmarks"
        private const val PLAYLISTS = "playlists"
    }
}
