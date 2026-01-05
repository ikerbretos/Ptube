package com.github.ptube.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ListAdapter
import com.github.ptube.api.obj.StreamItem
import com.github.ptube.constants.IntentData
import com.github.ptube.databinding.AllCaughtUpRowBinding
import com.github.ptube.databinding.TrendingRowBinding
import com.github.ptube.extensions.dpToPx
import com.github.ptube.extensions.toID
import com.github.ptube.extensions.formatShort
import com.github.ptube.helpers.ImageHelper
import com.github.ptube.helpers.NavigationHelper
import com.github.ptube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.ptube.ui.base.BaseActivity
import com.github.ptube.ui.extensions.setFormattedDuration
import com.github.ptube.ui.extensions.setWatchProgressLength
import com.github.ptube.ui.sheets.VideoOptionsBottomSheet
import com.github.ptube.ui.viewholders.VideoCardsViewHolder
import com.github.ptube.util.DeArrowUtil
import com.github.ptube.util.TextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoCardsAdapter(private val columnWidthDp: Float? = null) :
    ListAdapter<StreamItem, VideoCardsViewHolder>(DiffUtilItemCallback()) {

    override fun getItemViewType(position: Int): Int {
        val item = currentList[position]
        return when {
            item.type == StreamItem.TYPE_HEADER -> HEADER_TYPE
            item.type == CAUGHT_UP_STREAM_TYPE -> CAUGHT_UP_TYPE
            item.isShort -> SHORTS_TYPE
            else -> NORMAL_TYPE
        }
    }

    fun removeItemById(id: String) {
        val list = currentList.toMutableList()
        val index = list.indexOfFirst { it.url.orEmpty().toID() == id }
        if (index != -1) {
            list.removeAt(index)
            submitList(list)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoCardsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            HEADER_TYPE -> VideoCardsViewHolder(
                // Using ViewBinding for header
                 com.github.ptube.databinding.ItemHomeHeaderBinding.inflate(layoutInflater, parent, false)
            )
            CAUGHT_UP_TYPE -> VideoCardsViewHolder(
                AllCaughtUpRowBinding.inflate(layoutInflater, parent, false)
            )
            SHORTS_TYPE -> VideoCardsViewHolder(
                layoutInflater.inflate(com.github.ptube.R.layout.shorts_grid_item, parent, false)
            )
            else -> VideoCardsViewHolder(
                TrendingRowBinding.inflate(layoutInflater, parent, false)
            )
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VideoCardsViewHolder, position: Int) {
        val video = getItem(holder.bindingAdapterPosition)
        
        if (getItemViewType(position) == HEADER_TYPE) {
            holder.itemHomeHeaderBinding?.apply {
                headerTitle.text = video.title
            }
            return
        }
        
        val videoId = video.url.orEmpty().toID()

        if (getItemViewType(position) == SHORTS_TYPE) {
             val view = holder.itemView
             val title = view.findViewById<android.widget.TextView>(com.github.ptube.R.id.title)
             val views = view.findViewById<android.widget.TextView>(com.github.ptube.R.id.views)
             val thumbnail = view.findViewById<android.widget.ImageView>(com.github.ptube.R.id.thumbnail)
             
             title.text = video.title
             views.text = (video.views?.formatShort() ?: "0") + " views"
             ImageHelper.loadImage(video.thumbnail, thumbnail)
             view.setOnClickListener {
                  NavigationHelper.navigateShorts(view.context, videoId, video)
             }
             return
        }

        // ... existing normal video binding ...
        val context = (holder.trendingRowBinding ?: holder.allCaughtUpBinding)?.root?.context
        if (context == null) return // Safety check
        
        val activity = context as? BaseActivity
        val fragmentManager = activity?.supportFragmentManager ?: (context as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager
        
        if (fragmentManager == null || activity == null) return

        holder.trendingRowBinding?.apply {
            // set a fixed width for better visuals
            if (columnWidthDp != null) {
                root.updateLayoutParams {
                    width = columnWidthDp.dpToPx()
                }
            }
            watchProgress.setWatchProgressLength(videoId, video.duration ?: 0L)

            textViewTitle.text = video.title
            textViewChannel.text = TextUtils.formatViewsString(
                root.context,
                video.views ?: -1,
                video.uploaded,
                video.uploaderName
            )

            video.duration?.let {
                thumbnailDuration.setFormattedDuration(
                    it,
                    video.isShort,
                    video.uploaded
                )
            }
            ImageHelper.loadImage(video.thumbnail, thumbnail)

            if (video.uploaderAvatar != null) {
                channelImageContainer.isVisible = true
                ImageHelper.loadImage(video.uploaderAvatar, channelImage, true)
                channelImage.setOnClickListener {
                    NavigationHelper.navigateChannel(root.context, video.uploaderUrl)
                }
            } else {
                channelImageContainer.isGone = true
                textViewChannel.setOnClickListener {
                    NavigationHelper.navigateChannel(root.context, video.uploaderUrl)
                }
            }
            root.setOnClickListener {
                if (video.isShort == true || video.url?.contains("/shorts/") == true) {
                    NavigationHelper.navigateShorts(root.context, videoId, video)
                } else {
                    NavigationHelper.navigateVideo(root.context, videoId)
                }
            }

            root.setOnLongClickListener {
                fragmentManager.setFragmentResultListener(
                    VideoOptionsBottomSheet.VIDEO_OPTIONS_SHEET_REQUEST_KEY,
                    activity
                ) { _, _ ->
                    notifyItemChanged(position)
                }
                val sheet = VideoOptionsBottomSheet()
                sheet.arguments = bundleOf(IntentData.streamItem to video)
                sheet.show(fragmentManager, VideoCardsAdapter::class.java.name)
                true
            }

            menuButton.setOnClickListener {
                fragmentManager.setFragmentResultListener(
                    VideoOptionsBottomSheet.VIDEO_OPTIONS_SHEET_REQUEST_KEY,
                    activity
                ) { _, _ ->
                    notifyItemChanged(position)
                }
                val sheet = VideoOptionsBottomSheet()
                sheet.arguments = bundleOf(IntentData.streamItem to video)
                sheet.show(fragmentManager, VideoCardsAdapter::class.java.name)
            }

            CoroutineScope(Dispatchers.IO).launch {
                DeArrowUtil.deArrowVideoId(videoId)?.let { (title, thumbnail) ->
                    withContext(Dispatchers.Main) {
                        if (title != null) this@apply.textViewTitle.text = title
                        if (thumbnail != null) ImageHelper.loadImage(thumbnail, this@apply.thumbnail)
                    }
                }
            }
        }
    }

    companion object {
        private const val NORMAL_TYPE = 0
        private const val CAUGHT_UP_TYPE = 1
        private const val SHORTS_TYPE = 2
        private const val HEADER_TYPE = 3

        const val CAUGHT_UP_STREAM_TYPE = "caught"
    }
}
