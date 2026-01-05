package com.github.ptube.util

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import com.github.ptube.api.MediaServiceRepository
import com.github.ptube.api.obj.StreamItem
import com.github.ptube.api.obj.Streams
import com.github.ptube.extensions.toID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@UnstableApi
object ShortsPreloader {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val jobs = mutableMapOf<String, Job>()
    
    // In-memory cache for Streams metadata to avoid duplicate API calls
    private val streamsCache = mutableMapOf<String, Streams>()
    private val mutex = Mutex()

    suspend fun getStreams(videoId: String): Streams? {
        mutex.withLock {
            return streamsCache[videoId]
        }
    }

    private suspend fun cacheStreams(videoId: String, streams: Streams) {
        mutex.withLock {
            streamsCache[videoId] = streams
        }
    }

    fun preload(context: Context, items: List<StreamItem>, currentIndex: Int) {
        // Preload next 3 shorts
        val nextIndices = listOf(currentIndex + 1, currentIndex + 2, currentIndex + 3)
        
        for (index in nextIndices) {
            if (index < items.size) {
                val item = items[index]
                val videoId = item.url.orEmpty().toID()
                if (videoId.isEmpty()) continue
                
                synchronized(jobs) {
                    if (jobs.containsKey(videoId)) return@synchronized
                    
                    val job = scope.launch {
                        try {
                            Log.d("ShortsPreloader", "Preloading video info for: $videoId")
                            val streams = MediaServiceRepository.instance.getStreams(videoId).let {
                                com.github.ptube.util.DeArrowUtil.deArrowStreams(it, videoId)
                            }
                            cacheStreams(videoId, streams)
                            
                            // Get best available direct URI for caching
                            // Priority: Dash > HLS > 1st video stream
                            val uri = streams.dash ?: streams.hls ?: streams.videoStreams.firstOrNull()?.url
                            
                            if (uri != null) {
                                Log.d("ShortsPreloader", "Preloading first 512KB for: $videoId")
                                val dataSource = CacheManager.getDataSourceFactory(context).createDataSource() as androidx.media3.datasource.cache.CacheDataSource
                                val dataSpec = DataSpec.Builder()
                                    .setUri(uri.toUri())
                                    .setLength(512 * 1024) // 512KB is usually enough for instant start
                                    .build()
                                
                                val cacheWriter = CacheWriter(
                                    dataSource,
                                    dataSpec,
                                    null,
                                    null
                                )
                                // This will download into SimpleCache
                                cacheWriter.cache()
                                Log.d("ShortsPreloader", "Successfully preloaded: $videoId")
                            }
                        } catch (e: Exception) {
                            Log.e("ShortsPreloader", "Preload failed for $videoId", e)
                        }
                    }
                    jobs[videoId] = job
                }
            }
        }
    }
}
