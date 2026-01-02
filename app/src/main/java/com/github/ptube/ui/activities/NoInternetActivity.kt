package com.github.ptube.ui.activities

import android.content.Intent
import android.os.Bundle
import com.github.ptube.constants.IntentData
import com.github.ptube.databinding.ActivityNointernetBinding
import com.github.ptube.helpers.NavigationHelper
import com.github.ptube.ui.base.BaseActivity

class NoInternetActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityNointernetBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.getBooleanExtra(IntentData.maximizePlayer, false)) {
            NavigationHelper.openAudioPlayerFragment(this, offlinePlayer = true)
        }
    }
}
