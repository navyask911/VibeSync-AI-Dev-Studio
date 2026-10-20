package com.vibesync.promptstudio.data.diagnostics

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Best-effort Play Store features. Sideloaded builds and unavailable Play services fail closed.
 * These APIs do not prove that the review dialog was shown or that an update was installed.
 */
class PlayExperienceManager(private val activity: Activity, private val diagnostics: DiagnosticsStore) {
    private val updateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private var checkedThisSession = false
    private var listenerRegistered = false
    private val updateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            diagnostics.record(DiagnosticSeverity.INFO, "play", "update", "downloaded", "A flexible Play update was downloaded.")
        }
    }

    fun onResume() {
        if (!listenerRegistered) {
            updateManager.registerListener(updateListener)
            listenerRegistered = true
        }
        updateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    updateManager.completeUpdate()
                    return@addOnSuccessListener
                }
                if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    runCatching {
                        updateManager.startUpdateFlowForResult(info, AppUpdateType.FLEXIBLE, activity, UPDATE_REQUEST_CODE)
                    }.onFailure {
                        diagnostics.record(DiagnosticSeverity.WARNING, "play", "update", "resume_failed", "Could not resume the Play update flow.")
                    }
                    return@addOnSuccessListener
                }
                if (!checkedThisSession && info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    checkedThisSession = true
                    runCatching {
                        updateManager.startUpdateFlowForResult(info, AppUpdateType.FLEXIBLE, activity, UPDATE_REQUEST_CODE)
                    }.onFailure {
                        diagnostics.record(DiagnosticSeverity.WARNING, "play", "update", "start_failed", "Play update flow is unavailable on this installation.")
                    }
                }
            }
            .addOnFailureListener {
                diagnostics.record(DiagnosticSeverity.DEBUG, "play", "update", "check_unavailable", "Play update checks are unavailable; this can be normal for sideloaded builds.")
            }
    }

    fun onStop() {
        if (listenerRegistered) {
            runCatching { updateManager.unregisterListener(updateListener) }
            listenerRegistered = false
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == UPDATE_REQUEST_CODE && resultCode != Activity.RESULT_OK) {
            diagnostics.record(DiagnosticSeverity.INFO, "play", "update", "user_did_not_complete", "The Play update flow was cancelled or did not complete.")
        }
    }

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
        private const val UPDATE_REQUEST_CODE = 7821
        private const val MIN_RUNS_FOR_REVIEW = 6
        private const val KEY_SUCCESSFUL_RUNS = "successful_runs"
        private const val KEY_REVIEW_VERSION = "review_requested_version"
    }
}
