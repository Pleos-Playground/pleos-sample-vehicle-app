package ai.pleos.playground.sample.vehicle.ui.screen

import ai.pleos.playground.sample.vehicle.explorer.ApiFeatureDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiMethodDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiMethodStatus
import ai.pleos.playground.sample.vehicle.explorer.ApiOperation
import ai.pleos.playground.sample.vehicle.ui.data.MainUiState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun ApiActivityPanel(
    feature: ApiFeatureDescriptor,
    uiState: MainUiState,
    expanded: Boolean,
    compact: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val methods = feature.methods.sortedWith(
        compareBy<ApiMethodDescriptor> { it.id != uiState.focusedMethodId }
            .thenByDescending { uiState.apiMethodResponses[it.id].orEmpty() }
            .thenBy { apiActivityOrder(it.operation) }
    )
    val recentMethods = methods.filter {
        uiState.apiMethodStatuses[it.id] != null || uiState.apiMethodResponses[it.id] != null
    }.take(if (compact) 1 else 2)
    val historyState = rememberLazyListState()
    LaunchedEffect(feature.id, expanded, uiState.apiFocusSequence) {
        if (expanded && methods.any { it.id == uiState.focusedMethodId }) {
            historyState.scrollToItem(0)
        }
    }
    Surface(
        modifier = modifier.testTag("api-activity-dock"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "API activity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onToggleExpanded) {
                    Text(if (expanded) "Collapse" else "Expand")
                }
            }
            if (expanded) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("api-activity-history"),
                    state = historyState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(methods, key = { it.id }) { method ->
                        ApiActivityRow(method, uiState, expanded = true, compact = compact)
                    }
                }
            } else if (recentMethods.isEmpty()) {
                Text(
                    "Run an action to see its API result.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    recentMethods.forEach { method ->
                        ApiActivityRow(method, uiState, expanded = false, compact = compact)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiActivityRow(
    method: ApiMethodDescriptor,
    uiState: MainUiState,
    expanded: Boolean,
    compact: Boolean
) {
    val focused = uiState.focusedMethodId == method.id
    val status = uiState.apiMethodStatuses[method.id] ?: ApiMethodStatus.UNKNOWN
    val color by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(160),
        label = "api-activity-focus"
    )
    val response = uiState.apiMethodResponses[method.id]
    Surface(shape = RoundedCornerShape(8.dp), color = color) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    method.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!expanded && !compact && response != null) {
                    Text(
                        response,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    status.activityLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = status.activityColor()
                )
            }
            if ((expanded || compact) && response != null) {
                Text(
                    response,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun ApiMethodStatus.activityLabel(): String = when (this) {
    ApiMethodStatus.UNKNOWN -> "Not called"
    ApiMethodStatus.RUNNING -> "Calling"
    ApiMethodStatus.SUPPORTED -> "Supported"
    ApiMethodStatus.RESULT -> "Complete"
    ApiMethodStatus.UNSUPPORTED -> "Unsupported"
    ApiMethodStatus.FAILED -> "Failed"
}

@Composable
private fun ApiMethodStatus.activityColor() = when (this) {
    ApiMethodStatus.UNSUPPORTED, ApiMethodStatus.FAILED -> MaterialTheme.colorScheme.error
    ApiMethodStatus.SUPPORTED -> MaterialTheme.colorScheme.tertiary
    ApiMethodStatus.RUNNING -> MaterialTheme.colorScheme.primary
    ApiMethodStatus.RESULT -> MaterialTheme.colorScheme.secondary
    ApiMethodStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

internal fun apiActivityOrder(operation: ApiOperation): Int = when (operation) {
    ApiOperation.CAPABILITY -> 0
    ApiOperation.GET, ApiOperation.OTHER -> 1
    ApiOperation.SET -> 2
    ApiOperation.REGISTER -> 3
    ApiOperation.UNREGISTER -> 4
}
