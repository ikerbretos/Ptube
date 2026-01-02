package com.github.ptube.ui.preferences

import android.os.Bundle
import com.github.ptube.R
import com.github.ptube.ui.base.BasePreferenceFragment

class AudioVideoSettings : BasePreferenceFragment() {
    override val titleResourceId: Int = R.string.audio_video

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.audio_video_settings, rootKey)
    }
}
