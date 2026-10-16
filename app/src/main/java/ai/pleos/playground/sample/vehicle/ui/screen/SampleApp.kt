package ai.pleos.playground.sample.vehicle.ui.screen

import ai.pleos.playground.sample.vehicle.explorer.VehicleDomain
import ai.pleos.playground.sample.vehicle.ui.data.AppPage
import ai.pleos.playground.sample.vehicle.ui.data.MainUiState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SampleApp(
    uiState: MainUiState,
    onOpenDomain: (VehicleDomain) -> Unit,
    onSelectFeature: (String) -> Unit,
    onSearchFeatures: (String) -> Unit,
    onRunCapability: () -> Unit,
    onRunRead: (Map<String, String>) -> Unit,
    onRunSet: (Map<String, String>) -> Unit,
    onStartUpdates: (Map<String, String>) -> Unit,
    onStopUpdates: (Map<String, String>) -> Unit,
    onBackToFeatureList: () -> Unit,
    onDismissError: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        snackbarHostState.currentSnackbarData?.dismiss()
        val message = uiState.errorMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = "Dismiss",
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
            onDismissError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.page) {
            AppPage.HOME,
            AppPage.API_EXPLORER -> ApiCatalogScreen(
                uiState = uiState,
                onSelectDomain = onOpenDomain,
                onSelectFeature = onSelectFeature,
                onSearchFeatures = onSearchFeatures,
                onRunCapability = onRunCapability,
                onRunRead = onRunRead,
                onRunSet = onRunSet,
                onStartUpdates = onStartUpdates,
                onStopUpdates = onStopUpdates,
                onBackToFeatureList = onBackToFeatureList,
                onDismissFeatureError = onDismissError
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(24.dp)
        )
    }
}
