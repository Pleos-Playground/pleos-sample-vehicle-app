package ai.pleos.playground.sample.vehicle.ui.screen

import ai.pleos.playground.sample.vehicle.explorer.ApiFeatureDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiMethodStatus
import ai.pleos.playground.sample.vehicle.explorer.requestValues
import ai.pleos.playground.sample.vehicle.ui.data.MainUiState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FeatureDashboardScreen(
    feature: ApiFeatureDescriptor?,
    uiState: MainUiState,
    onRunCapability: () -> Unit,
    onRunRead: (Map<String, String>) -> Unit,
    onRunSet: (Map<String, String>) -> Unit,
    onStartUpdates: (Map<String, String>) -> Unit,
    onStopUpdates: (Map<String, String>) -> Unit,
    onBackToFeatureList: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (feature == null) {
            Text("Select a vehicle feature to open its controls.", Modifier.padding(24.dp))
            return@Card
        }

        val parameters = feature.methods.flatMap { it.parameters }.distinctBy { it.name }
        val values = remember(feature.id, parameters) {
            mutableStateMapOf<String, String>().apply {
                parameters.forEach { put(it.name, it.defaultValue) }
            }
        }
        val capabilityStatus = feature.capability?.let {
            uiState.apiMethodStatuses[it.id]
        } ?: ApiMethodStatus.UNKNOWN
        val busy = feature.methods.any {
            uiState.apiMethodStatuses[it.id] == ApiMethodStatus.RUNNING
        }
        var activityExpanded by remember(feature.id) { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(onClick = onBackToFeatureList) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to features")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${feature.domain.title} / ${feature.title}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        feature.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardStatusPill(capabilityStatus, feature.capability != null)
                        ApiExecutionModeBadge(feature.executionMode)
                    }
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val compactActivity = maxWidth < 720.dp
                val compactHeight = if (compactActivity) 144.dp else 128.dp
                val activityHeight = if (activityExpanded) {
                    minOf(256.dp, maxHeight * 0.45f).coerceAtLeast(compactHeight)
                } else {
                    compactHeight
                }
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        uiState.featureErrorMessage?.let { message ->
                            FeatureErrorCard(message, onDismissError)
                        }
                        FeatureControlPane(
                            feature = feature,
                            uiState = uiState,
                            capabilityStatus = capabilityStatus,
                            busy = busy,
                            readValues = values,
                            writeValues = values,
                            onReadValueChanged = { name, value -> values[name] = value },
                            onWriteValueChanged = { name, value -> values[name] = value },
                            onRunCapability = onRunCapability,
                            onRunRead = { onRunRead(values.toMap()) },
                            onRunSet = { onRunSet(values.toMap()) },
                            onStartUpdates = {
                                feature.register?.let { onStartUpdates(requestValues(it, values)) }
                            },
                            onStopUpdates = {
                                feature.unregister?.let { onStopUpdates(requestValues(it, values)) }
                            },
                            scrollable = false
                        )
                    }
                    ApiActivityPanel(
                        feature = feature,
                        uiState = uiState,
                        expanded = activityExpanded,
                        compact = compactActivity,
                        onToggleExpanded = { activityExpanded = !activityExpanded },
                        modifier = Modifier.fillMaxWidth().height(activityHeight)
                    )
                }
            }
        }
    }
}
