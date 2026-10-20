package com.vibesync.promptstudio.data.diagnostics

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Optional in-app review request. Google Play decides whether to show a dialog.
 */
class PlayExperienceManager(private val activity: Activity, private val diagnostics: DiagnosticsStore) {
    fun onSuccessfulPromptRun(allowReview: Boolean = true) {
        val prefs = activity.getSharedPreferences("vibesync_play_experience", Context.MODE_PRIVATE)
        val completedRuns = prefs.getInt(KEY_SUCCESSFUL_RUNS, 0) + 1
        prefs.edit().putInt(KEY_SUCCESSFUL_RUNS, completedRuns).apply()
        if (!allowReview || completedRuns < MIN_RUNS_FOR_REVIEW) return

        val versionCode = runCatching {
            val info = if (android.os.Build.VERSION.SDK_INT >= 28) {
                activity.packageManager.getPackageInfo(activity.packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                activity.packageManager.getPackageInfo(activity.packageName, 0).versionCode.toLong()
            }
            info
        }.getOrDefault(-1L)

        if (prefs.getLong(KEY_REVIEW_VERSION, -1L) == versionCode) return
        prefs.edit().putLong(KEY_REVIEW_VERSION, versionCode).apply()

        val manager = runCatching { ReviewManagerFactory.create(activity) }.getOrElse {
            diagnostics.record(DiagnosticSeverity.DEBUG, "play", "review", "unavailable", "In-app review is not available on this installation.")
            return
        }
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (!request.isSuccessful || request.result == null) {
                diagnostics.record(DiagnosticSeverity.DEBUG, "play", "review", "request_unavailable", "Play did not provide an in-app review flow.")
                return@addOnCompleteListener
            }
            manager.launchReviewFlow(activity, request.result).addOnCompleteListener {
                diagnostics.record(DiagnosticSeverity.INFO, "play", "review", "flow_completed", "The Play review flow completed; display is controlled by Google Play.")
            }
        }
    }

    companion object {
        private const val MIN_RUNS_FOR_REVIEW = 6
        private const val KEY_SUCCESSFUL_RUNS = "successful_runs"
        private const val KEY_REVIEW_VERSION = "review_requested_version"
    }
}
