package com.github.ptube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.ptube.R
import com.github.ptube.api.obj.StreamItem
import com.github.ptube.databinding.ShortsRowBinding
import com.github.ptube.extensions.toID
import com.github.ptube.helpers.ImageHelper
import com.github.ptube.helpers.NavigationHelper
import com.github.ptube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.ptube.util.TextUtils

class ShortsAdapter(private val isGridLayout: Boolean = false) : ListAdapter<StreamItem, ShortsAdapter.ShortsViewHolder>(DiffUtilItemCallback()) {

    private var onItemClickListener: ((StreamItem) -> Unit)? = null

    fun setOnItemClickListener(listener: (StreamItem) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortsViewHolder {
        val layoutId = if (isGridLayout) R.layout.shorts_grid_item else R.layout.shorts_row
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        val binding = ShortsRowBinding.bind(view) // Both layouts share ID structure, so binding works
        return ShortsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShortsViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ShortsViewHolder(private val binding: ShortsRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StreamItem) {
            binding.title.text = item.title
            binding.views.text = TextUtils.formatViewsString(binding.root.context, item.views ?: 0, 0)
            
            ImageHelper.loadImage(item.thumbnail, binding.thumbnail)

            binding.root.setOnClickListener {
                onItemClickListener?.invoke(item) ?: run {
                    NavigationHelper.navigateShorts(
                        binding.root.context,
                        item.url.orEmpty().toID(),
                        item
                    )
                }
            }
        }
    }
}
