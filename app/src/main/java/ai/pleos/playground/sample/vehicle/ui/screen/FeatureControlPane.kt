package ai.pleos.playground.sample.vehicle.ui.screen

import ai.pleos.playground.sample.vehicle.explorer.ApiFeatureDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiMethodStatus
import ai.pleos.playground.sample.vehicle.explorer.ApiNumericRange
import ai.pleos.playground.sample.vehicle.explorer.ApiParameterKind
import ai.pleos.playground.sample.vehicle.ui.data.MainUiState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun FeatureControlPane(
    feature: ApiFeatureDescriptor,
    uiState: MainUiState,
    capabilityStatus: ApiMethodStatus,
    busy: Boolean,
    readValues: Map<String, String>,
    writeValues: Map<String, String>,
    onReadValueChanged: (String, String) -> Unit,
    onWriteValueChanged: (String, String) -> Unit,
    onRunCapability: () -> Unit,
    onRunRead: () -> Unit,
    onRunSet: () -> Unit,
    onStartUpdates: () -> Unit,
    onStopUpdates: () -> Unit,
    scrollable: Boolean,
    modifier: Modifier = Modifier
) {
    val contentModifier = if (scrollable) {
        modifier.verticalScroll(rememberScrollState())
    } else {
        modifier
    }
    val ranges = uiState.featureValueRanges[feature.id].orEmpty()
    val writeRange = selectedEditorRange(feature.write?.parameters.orEmpty(), writeValues, ranges)
    val supportRange = if (feature.write != null) writeRange else {
        selectedEditorRange(feature.read?.parameters.orEmpty(), readValues, ranges)
    }
    Column(
        modifier = contentModifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SupportSection(
            feature = feature,
            status = capabilityStatus,
            range = supportRange,
            busy = busy,
            onRetry = onRunCapability
        )
        CurrentValueSection(
            feature = feature,
            currentValue = currentValue(feature, uiState),
            currentValueDetail = uiState.featureCurrentDetails[feature.id],
            readValues = readValues,
            enabled = !busy,
            onValueChanged = onReadValueChanged,
            onRefresh = onRunRead
        )
        feature.write?.let { method ->
            ControlSection(title = "Change value") {
                method.parameters.filterNot { parameter ->
                    parameter.kind == ApiParameterKind.AREA && feature.read?.parameters?.any {
                        it.kind == ApiParameterKind.AREA && it.name == parameter.name
                    } == true
                }.forEach { parameter ->
                    ApiParameterEditor(
                        parameter = parameter,
                        value = writeValues[parameter.name].orEmpty(),
                        enabled = !busy,
                        range = writeRange,
                        featureId = feature.id,
                        onValueChanged = { onWriteValueChanged(parameter.name, it) }
                    )
                }
                Button(
                    onClick = onRunSet,
                    enabled = !busy && method.parameters.all {
                        isParameterInputValid(it, writeValues[it.name].orEmpty(), writeRange)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply change")
                }
                Text(
                    text = "Apply sends only the change request. Use Refresh when you want to " +
                        "read the current value.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (feature.hasLiveUpdates) {
            LiveUpdatesSection(
                feature = feature,
                registered = uiState.subscribedFeatureId == feature.id,
                enabled = !busy,
                onStart = onStartUpdates,
                onStop = onStopUpdates
            )
        }
    }
}

@Composable
private fun SupportSection(
    feature: ApiFeatureDescriptor,
    status: ApiMethodStatus,
    range: ApiNumericRange?,
    busy: Boolean,
    onRetry: () -> Unit
) {
    ControlSection(title = "Vehicle support") {
        Column {
            DashboardStatusPill(status, feature.capability != null)
            Text(
                text = when {
                    feature.capability == null -> "No support check is declared for this feature."
                    status == ApiMethodStatus.UNSUPPORTED ->
                        "The connected vehicle does not support this feature."
                    status == ApiMethodStatus.FAILED -> "The support check failed."
                    range != null -> "Selected area range: ${numberText(range.minimum)}..${numberText(range.maximum)}"
                    status == ApiMethodStatus.SUPPORTED || status == ApiMethodStatus.RESULT ->
                        "Support checked. No numeric range was reported for the selected area."
                    else -> "Support is checked automatically when this feature opens."
                },
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onRetry,
                enabled = !busy && feature.capability != null,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 10.dp)
            ) {
                Text("Check support")
            }
        }
    }
}

@Composable
private fun CurrentValueSection(
    feature: ApiFeatureDescriptor,
    currentValue: String,
    currentValueDetail: String?,
    readValues: Map<String, String>,
    enabled: Boolean,
    onValueChanged: (String, String) -> Unit,
    onRefresh: () -> Unit
) {
    ControlSection(title = "Current value") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentValue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                currentValueDetail?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(
                onClick = onRefresh,
                enabled = enabled && feature.read != null
            ) {
                Text("Refresh")
            }
        }
        feature.read?.parameters?.forEach { parameter ->
            ApiParameterEditor(
                parameter = parameter,
                value = readValues[parameter.name].orEmpty(),
                enabled = enabled,
                onValueChanged = { onValueChanged(parameter.name, it) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveUpdatesSection(
    feature: ApiFeatureDescriptor,
    registered: Boolean,
    enabled: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    ControlSection(title = "Live updates") {
        Text(
            text = if (registered) "Receiving vehicle updates" else "Updates are stopped",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Run either action at any time to test the corresponding API.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onStart,
                enabled = enabled && feature.register != null
            ) {
                Text("Start updates")
            }
            OutlinedButton(
                onClick = onStop,
                enabled = enabled && feature.unregister != null
            ) {
                Text("Stop updates")
            }
        }
    }
}
