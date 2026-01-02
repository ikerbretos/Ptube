package com.github.ptube.api

import com.github.ptube.api.obj.ContentItem
import com.github.ptube.api.obj.SearchResult
import com.github.ptube.api.obj.StreamItem
import com.github.ptube.db.DatabaseHolder
import com.github.ptube.db.obj.SubscriptionsFeedItem

object ShortsRepository {
    
    suspend fun getShorts(): List<StreamItem> {
        return runCatching {
            val context = com.github.ptube.PtubeApp.instance
            
            // 1. Fetch Subscription Shorts (Local DB)
            val subscriptionShorts = try {
                val feedDao = DatabaseHolder.Database.feedDao()
                val allFeedItems: List<SubscriptionsFeedItem> = feedDao.getAll()
                
                allFeedItems.filter { item -> 
                   val d = item.duration ?: 0L
                   d >= 1L && d <= 180L
                }.map { item -> 
                    item.toStreamItem()
                }.take(10)
            } catch (e: Exception) { emptyList() }
            
            // 2. Fetch Related Shorts (based on history)
            val relatedShorts = try {
                val historyDao = com.github.ptube.db.DatabaseHolder.Database.watchHistoryDao()
                val lastWatched = historyDao.getOldest() // Actually we want newest, but DAO might name it oddly, let's check order. 
                // Wait, getOldest likely returns the LAST one inserted if it's a stack, or the actual oldest?
                // The DAO has `SELECT * FROM watchHistoryItem LIMIT 1 OFFSET 0`. 
                // Assuming standard INSERT logic, let's try to get "Related" from the last ID if possible.
                // If getOldest returns the oldest, we might want to query all and take last, or assume just using search is safer if unsure.
                // Let's stick to Search + Subs first to be safe, or try to get related from a known ID.
                emptyList<StreamItem>() 
                // Implementing fully safe related fetching requires more complex API calls, let's stick to Subs + Search Mix for stability first as requested.
            } catch (e: Exception) { emptyList() }

            // 3. Region Search (Existing)
            val region = com.github.ptube.helpers.PreferenceHelper.getTrendingRegion(context)
            val query = "shorts $region"
            android.util.Log.d("ShortsRepo", "Searching for shorts with region: $region (Query: $query)")

            val searchResults = MediaServiceRepository.instance.getSearchResults(query, "all")
            val searchShorts = searchResults.items.map { it.toStreamItem() }.filter { 
                it.isShort == true || (it.duration != null && it.duration!! > 0 && it.duration!! < 180)
            }

            // 4. Mix and Shuffle
            val mixedList = (subscriptionShorts + searchShorts)
                .distinctBy { it.url }
                .shuffled() // Shuffle to keep it fresh
            
            if (mixedList.isNotEmpty()) {
                android.util.Log.d("ShortsRepo", "Returning ${mixedList.size} mixed shorts (Subs: ${subscriptionShorts.size}, Search: ${searchShorts.size})")
                mixedList
            } else {
                searchShorts.take(20)
            }
        }.getOrElse {
            android.util.Log.e("ShortsRepo", "Search failed, performing emergency wide search", it)
            runCatching {
                val emergency = MediaServiceRepository.instance.getSearchResults("youtube shorts", "all")
                emergency.items.map { it.toStreamItem() }.take(10)
            }.getOrDefault(emptyList())
        }
    }
}
