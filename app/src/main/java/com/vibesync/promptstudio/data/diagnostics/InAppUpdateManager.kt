package com.vibesync.promptstudio.data.diagnostics

import android.app.Activity
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.vibesync.promptstudio.R

/**
 * Best-effort Play Store update workflow. Sideloaded builds or builds not served by Play skip
 * updates; core app features never depend on update availability.
 */
class InAppUpdateManager(
    private val activity: Activity,
    private val diagnostics: DiagnosticsStore
) {
    private val updateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private var checkedThisSession = false
    private var listenerRegistered = false
    private var restartSnackbarShown = false

    private val updateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> showRestartSnackbar()
            InstallStatus.FAILED -> diagnostics.record(
                DiagnosticSeverity.WARNING, "play", "update", "install_failed",
                "Play reported that the update installation failed."
            )
        }
    }

    fun onResume() {
        if (!listenerRegistered) {
            runCatching {
                updateManager.registerListener(updateListener)
                listenerRegistered = true
            }
        }
        runCatching { updateManager.appUpdateInfo }
            .onSuccess { task ->
                task.addOnSuccessListener { info ->
                    if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        showRestartSnackbar()
                        return@addOnSuccessListener
                    }
                    val critical = info.updatePriority() >= CRITICAL_UPDATE_PRIORITY
                    val type = when {
                        info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                            critical && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                        info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                        info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE -> null
                        critical && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                        info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                        else -> null
                    }
                    if (type != null && (!checkedThisSession ||
                            info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)) {
                        checkedThisSession = true
                        try {
                            updateManager.startUpdateFlowForResult(info, type, activity, UPDATE_REQUEST_CODE)
                        } catch (error: Exception) {
                            diagnostics.record(
                                DiagnosticSeverity.WARNING, "play", "update", "start_failed",
                                "Play update flow could not be started: " + error.javaClass.simpleName
                            )
                        }
                    }
                }
                task.addOnFailureListener {
                    diagnostics.record(
                        DiagnosticSeverity.DEBUG, "play", "update", "check_unavailable",
                        "Play update checks are unavailable; this can be normal for some sideloaded installations."
                    )
                }
            }
    }

    fun onStop() {
        if (listenerRegistered) {
            runCatching { updateManager.unregisterListener(updateListener) }
            listenerRegistered = false
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode != UPDATE_REQUEST_CODE) return
        when (resultCode) {
            Activity.RESULT_OK -> diagnostics.record(
                DiagnosticSeverity.INFO, "play", "update", "flow_accepted",
                "The Play update flow returned success."
            )
            Activity.RESULT_CANCELED -> diagnostics.record(
                DiagnosticSeverity.INFO, "play", "update", "flow_cancelled",
                "The Play update flow was cancelled by the user."
            )
            ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> diagnostics.record(
                DiagnosticSeverity.WARNING, "play", "update", "flow_failed",
                "The Play update flow reported a failure."
            )
        }
    }

    private fun showRestartSnackbar() {
        if (restartSnackbarShown || activity.isFinishing || activity.isDestroyed) return
        val root = activity.findViewById<android.view.View>(android.R.id.content) ?: return
        restartSnackbarShown = true
        Snackbar.make(root, activity.getString(R.string.update_downloaded), Snackbar.LENGTH_INDEFINITE)
            .setAction(activity.getString(R.string.restart_now)) {
                updateManager.completeUpdate().addOnFailureListener {
                    restartSnackbarShown = false
                    diagnostics.record(
                        DiagnosticSeverity.WARNING, "play", "update", "complete_failed",
                        "Play could not complete the flexible update."
                    )
                }
            }
            .show()
    }

    companion object {
        private const val UPDATE_REQUEST_CODE = 7821
        private const val CRITICAL_UPDATE_PRIORITY = 4
    }
}
