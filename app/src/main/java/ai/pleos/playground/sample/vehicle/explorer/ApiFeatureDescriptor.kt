package ai.pleos.playground.sample.vehicle.explorer

data class ApiFeatureDescriptor(
    val id: String,
    val domain: VehicleDomain,
    val title: String,
    val description: String,
    val methods: List<ApiMethodDescriptor>,
    val capability: ApiMethodDescriptor?,
    val read: ApiMethodDescriptor?,
    val write: ApiMethodDescriptor?,
    val register: ApiMethodDescriptor?,
    val unregister: ApiMethodDescriptor?
) {
    val isTypedReference: Boolean
        get() = methods.any { it.executionMode == ApiExecutionMode.TYPED_REFERENCE }

    val executionMode: ApiExecutionMode
        get() = if (isTypedReference) {
            ApiExecutionMode.TYPED_REFERENCE
        } else {
            ApiExecutionMode.REFLECTION
        }

    val hasLiveUpdates: Boolean
        get() = register != null || unregister != null
}

fun apiFeatures(domain: VehicleDomain, methods: List<ApiMethodDescriptor>): List<ApiFeatureDescriptor> =
    methods
        .groupBy { it.featureKey() }
        .map { (key, groupedMethods) ->
            ApiFeatureDescriptor(
                id = "${domain.name}:$key",
                domain = domain,
                title = key.humanizeFeatureKey(),
                description = featureDescription(key),
                methods = groupedMethods.sortedBy { it.operation.ordinal },
                capability = groupedMethods.firstOrNull { it.operation == ApiOperation.CAPABILITY },
                read = groupedMethods.firstOrNull { it.operation == ApiOperation.GET }
                    ?: groupedMethods.firstOrNull { it.operation == ApiOperation.OTHER },
                write = groupedMethods.firstOrNull { it.operation == ApiOperation.SET },
                register = groupedMethods.firstOrNull { it.operation == ApiOperation.REGISTER },
                unregister = groupedMethods.firstOrNull { it.operation == ApiOperation.UNREGISTER }
            )
        }
        .sortedWith(
            compareByDescending<ApiFeatureDescriptor> { it.isTypedReference }
                .thenBy { it.title }
        )

private fun ApiMethodDescriptor.featureKey(): String = name
    .removePrefix("check")
    .removePrefix("getCurrent")
    .removePrefix("get")
    .removePrefix("set")
    .removePrefix("register")
    .removePrefix("unregister")
    .removePrefix("is")
    .removePrefix("move")
    .removeSuffix("Capabilities")
    .removeSuffix("Capability")
    .removeSuffix("State")
    .ifBlank { name }

private fun String.humanizeFeatureKey(): String = replace(
    Regex("([a-z0-9])([A-Z])"),
    "$1 $2"
).replaceFirstChar { it.uppercase() }

private fun featureDescription(key: String): String = when {
    key.contains("temperature", ignoreCase = true) -> "Current temperature and target value"
    key.contains("window", ignoreCase = true) -> "Window position and control"
    key.contains("light", ignoreCase = true) -> "Lighting state and control"
    key.contains("door", ignoreCase = true) -> "Door state, capability, and updates"
    else -> "Capability, current value, and control flow"
}
