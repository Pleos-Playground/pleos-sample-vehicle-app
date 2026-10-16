package ai.pleos.playground.sample.vehicle.ui.data

import ai.pleos.playground.sample.vehicle.explorer.ApiMethodDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiMethodStatus
import ai.pleos.playground.sample.vehicle.explorer.ApiFeatureDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiNumericRange
import ai.pleos.playground.sample.vehicle.explorer.ApiOperation
import ai.pleos.playground.sample.vehicle.explorer.VehicleDomain

data class MainUiState(
    val page: AppPage = AppPage.HOME,
    val runtimeVersions: RuntimeVersions = RuntimeVersions(),
    val selectedDomain: VehicleDomain? = null,
    val apiExplorerPane: ApiExplorerPane = ApiExplorerPane.FEATURE_LIST,
    val apiMethods: List<ApiMethodDescriptor> = emptyList(),
    val apiFeatures: List<ApiFeatureDescriptor> = emptyList(),
    val domainFeatureCounts: Map<VehicleDomain, Int> = emptyMap(),
    val featureSearchQuery: String = "",
    val selectedFeatureId: String? = null,
    val focusedMethodId: String? = null,
    val apiFocusSequence: Long = 0,
    val apiMethodStatuses: Map<String, ApiMethodStatus> = emptyMap(),
    val apiMethodResponses: Map<String, String> = emptyMap(),
    val featureCurrentValues: Map<String, String> = emptyMap(),
    val featureCurrentDetails: Map<String, String> = emptyMap(),
    val featureValueRanges: Map<String, Map<String, ApiNumericRange>> = emptyMap(),
    val explorerEvents: List<String> = emptyList(),
    val isInitialized: Boolean = false,
    val isSubscribed: Boolean = false,
    val subscribedFeatureId: String? = null,
    val capabilityAccess: String? = null,
    val currentPosition: Int? = null,
    val errorMessage: String? = null,
    val featureErrorMessage: String? = null
)

internal fun MainUiState.withCapabilityRanges(
    featureId: String,
    ranges: Map<String, ApiNumericRange>
): MainUiState = copy(
    featureValueRanges = if (ranges.isEmpty()) {
        featureValueRanges - featureId
    } else {
        featureValueRanges + (featureId to ranges)
    }
)

internal fun MainUiState.withFeatureResult(
    featureId: String,
    operation: ApiOperation,
    value: String,
    detail: String?
): MainUiState {
    if (operation !in setOf(ApiOperation.GET, ApiOperation.REGISTER, ApiOperation.OTHER)) return this
    return copy(
        featureCurrentValues = featureCurrentValues + (featureId to value),
        featureCurrentDetails = if (detail == null) {
            featureCurrentDetails - featureId
        } else {
            featureCurrentDetails + (featureId to detail)
        }
    )
}

data class RuntimeVersions(
    val sdkVersion: String = "unknown",
    val vehicleServiceVersion: String = "unknown"
)

enum class AppPage {
    HOME,
    API_EXPLORER
}

enum class ApiExplorerPane {
    FEATURE_LIST,
    FEATURE_DASHBOARD
}
