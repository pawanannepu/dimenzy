package com.dimenzy.app.ui.ar

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

enum class ArCoreStatus { Ready, NeedsInstall, Unsupported, Failed }

enum class InstallResult { Installed, Requested, Declined, Failed }

/** Wraps `ArCoreApk` (spec 0001 and 0003): the availability check and the Play Services for AR install prompt. */
class ArCoreCheck(private val context: Context) {

  /** Polls every 200 ms while ARCore is still deciding, and gives up after 10 s. */
  suspend fun availability(): ArCoreStatus =
    withTimeoutOrNull(TIMEOUT_MS) {
      var status = statusOf(checkOnce())
      while (status == null) {
        delay(POLL_MS)
        status = statusOf(checkOnce())
      }
      status
    } ?: ArCoreStatus.Failed

  /** Must run on the main thread. [userRequested] is `false` only on the resume after [InstallResult.Requested]. */
  fun requestInstall(activity: Activity, userRequested: Boolean): InstallResult =
    try {
      when (ArCoreApk.getInstance().requestInstall(activity, userRequested)) {
        ArCoreApk.InstallStatus.INSTALLED -> InstallResult.Installed
        ArCoreApk.InstallStatus.INSTALL_REQUESTED -> InstallResult.Requested
        null -> InstallResult.Failed
      }
    } catch (e: UnavailableUserDeclinedInstallationException) {
      InstallResult.Declined
    } catch (e: UnavailableException) {
      Log.w(TAG, "ARCore install unavailable", e)
      InstallResult.Failed
    } catch (e: RuntimeException) {
      Log.w(TAG, "ARCore install failed", e)
      InstallResult.Failed
    }

  private fun checkOnce(): ArCoreApk.Availability? =
    try {
      ArCoreApk.getInstance().checkAvailability(context)
    } catch (e: RuntimeException) {
      Log.w(TAG, "ARCore check failed", e)
      ArCoreApk.Availability.UNKNOWN_ERROR
    }

  /** `null` means ARCore is still checking. */
  private fun statusOf(availability: ArCoreApk.Availability?): ArCoreStatus? = when (availability) {
    ArCoreApk.Availability.SUPPORTED_INSTALLED -> ArCoreStatus.Ready
    ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
    ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> ArCoreStatus.NeedsInstall
    ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> ArCoreStatus.Unsupported
    ArCoreApk.Availability.UNKNOWN_CHECKING -> null
    ArCoreApk.Availability.UNKNOWN_ERROR,
    ArCoreApk.Availability.UNKNOWN_TIMED_OUT,
    null -> ArCoreStatus.Failed
  }

  private companion object {
    const val TAG = "Dimenzy"
    const val POLL_MS = 200L
    const val TIMEOUT_MS = 10_000L
  }
}
