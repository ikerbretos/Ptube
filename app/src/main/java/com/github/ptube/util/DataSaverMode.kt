package com.github.ptube.util

import android.content.Context
import com.github.ptube.constants.PreferenceKeys
import com.github.ptube.helpers.NetworkHelper
import com.github.ptube.helpers.PreferenceHelper

object DataSaverMode {
    fun isEnabled(context: Context): Boolean {
        val pref = PreferenceHelper.getString(PreferenceKeys.DATA_SAVER_MODE, "disabled")
        return when (pref) {
            "enabled" -> true
            "disabled" -> false
            "metered" -> NetworkHelper.isNetworkMetered(context)
            else -> throw IllegalArgumentException()
        }
    }
}
