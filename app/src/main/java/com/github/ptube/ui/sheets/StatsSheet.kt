package com.github.ptube.ui.sheets

import android.os.Bundle
import android.view.View
import com.github.ptube.R
import com.github.ptube.constants.IntentData
import com.github.ptube.databinding.DialogStatsBinding
import com.github.ptube.extensions.parcelable
import com.github.ptube.helpers.ClipboardHelper
import com.github.ptube.obj.VideoStats

class StatsSheet : ExpandedBottomSheet(R.layout.dialog_stats) {
    private lateinit var stats: VideoStats

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stats = arguments?.parcelable(IntentData.videoStats)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = DialogStatsBinding.bind(view)
        binding.videoId.setText(stats.videoId)
        binding.videoIdCopy.setEndIconOnClickListener {
            ClipboardHelper.save(requireContext(), "text", stats.videoId)
        }
        binding.videoInfo.setText(stats.videoInfo)
        binding.audioInfo.setText(stats.audioInfo)
        binding.videoQuality.setText(stats.videoQuality)
    }
}
