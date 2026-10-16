package ai.pleos.playground.sample.vehicle.explorer

data class FeatureDisplayValue(
    val value: String,
    val detail: String? = null
)

fun formatFeatureDisplay(
    featureTitle: String,
    message: String,
    requestValues: Map<String, String>
): FeatureDisplayValue {
    val sanitizedMessage = sanitizeJvmObjectIdentity(message)
    if (featureTitle != "Door Lock") {
        return FeatureDisplayValue(value = sanitizedMessage)
    }

    val locked = requestValues["value"].toBooleanOrNull()
        ?: trailingBoolean(message)
        ?: return FeatureDisplayValue(value = sanitizedMessage)
    val area = requestValues["area"]
    return FeatureDisplayValue(
        value = if (locked) "Locked" else "Unlocked",
        detail = area?.let { "Area: $it" }
    )
}

fun formatApiResponse(
    featureTitle: String,
    methodName: String,
    message: String,
    requestValues: Map<String, String>
): String {
    val display = formatFeatureDisplay(
        featureTitle = featureTitle,
        message = message,
        requestValues = requestValues
    )
    if (featureTitle == "Door Lock" && display.value in setOf("Locked", "Unlocked")) {
        return buildString {
            append(methodName)
            append(" · ")
            append(display.value)
            display.detail?.let {
                append(" · ")
                append(it)
            }
        }
    }
    return sanitizeJvmObjectIdentity(message)
}

fun sanitizeJvmObjectIdentity(message: String): String = JVM_OBJECT_IDENTITY.replace(message) {
    it.value.substringBefore('@').substringAfterLast('.')
}

private fun String?.toBooleanOrNull(): Boolean? = when (this?.lowercase()) {
    "true" -> true
    "false" -> false
    else -> null
}

private fun trailingBoolean(message: String): Boolean? = BOOLEAN_RESULT.find(message)
    ?.groupValues
    ?.getOrNull(1)
    ?.toBooleanOrNull()

private val BOOLEAN_RESULT = Regex("""(?:,|\s)(true|false)\s*$""")
private val JVM_OBJECT_IDENTITY = Regex(
    """([A-Za-z_$][A-Za-z0-9_$]*(?:\.[A-Za-z_$][A-Za-z0-9_$]*)*)@[0-9a-fA-F]+"""
)
