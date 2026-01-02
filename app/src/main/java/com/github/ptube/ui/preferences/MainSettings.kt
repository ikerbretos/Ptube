package com.github.ptube.ui.preferences

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.github.ptube.BuildConfig
import com.github.ptube.R
import com.github.ptube.helpers.PreferenceHelper
import com.github.ptube.ui.base.BasePreferenceFragment
import com.github.ptube.ui.dialogs.ErrorDialog
import com.github.ptube.util.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainSettings : BasePreferenceFragment() {
    override val titleResourceId: Int = R.string.settings

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val update = findPreference<Preference>("update")
        update?.summary = "v${BuildConfig.VERSION_NAME}"

        // check app update manually
        update?.setOnPreferenceClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                UpdateChecker(requireContext()).checkUpdate(true)
            }

            true
        }

        val crashlog = findPreference<Preference>("crashlog")
        crashlog?.isVisible = PreferenceHelper.getErrorLog().isNotEmpty() && BuildConfig.DEBUG
        crashlog?.setOnPreferenceClickListener {
            ErrorDialog().show(childFragmentManager, null)
            crashlog.isVisible = false
            true
        }
    }
}
