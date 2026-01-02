package com.github.ptube.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.ptube.db.dao.CustomInstanceDao
import com.github.ptube.db.dao.DownloadDao
import com.github.ptube.db.dao.LocalPlaylistsDao
import com.github.ptube.db.dao.LocalSubscriptionDao
import com.github.ptube.db.dao.PlaylistBookmarkDao
import com.github.ptube.db.dao.SearchHistoryDao
import com.github.ptube.db.dao.SubscriptionGroupsDao
import com.github.ptube.db.dao.SubscriptionsFeedDao
import com.github.ptube.db.dao.WatchHistoryDao
import com.github.ptube.db.dao.WatchPositionDao
import com.github.ptube.db.obj.CustomInstance
import com.github.ptube.db.obj.Download
import com.github.ptube.db.obj.DownloadChapter
import com.github.ptube.db.obj.DownloadItem
import com.github.ptube.db.obj.DownloadPlaylist
import com.github.ptube.db.obj.DownloadPlaylistVideosCrossRef
import com.github.ptube.db.obj.LocalPlaylist
import com.github.ptube.db.obj.LocalPlaylistItem
import com.github.ptube.db.obj.LocalSubscription
import com.github.ptube.db.obj.PlaylistBookmark
import com.github.ptube.db.obj.SearchHistoryItem
import com.github.ptube.db.obj.SubscriptionGroup
import com.github.ptube.db.obj.SubscriptionsFeedItem
import com.github.ptube.db.obj.WatchHistoryItem
import com.github.ptube.db.obj.WatchPosition

@Database(
    entities = [
        WatchHistoryItem::class,
        WatchPosition::class,
        SearchHistoryItem::class,
        CustomInstance::class,
        LocalSubscription::class,
        PlaylistBookmark::class,
        LocalPlaylist::class,
        LocalPlaylistItem::class,
        Download::class,
        DownloadItem::class,
        DownloadChapter::class,
        DownloadPlaylist::class,
        DownloadPlaylistVideosCrossRef::class,
        SubscriptionGroup::class,
        SubscriptionsFeedItem::class
    ],
    version = 21,
    autoMigrations = [
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Watch History
     */
    abstract fun watchHistoryDao(): WatchHistoryDao

    /**
     * Watch Positions
     */
    abstract fun watchPositionDao(): WatchPositionDao

    /**
     * Search History
     */
    abstract fun searchHistoryDao(): SearchHistoryDao

    /**
     * Custom Instances
     */
    abstract fun customInstanceDao(): CustomInstanceDao

    /**
     * Local Subscriptions
     */
    abstract fun localSubscriptionDao(): LocalSubscriptionDao

    /**
     * Bookmarked Playlists
     */
    abstract fun playlistBookmarkDao(): PlaylistBookmarkDao

    /**
     * Local playlists
     */
    abstract fun localPlaylistsDao(): LocalPlaylistsDao

    /**
     * Downloads
     */
    abstract fun downloadDao(): DownloadDao

    /**
     * Subscription groups
     */
    abstract fun subscriptionGroupsDao(): SubscriptionGroupsDao

    /**
     * Locally cached subscription feed
     */
    abstract fun feedDao(): SubscriptionsFeedDao
}
