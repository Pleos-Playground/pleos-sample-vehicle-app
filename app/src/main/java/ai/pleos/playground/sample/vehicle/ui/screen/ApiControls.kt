package ai.pleos.playground.sample.vehicle.ui.screen

import ai.pleos.playground.sample.vehicle.explorer.ApiExecutionMode
import ai.pleos.playground.sample.vehicle.explorer.ApiNumericRange
import ai.pleos.playground.sample.vehicle.explorer.ApiParameterDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiParameterKind
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun ApiExecutionModeBadge(executionMode: ApiExecutionMode) {
    if (executionMode != ApiExecutionMode.TYPED_REFERENCE) return
    Surface(
        shape = RoundedCornerShape(100),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Text(
            text = "Reference",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ApiParameterEditor(
    parameter: ApiParameterDescriptor,
    value: String,
    enabled: Boolean = true,
    range: ApiNumericRange? = null,
    featureId: String? = null,
    onValueChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = parameterOptionLabel(parameter.name),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        when (parameterEditorKind(parameter, range, featureId)) {
            ParameterEditorKind.AREA -> AreaParameterEditor(parameter, value, enabled, onValueChanged)
            ParameterEditorKind.SWITCH -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(if (value == "true") "ON" else "OFF", style = MaterialTheme.typography.bodyLarge)
                    Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = value == "true",
                    onCheckedChange = { onValueChanged(it.toString()) },
                    enabled = enabled,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = parameter.name }
                )
            }
            ParameterEditorKind.CHOICES -> FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                parameterEditorChoices(parameter, range, featureId).forEach { option ->
                    FilterChip(
                        selected = option.value == value,
                        onClick = { onValueChanged(option.value) },
                        enabled = enabled,
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = {
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(option.label, style = MaterialTheme.typography.labelLarge)
                                if (option.label != option.value) {
                                    Text(option.value, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    )
                }
            }
            ParameterEditorKind.NUMBER, ParameterEditorKind.RANGE -> NumericParameterEditor(
                parameter, value, enabled, range, onValueChanged
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AreaParameterEditor(
    parameter: ApiParameterDescriptor,
    value: String,
    enabled: Boolean,
    onValueChanged: (String) -> Unit
) {
    var expanded by remember(parameter) { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = "${parameterOptionLabel(value)} · $value",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && enabled) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            parameter.options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(parameterOptionLabel(option), style = MaterialTheme.typography.bodyLarge)
                            Text(option, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = { onValueChanged(option); expanded = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun NumericParameterEditor(
    parameter: ApiParameterDescriptor,
    value: String,
    enabled: Boolean,
    range: ApiNumericRange?,
    onValueChanged: (String) -> Unit
) {
    val valid = isParameterInputValid(parameter, value, range)
    val decrease = steppedNumericValue(parameter, value, range, -1)
    val increase = steppedNumericValue(parameter, value, range, 1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { decrease?.let(onValueChanged) },
            enabled = enabled && decrease != null && decrease != value,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "Decrease ${parameter.name} by 1" }
        ) { Text("-") }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            enabled = enabled,
            isError = !valid,
            modifier = Modifier.weight(1f).testTag("${parameter.name}-number"),
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (parameter.kind == ApiParameterKind.FLOAT) KeyboardType.Decimal else KeyboardType.Number
            )
        )
        OutlinedButton(
            onClick = { increase?.let(onValueChanged) },
            enabled = enabled && increase != null && increase != value,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "Increase ${parameter.name} by 1" }
        ) { Text("+") }
    }
    if (!valid) {
        Text(
            text = if (range != null) {
                "Enter ${if (parameter.kind == ApiParameterKind.INTEGER) "an integer" else "a number"} from ${numberText(range.minimum)} to ${numberText(range.maximum)}."
            } else if (parameter.kind == ApiParameterKind.INTEGER) {
                "Enter a valid integer."
            } else {
                "Enter a finite number."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    if (range != null) {
        if (parameterEditorKind(parameter, range, null) == ParameterEditorKind.RANGE) {
            val fraction = value.toDoubleOrNull()?.takeIf { it.isFinite() }?.let {
                ((it - range.minimum) / (range.maximum - range.minimum)).coerceIn(0.0, 1.0).toFloat()
            } ?: 0f
            Slider(
                value = fraction,
                onValueChange = { onValueChanged(sliderNumericValue(parameter, it, range)) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("${parameter.name}-slider")
                    .semantics { contentDescription = parameter.name; stateDescription = value }
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(numberText(range.minimum), style = MaterialTheme.typography.bodySmall)
            Text(numberText(range.maximum), style = MaterialTheme.typography.bodySmall)
        }
    } else {
        Text("Range not reported", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
