package ai.pleos.playground.sample.vehicle.ui.screen

import ai.pleos.playground.sample.vehicle.explorer.ApiFeatureDescriptor
import ai.pleos.playground.sample.vehicle.explorer.VehicleDomain
import ai.pleos.playground.sample.vehicle.ui.data.ApiExplorerPane
import ai.pleos.playground.sample.vehicle.ui.data.MainUiState
import ai.pleos.playground.sample.vehicle.ui.data.RuntimeVersions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ApiCatalogScreen(
    uiState: MainUiState,
    onSelectDomain: (VehicleDomain) -> Unit,
    onSelectFeature: (String) -> Unit,
    onSearchFeatures: (String) -> Unit,
    onRunCapability: () -> Unit,
    onRunRead: (Map<String, String>) -> Unit,
    onRunSet: (Map<String, String>) -> Unit,
    onStartUpdates: (Map<String, String>) -> Unit,
    onStopUpdates: (Map<String, String>) -> Unit,
    onBackToFeatureList: () -> Unit,
    onDismissFeatureError: () -> Unit
) {
    val selectedFeature = uiState.apiFeatures.firstOrNull {
        it.id == uiState.selectedFeatureId
    }
    val visibleFeatures = uiState.apiFeatures.filter { feature ->
        val query = uiState.featureSearchQuery.trim()
        query.isBlank() || feature.title.contains(query, ignoreCase = true) ||
            feature.description.contains(query, ignoreCase = true) ||
            feature.methods.any { it.name.contains(query, ignoreCase = true) }
    }
    val content: @Composable (Modifier) -> Unit = { modifier ->
        if (uiState.apiExplorerPane == ApiExplorerPane.FEATURE_DASHBOARD) {
            FeatureDashboardScreen(
                feature = selectedFeature,
                uiState = uiState,
                onRunCapability = onRunCapability,
                onRunRead = onRunRead,
                onRunSet = onRunSet,
                onStartUpdates = onStartUpdates,
                onStopUpdates = onStopUpdates,
                onBackToFeatureList = onBackToFeatureList,
                onDismissError = onDismissFeatureError,
                modifier = modifier
            )
        } else {
            FeatureListPanel(
                selectedDomain = uiState.selectedDomain,
                features = visibleFeatures,
                searchQuery = uiState.featureSearchQuery,
                onSelectFeature = onSelectFeature,
                onSearch = onSearchFeatures,
                modifier = modifier
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Start))
            .padding(start = AaosSystemRailInset),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1480.dp)
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CatalogHeader(
                runtimeVersions = uiState.runtimeVersions,
                isInitialized = uiState.isInitialized
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (catalogLayoutMode(maxWidth)) {
                    CatalogLayoutMode.SIDE_BY_SIDE -> Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        DomainSidebar(
                            selectedDomain = uiState.selectedDomain,
                            featureCounts = uiState.domainFeatureCounts,
                            onSelectDomain = onSelectDomain,
                            modifier = Modifier.width(320.dp)
                        )
                        content(Modifier.weight(1f))
                    }
                    CatalogLayoutMode.MODAL_DRAWER -> CompactCatalogLayout(
                        selectedDomain = uiState.selectedDomain,
                        featureCounts = uiState.domainFeatureCounts,
                        onSelectDomain = onSelectDomain,
                        content = content
                    )
                }
            }
        }
    }
}

internal enum class CatalogLayoutMode {
    SIDE_BY_SIDE,
    MODAL_DRAWER
}

internal fun catalogLayoutMode(maxWidth: Dp): CatalogLayoutMode =
    if (maxWidth >= 760.dp) {
        CatalogLayoutMode.SIDE_BY_SIDE
    } else {
        CatalogLayoutMode.MODAL_DRAWER
    }

@Composable
private fun CompactCatalogLayout(
    selectedDomain: VehicleDomain?,
    featureCounts: Map<VehicleDomain, Int>,
    onSelectDomain: (VehicleDomain) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DomainSidebar(
                    selectedDomain = selectedDomain,
                    featureCounts = featureCounts,
                    onSelectDomain = { domain ->
                        onSelectDomain(domain)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompactDomainMenuButton(
                selectedDomain = selectedDomain,
                onOpen = { scope.launch { drawerState.open() } }
            )
            content(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CompactDomainMenuButton(
    selectedDomain: VehicleDomain?,
    onOpen: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "VEHICLE DOMAIN",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = selectedDomain?.title ?: "Select a domain",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(onClick = onOpen) {
                Text("Browse domains")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogHeader(runtimeVersions: RuntimeVersions, isInitialized: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("catalog-header"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (maxWidth >= 900.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CatalogIdentity(Modifier.weight(1f))
                    VersionChip(label = "SDK", version = runtimeVersions.sdkVersion)
                    VersionChip(label = "Vehicle Service", version = runtimeVersions.vehicleServiceVersion)
                    ReadyPill(isInitialized)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CatalogIdentity()
                        ReadyPill(isInitialized)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VersionChip(label = "SDK", version = runtimeVersions.sdkVersion)
                        VersionChip(label = "Vehicle Service", version = runtimeVersions.vehicleServiceVersion)
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogIdentity(modifier: Modifier = Modifier) {
    Text(
        text = "Vehicle Playground",
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun VersionChip(label: String, version: String) {
    Surface(
        shape = RoundedCornerShape(100),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "$label $version",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReadyPill(isInitialized: Boolean) {
    val color = if (isInitialized) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }
    Surface(
        shape = RoundedCornerShape(100),
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isInitialized) "SDK initialized" else "Initialization failed",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun DomainSidebar(
    selectedDomain: VehicleDomain?,
    featureCounts: Map<VehicleDomain, Int>,
    onSelectDomain: (VehicleDomain) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "BROWSE VEHICLE APIS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Choose a domain. Counts show features.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SidebarSection(
                modifier = Modifier.weight(1f).padding(top = 8.dp, bottom = 8.dp),
                title = "VEHICLE DOMAINS",
                trailing = "${VehicleDomain.entries.size} domains"
            ) {
                DomainList(
                    selectedDomain = selectedDomain,
                    featureCounts = featureCounts,
                    onSelectDomain = onSelectDomain,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FeatureListPanel(
    selectedDomain: VehicleDomain?,
    features: List<ApiFeatureDescriptor>,
    searchQuery: String,
    onSelectFeature: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val featureCountLabel = if (features.size == 1) "1 feature" else "${features.size} features"
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "${selectedDomain?.title ?: "Vehicle"} FEATURES",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Choose a feature to open its capability and control dashboard.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = featureCountLabel,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search features") },
                singleLine = true
            )
            if (features.isEmpty()) {
                Text(
                    text = "No features match your search.",
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(featureGridColumnCount(maxWidth)),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        gridItems(features, key = { it.id }) { feature ->
                            FeatureListItem(
                                feature = feature,
                                selected = false,
                                onClick = { onSelectFeature(feature.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SidebarSection(
    modifier: Modifier,
    title: String,
    trailing: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            content()
        }
    }
}

@Composable
private fun DomainList(
    selectedDomain: VehicleDomain?,
    featureCounts: Map<VehicleDomain, Int>,
    onSelectDomain: (VehicleDomain) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(domainBrowseOrder, key = { it.name }) { domain ->
            Surface(
                onClick = { onSelectDomain(domain) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (domain == selectedDomain) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = domain.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (domain == selectedDomain) FontWeight.Bold else null
                    )
                    Text(
                        text = featureCounts[domain]?.toString() ?: "-",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

internal val domainBrowseOrder: List<VehicleDomain> = buildList {
    add(VehicleDomain.DOOR)
    addAll(VehicleDomain.entries.filterNot { it == VehicleDomain.DOOR })
}

@Composable
private fun FeatureListItem(
    feature: ApiFeatureDescriptor,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        }
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = feature.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                ApiExecutionModeBadge(feature.executionMode)
            }
            Text(
                text = feature.methods.size.toString() + " API requests",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = feature.description,
                modifier = Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal fun featureGridColumnCount(maxWidth: Dp): Int = when {
    maxWidth >= 1000.dp -> 3
    maxWidth >= 680.dp -> 2
    else -> 1
}
