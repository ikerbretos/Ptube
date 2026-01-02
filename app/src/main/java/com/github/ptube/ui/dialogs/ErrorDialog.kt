package com.github.ptube.ui.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.github.ptube.R
import com.github.ptube.helpers.ClipboardHelper
import com.github.ptube.helpers.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ErrorDialog : DialogFragment() {
    @SuppressLint("PrivateResource")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val errorLog = PreferenceHelper.getErrorLog()
        // reset the error log
        PreferenceHelper.saveErrorLog("")

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.error_occurred)
            .setMessage(errorLog)
            .setNegativeButton(R.string.okay, null)
            .setPositiveButton(androidx.preference.R.string.copy) { _, _ ->
                ClipboardHelper.save(requireContext(), text = errorLog, notify = true)
            }
            .show()
    }
}
