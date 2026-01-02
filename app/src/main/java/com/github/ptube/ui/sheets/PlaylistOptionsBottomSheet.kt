package com.github.ptube.ui.sheets

import android.os.Bundle
import androidx.core.os.bundleOf
import com.github.ptube.R
import com.github.ptube.api.MediaServiceRepository
import com.github.ptube.api.PlaylistsHelper
import com.github.ptube.constants.IntentData
import com.github.ptube.db.DatabaseHolder
import com.github.ptube.enums.ImportFormat
import com.github.ptube.enums.PlaylistType
import com.github.ptube.enums.ShareObjectType
import com.github.ptube.extensions.serializable
import com.github.ptube.extensions.toID
import com.github.ptube.extensions.toastFromMainDispatcher
import com.github.ptube.helpers.BackgroundHelper
import com.github.ptube.helpers.ContextHelper
import com.github.ptube.helpers.DownloadHelper
import com.github.ptube.obj.ShareData
import com.github.ptube.ui.activities.MainActivity
import com.github.ptube.ui.base.BaseActivity
import com.github.ptube.ui.dialogs.DeletePlaylistDialog
import com.github.ptube.ui.dialogs.PlaylistDescriptionDialog
import com.github.ptube.ui.dialogs.RenamePlaylistDialog
import com.github.ptube.ui.dialogs.ShareDialog
import com.github.ptube.ui.preferences.BackupRestoreSettings
import com.github.ptube.util.PlayingQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class PlaylistOptionsBottomSheet : BaseBottomSheet() {
    private lateinit var playlistName: String
    private lateinit var playlistId: String
    private lateinit var playlistType: PlaylistType

    private var exportFormat: ImportFormat = ImportFormat.NEWPIPE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            playlistName = it.getString(IntentData.playlistName)!!
            playlistId = it.getString(IntentData.playlistId)!!
            playlistType = it.serializable(IntentData.playlistType)!!
        }

        setTitle(playlistName)

        // options for the dialog
        val optionsList = mutableListOf(R.string.playOnBackground, R.string.download)

        if (PlayingQueue.isNotEmpty()) optionsList.add(R.string.add_to_queue)

        val isBookmarked = runBlocking(Dispatchers.IO) {
            DatabaseHolder.Database.playlistBookmarkDao().includes(playlistId)
        }

        if (playlistType == PlaylistType.PUBLIC) {
            optionsList.add(R.string.share)
            optionsList.add(R.string.clonePlaylist)

            // only add the bookmark option to the playlist if public
            optionsList.add(
                if (isBookmarked) R.string.remove_bookmark else R.string.add_to_bookmarks
            )
        } else {
            optionsList.add(R.string.export_playlist)
            optionsList.add(R.string.renamePlaylist)
            optionsList.add(R.string.change_playlist_description)
            optionsList.add(R.string.deletePlaylist)
        }

        setSimpleItems(optionsList.map { getString(it) }) { which ->
            val mFragmentManager = (context as BaseActivity).supportFragmentManager

            when (optionsList[which]) {
                // play the playlist in the background
                R.string.playOnBackground -> {
                    val playlist = withContext(Dispatchers.IO) {
                        runCatching { PlaylistsHelper.getPlaylist(playlistId) }
                    }.getOrElse {
                        context?.toastFromMainDispatcher(R.string.error)
                        return@setSimpleItems
                    }

                    playlist.relatedStreams.firstOrNull()?.let {
                        BackgroundHelper.playOnBackground(
                            requireContext(),
                            it.url!!.toID(),
                            playlistId = playlistId
                        )
                    }
                }

                R.string.add_to_queue -> {
                    PlayingQueue.insertPlaylist(playlistId, null)
                }
                // Clone the playlist to the users Piped account
                R.string.clonePlaylist -> {
                    val context = requireContext()
                    val playlistId = withContext(Dispatchers.IO) {
                        runCatching {
                            PlaylistsHelper.clonePlaylist(playlistId)
                        }.getOrNull()
                    }
                    context.toastFromMainDispatcher(
                        if (playlistId != null) R.string.playlistCloned else R.string.server_error
                    )
                }
                // share the playlist
                R.string.share -> {
                    val newShareDialog = ShareDialog()
                    newShareDialog.arguments = bundleOf(
                        IntentData.id to playlistId,
                        IntentData.shareObjectType to ShareObjectType.PLAYLIST,
                        IntentData.shareData to ShareData(currentPlaylist = playlistName)
                    )
                    // using parentFragmentManager, childFragmentManager doesn't work here
                    newShareDialog.show(parentFragmentManager, ShareDialog::class.java.name)
                }

                R.string.deletePlaylist -> {
                    val newDeletePlaylistDialog = DeletePlaylistDialog()
                    newDeletePlaylistDialog.arguments = bundleOf(
                        IntentData.playlistId to playlistId
                    )
                    newDeletePlaylistDialog.show(mFragmentManager, null)
                }

                R.string.renamePlaylist -> {
                    val newRenamePlaylistDialog = RenamePlaylistDialog()
                    newRenamePlaylistDialog.arguments = bundleOf(
                        IntentData.playlistId to playlistId,
                        IntentData.playlistName to playlistName
                    )
                    newRenamePlaylistDialog.show(mFragmentManager, null)
                }

                R.string.change_playlist_description -> {
                    val newPlaylistDescriptionDialog = PlaylistDescriptionDialog()
                    newPlaylistDescriptionDialog.arguments = bundleOf(
                        IntentData.playlistId to playlistId,
                        IntentData.playlistDescription to ""
                    )
                    newPlaylistDescriptionDialog.show(mFragmentManager, null)
                }

                R.string.download -> {
                    DownloadHelper.startDownloadPlaylistDialog(
                        requireContext(),
                        mFragmentManager,
                        playlistId,
                        playlistName,
                        playlistType
                    )
                }

                R.string.export_playlist -> {
                    val context = requireContext()

                    BackupRestoreSettings.createImportFormatDialog(
                        context,
                        R.string.export_playlist,
                        BackupRestoreSettings.exportPlaylistFormatList + listOf(ImportFormat.URLSORIDS)
                    ) { format, includeTimestamp ->
                        exportFormat = format
                        ContextHelper.unwrapActivity<MainActivity>(context)
                            .startPlaylistExport(playlistId, playlistName, exportFormat, includeTimestamp)
                    }
                }

                else -> {
                    withContext(Dispatchers.IO) {
                        if (isBookmarked) {
                            DatabaseHolder.Database.playlistBookmarkDao().deleteById(playlistId)
                        } else {
                            val bookmark = try {
                                MediaServiceRepository.instance.getPlaylist(playlistId)
                            } catch (e: Exception) {
                                return@withContext
                            }.toPlaylistBookmark(playlistId)
                            DatabaseHolder.Database.playlistBookmarkDao().insert(bookmark)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val PLAYLIST_OPTIONS_REQUEST_KEY = "playlist_options_request_key"
    }
}
