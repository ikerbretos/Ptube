package com.github.ptube.ui.preferences

import android.os.Bundle
import com.github.ptube.R
import com.github.ptube.ui.base.BasePreferenceFragment

class SponsorBlockSettings : BasePreferenceFragment() {
    override val titleResourceId: Int = R.string.sponsorblock

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sponsorblock_settings, rootKey)
    }
}
