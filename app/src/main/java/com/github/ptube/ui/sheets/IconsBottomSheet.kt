package com.github.ptube.ui.sheets

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.github.ptube.R
import com.github.ptube.databinding.BottomSheetBinding
import com.github.ptube.ui.adapters.IconsSheetAdapter

class IconsBottomSheet : ExpandedBottomSheet(R.layout.bottom_sheet) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = BottomSheetBinding.bind(view)
        binding.optionsRecycler.layoutManager = GridLayoutManager(context, 3)
        binding.optionsRecycler.adapter = IconsSheetAdapter()
    }
}
