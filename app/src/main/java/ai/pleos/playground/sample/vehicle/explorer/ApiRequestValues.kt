package ai.pleos.playground.sample.vehicle.explorer

fun requestValues(
    method: ApiMethodDescriptor,
    selectedValues: Map<String, String>
): Map<String, String> = method.parameters.associate { parameter ->
    parameter.name to (selectedValues[parameter.name] ?: parameter.defaultValue)
}
