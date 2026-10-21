package com.vibesync.promptstudio.presentation.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.vibesync.promptstudio.MainActivity
import com.vibesync.promptstudio.R
import com.vibesync.promptstudio.VibeSyncApplication
import com.vibesync.promptstudio.data.diagnostics.DiagnosticSeverity
import kotlinx.coroutines.launch

class AuthCallbackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callback = intent?.data
        if (callback == null) {
            finish()
            return
        }
        val manager = (application as VibeSyncApplication).container.oauthManager
        lifecycleScope.launch {
            val result = runCatching { manager.handleCallback(callback) }
            val message = if (result.isSuccess) {
                getString(R.string.oauth_callback_success_message)
            } else {
                getString(R.string.oauth_callback_failure_message)
            }
            (application as VibeSyncApplication).diagnostics.record(
                if (result.isSuccess) DiagnosticSeverity.INFO else DiagnosticSeverity.WARNING,
                "auth",
                "oauth",
                if (result.isSuccess) "callback_succeeded" else "callback_failed",
                message
            )
            Toast.makeText(this@AuthCallbackActivity, message, Toast.LENGTH_LONG).show()
            startActivity(
                Intent(this@AuthCallbackActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            setResult(if (result.isSuccess) Activity.RESULT_OK else Activity.RESULT_CANCELED)
            finish()
        }
    }
}
