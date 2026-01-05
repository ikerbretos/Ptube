package com.github.ptube.api

import com.github.ptube.api.obj.ContentItem
import com.github.ptube.api.obj.SearchResult
import com.github.ptube.api.obj.StreamItem
import com.github.ptube.db.DatabaseHolder
import com.github.ptube.db.obj.SubscriptionsFeedItem

object ShortsRepository {
    
    private var lastContinuation: String? = null
    
    // Create InnerTubeApi instance directly since we need it here
    private val innerTubeApi by lazy {
        RetrofitInstance.buildRetrofitInstance<InnerTubeApi>("https://www.youtube.com/")
    }

    suspend fun getNextShortsBatch(forceRefresh: Boolean = false): List<StreamItem> {
        return runCatching {
            if (forceRefresh) lastContinuation = null
            
            // If we have no token, we start fresh (browseId="FEShorts", continuation=null)
            // If we have token, usually browseId must be null or "FEShorts" depending on endpoint behavior but often just continuation is enough.
            // InnerTubeApi Body handles browseId="FEShorts" by default, so we set it to null if using continuation?
            // Actually, usually you pass ONE: either browseId OR continuation.
            val browseId = if (lastContinuation == null) "FEShorts" else null
            val continuation = lastContinuation

            val body = InnerTubeApi.InnerTubeBody(
                browseId = browseId,
                continuation = continuation
            )

            val response = innerTubeApi.getShorts(body)
            
            // 1. Extract Items
            val items = mutableListOf<StreamItem>()
            
            // Items can be in contents -> richGridRenderer -> contents
            // OR in onResponseReceivedActions (continuation)
            
            // Items can be in contents -> richGridRenderer -> contents
            // OR in onResponseReceivedActions (continuation)
            
            val richItems = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.richGridRenderer?.contents
                ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.richGridRenderer?.contents
                ?: emptyList()
            
            // Extract from RichItems (Main page)
            richItems.forEach { item ->
                item.richItemRenderer?.content?.reelItemRenderer?.let { renderer ->
                    items.add(renderer.toStreamItem())
                }
            }

            // Extract from Continuation Actions (Next Page)
            // Now that we updated InnerTubeApi to use List<RichItem> for continuationItems:
            response.onResponseReceivedActions?.forEach { action ->
                action.appendContinuationItemsAction?.continuationItems?.forEach { item ->
                    item.richItemRenderer?.content?.reelItemRenderer?.let { renderer ->
                        items.add(renderer.toStreamItem())
                    }
                }
            }
            
            // 2. Extract Token
            // Token can be in:
            // A) richGridRenderer -> contents -> last item -> continuationItemRenderer -> ...
            // B) onResponseReceivedActions -> ... -> continuationItems -> last item -> ...
            
            var newToken: String? = null
            
            // Check RichGrid (First Page - Desktop & Mobile)
            val tabRenderer = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer
                ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer
            
            tabRenderer?.content?.richGridRenderer?.contents?.lastOrNull()?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token?.let {
                newToken = it
            }
            
            // Check Actions (Next Page)
            response.onResponseReceivedActions?.forEach { action ->
                 action.appendContinuationItemsAction?.continuationItems?.lastOrNull()?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token?.let {
                     newToken = it
                 }
            }
            
            lastContinuation = newToken

            // Filter watched
            val historyDao = DatabaseHolder.Database.watchHistoryDao()
            val watchedIds = historyDao.getAll().map { it.videoId }.toSet()
            
            items.filter { 
               !watchedIds.contains(it.url.orEmpty().toID())
            }.distinctBy { it.url }

        }.getOrElse {
            android.util.Log.e("ShortsRepo", "Continuation fetch failed", it)
            emptyList()
        }
    }
    
    // Helper to map InnerTube Reel to StreamItem
    private fun InnerTubeApi.ReelItemRenderer.toStreamItem(): StreamItem {
        return StreamItem(
            url = "https://www.youtube.com/shorts/$videoId",
            title = headline?.text ?: "",
            uploaderName = navigationEndpoint?.reelWatchEndpoint?.overlay?.reelPlayerOverlayRenderer?.reelPlayerHeaderSupportedRenderers?.reelPlayerHeaderRenderer?.channelTitleText?.text ?: "",
            thumbnail = thumbnail?.thumbnails?.maxByOrNull { it.width }?.url ?: "",
            uploaderAvatar = navigationEndpoint?.reelWatchEndpoint?.overlay?.reelPlayerOverlayRenderer?.reelPlayerHeaderSupportedRenderers?.reelPlayerHeaderRenderer?.channelThumbnail?.thumbnails?.firstOrNull()?.url,
            duration = 60, // detailed duration not always available in browse
            uploaderUrl = "",
            isShort = true
        )
    }
    
    // For compatibility with HomeViewModel
    suspend fun getShorts(append: Boolean): List<StreamItem> {
        return getNextShortsBatch(forceRefresh = !append)
    }

    // Helper for toID
    private fun String.toID(): String = com.github.ptube.util.TextUtils.getVideoIdFromUri(android.net.Uri.parse(this)) ?: ""
}
