package com.github.ptube.ui.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.ptube.R
import kotlin.system.exitProcess

class CrashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)

        val error = intent.getStringExtra("error") ?: "Unknown error"
        val errorView = findViewById<TextView>(R.id.errorTextView)
        errorView.text = error

        findViewById<Button>(R.id.copyButton).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Crash Log", error)
            clipboard.setPrimaryClip(clip)
        }

        findViewById<Button>(R.id.closeButton).setOnClickListener {
            finishAffinity()
            exitProcess(0)
        }
    }
}
