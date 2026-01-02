package com.github.ptube.ui.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.github.ptube.databinding.VideoTagRowBinding
import com.github.ptube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.ptube.ui.viewholders.VideoTagsViewHolder

class VideoTagsAdapter : ListAdapter<String, VideoTagsViewHolder>(DiffUtilItemCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoTagsViewHolder {
        val binding = VideoTagRowBinding.inflate(LayoutInflater.from(parent.context))
        return VideoTagsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoTagsViewHolder, position: Int) {
        val tag = getItem(holder.bindingAdapterPosition)
        holder.binding.apply {
            tagText.text = tag
            root.setOnClickListener {
                val mainActivity = root.context as MainActivity
                mainActivity.setQuery(tag, true)
                // minimizes the player fragment to the mini player
                mainActivity.onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}
