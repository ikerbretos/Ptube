package com.github.ptube.ui.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.github.ptube.databinding.AllCaughtUpRowBinding
import com.github.ptube.databinding.TrendingRowBinding

class VideoCardsViewHolder : RecyclerView.ViewHolder {
    var trendingRowBinding: TrendingRowBinding? = null
    var allCaughtUpBinding: AllCaughtUpRowBinding? = null
    var itemHomeHeaderBinding: com.github.ptube.databinding.ItemHomeHeaderBinding? = null

    constructor(binding: TrendingRowBinding) : super(binding.root) {
        trendingRowBinding = binding
    }

    constructor(binding: AllCaughtUpRowBinding) : super(binding.root) {
        allCaughtUpBinding = binding
    }

    constructor(itemView: android.view.View) : super(itemView) {
        // Generic constructor for manual binding (e.g. Shorts)
    }

    constructor(binding: com.github.ptube.databinding.ItemHomeHeaderBinding) : super(binding.root) {
        itemHomeHeaderBinding = binding
    }
}
