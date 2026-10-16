package ai.pleos.playground.sample.vehicle.explorer

data class ApiMethodDescriptor(
    val id: String,
    val name: String,
    val displayName: String,
    val operation: ApiOperation,
    val parameters: List<ApiParameterDescriptor>,
    val executionMode: ApiExecutionMode
)

data class ApiParameterDescriptor(
    val name: String,
    val kind: ApiParameterKind,
    val defaultValue: String,
    val options: List<String> = emptyList()
)

enum class ApiParameterKind {
    AREA,
    BOOLEAN,
    ENUM,
    FLOAT,
    INTEGER
}

enum class ApiOperation {
    CAPABILITY,
    GET,
    SET,
    REGISTER,
    UNREGISTER,
    OTHER
}

enum class ApiExecutionMode {
    TYPED_REFERENCE,
    REFLECTION
}

enum class ApiMethodStatus {
    UNKNOWN,
    RUNNING,
    SUPPORTED,
    RESULT,
    UNSUPPORTED,
    FAILED
}

internal fun apiExecutionMode(
    domain: VehicleDomain,
    methodName: String
): ApiExecutionMode = if (
    domain == VehicleDomain.DOOR && methodName in TYPED_DOOR_METHODS
) {
    ApiExecutionMode.TYPED_REFERENCE
} else {
    ApiExecutionMode.REFLECTION
}

private val TYPED_DOOR_METHODS = setOf(
    "checkDoorPositionCapabilities",
    "getCurrentDoorPosition",
    "setDoorPosition",
    "registerDoorPosition",
    "unregisterDoorPosition"
)
