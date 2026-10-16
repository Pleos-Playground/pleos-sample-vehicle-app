package ai.pleos.playground.sample.vehicle.vehicle

import ai.pleos.playground.sample.vehicle.BuildConfig
import ai.pleos.playground.sample.vehicle.ui.data.RuntimeVersions
import android.content.Context
import android.content.pm.PackageManager

class RuntimeVersionProvider(context: Context) {

    private val packageManager = context.packageManager

    fun read(): RuntimeVersions = RuntimeVersions(
        sdkVersion = BuildConfig.VEHICLE_SDK_VERSION,
        vehicleServiceVersion = readPackageVersion(VEHICLE_SERVICE_PACKAGE)
    )

    private fun readPackageVersion(packageName: String): String {
        return runCatching {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: "not installed"
    }

    companion object {
        const val VEHICLE_SERVICE_PACKAGE = "ai.pleos.playground.service.vehicle"
    }
}
