package ai.pleos.playground.sample.vehicle.ui.screen

import ai.pleos.playground.sample.vehicle.explorer.ApiFeatureDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiMethodStatus
import ai.pleos.playground.sample.vehicle.ui.data.MainUiState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun DashboardStatusPill(status: ApiMethodStatus, hasCapability: Boolean) {
    val (label, color) = when {
        !hasCapability -> "Support unknown" to MaterialTheme.colorScheme.onSurfaceVariant
        status == ApiMethodStatus.SUPPORTED -> "Supported" to MaterialTheme.colorScheme.tertiary
        status == ApiMethodStatus.UNSUPPORTED -> "Unsupported" to MaterialTheme.colorScheme.error
        status == ApiMethodStatus.RUNNING -> "Checking" to MaterialTheme.colorScheme.primary
        status == ApiMethodStatus.FAILED -> "Check failed" to MaterialTheme.colorScheme.error
        else -> "Checking on entry" to MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = RoundedCornerShape(100),
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Text(text = label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
internal fun FeatureErrorCard(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Request failed", fontWeight = FontWeight.Bold)
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun ControlSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
internal fun LatestEvent(event: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "Latest vehicle event\n$event",
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

internal fun currentValue(feature: ApiFeatureDescriptor, uiState: MainUiState): String = when {
    feature.title == "Door Position" && uiState.currentPosition != null ->
        uiState.currentPosition.toString()
    uiState.featureCurrentValues[feature.id] != null ->
        uiState.featureCurrentValues.getValue(feature.id)
    feature.read == null -> "No read API"
    else -> "Waiting for first read"
}
