package com.vibesync.promptstudio.presentation.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.vibesync.promptstudio.MainActivity
import com.vibesync.promptstudio.VibeSyncApplication
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
            val message = result.exceptionOrNull()?.message
                ?: "OAuth identity token stored separately. Add a provider API key to run models."
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
