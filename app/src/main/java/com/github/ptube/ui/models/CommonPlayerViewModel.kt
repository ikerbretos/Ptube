package com.github.ptube.ui.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.ptube.extensions.updateIfChanged

class CommonPlayerViewModel : ViewModel() {
    val isMiniPlayerVisible = MutableLiveData(false)
    val isFullscreen = MutableLiveData(false)
    var maxSheetHeightPx = 0

    val sheetExpand = MutableLiveData<Boolean?>()

    fun setSheetExpand(state: Boolean?) {
        sheetExpand.updateIfChanged(state)
    }
}
