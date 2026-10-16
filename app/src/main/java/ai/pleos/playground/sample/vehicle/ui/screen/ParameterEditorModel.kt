package ai.pleos.playground.sample.vehicle.ui.screen

import ai.pleos.playground.sample.vehicle.explorer.ApiNumericRange
import ai.pleos.playground.sample.vehicle.explorer.ApiParameterDescriptor
import ai.pleos.playground.sample.vehicle.explorer.ApiParameterKind
import java.math.BigDecimal
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

internal enum class ParameterEditorKind { AREA, SWITCH, CHOICES, NUMBER, RANGE }

internal data class ParameterEditorChoice(val value: String, val label: String)

internal fun selectedEditorRange(
    parameters: List<ApiParameterDescriptor>,
    values: Map<String, String>,
    ranges: Map<String, ApiNumericRange>
): ApiNumericRange? {
    val area = parameters.firstOrNull { it.kind == ApiParameterKind.AREA }
    val key = if (area == null) "GLOBAL" else values[area.name] ?: return null
    return ranges[key]
}

internal fun parameterEditorKind(
    parameter: ApiParameterDescriptor,
    range: ApiNumericRange?,
    featureId: String?
): ParameterEditorKind = when (parameter.kind) {
    ApiParameterKind.AREA -> ParameterEditorKind.AREA
    ApiParameterKind.BOOLEAN -> ParameterEditorKind.SWITCH
    ApiParameterKind.ENUM -> ParameterEditorKind.CHOICES
    ApiParameterKind.INTEGER, ApiParameterKind.FLOAT -> when {
        parameterEditorChoices(parameter, range, featureId).isNotEmpty() -> ParameterEditorKind.CHOICES
        range != null && range.minimum < range.maximum &&
            (parameter.kind != ApiParameterKind.INTEGER || ceil(range.minimum) < floor(range.maximum)) -> ParameterEditorKind.RANGE
        else -> ParameterEditorKind.NUMBER
    }
}

internal fun parameterEditorChoices(
    parameter: ApiParameterDescriptor,
    range: ApiNumericRange?,
    featureId: String?
): List<ParameterEditorChoice> = when {
    parameter.kind == ApiParameterKind.INTEGER && featureId == "DOOR:DoorPosition" &&
        range?.minimum == 0.0 && range.maximum == 1.0 -> listOf(
        ParameterEditorChoice("0", "Closed"), ParameterEditorChoice("1", "Open")
    )
    parameter.kind == ApiParameterKind.ENUM -> parameter.options.map {
        ParameterEditorChoice(it, parameterOptionLabel(it))
    }
    else -> emptyList()
}

internal fun parameterOptionLabel(value: String): String = value
    .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
    .split('_', ' ')
    .joinToString(" ") { word -> word.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() } }

internal fun isParameterInputValid(
    parameter: ApiParameterDescriptor,
    value: String,
    range: ApiNumericRange?
): Boolean {
    val number = when (parameter.kind) {
        ApiParameterKind.INTEGER -> value.toIntOrNull()?.toDouble() ?: return false
        ApiParameterKind.FLOAT -> {
            val parsed = value.toFloatOrNull()?.takeIf { it.isFinite() } ?: return false
            val exact = value.toBigDecimalOrNull() ?: return false
            if (parsed == 0f && exact.signum() != 0) return false
            parsed.toDouble()
        }
        else -> return true
    }
    if (range == null) return true
    return if (parameter.kind == ApiParameterKind.FLOAT) {
        number in range.minimum.toFloat().toDouble()..range.maximum.toFloat().toDouble()
    } else {
        number in range.minimum..range.maximum
    }
}

internal fun steppedNumericValue(
    parameter: ApiParameterDescriptor,
    value: String,
    range: ApiNumericRange?,
    direction: Int
): String? {
    if (!isParameterInputValid(parameter, value, range)) return null
    val next = BigDecimal.valueOf(value.toDouble()).add(BigDecimal.valueOf(direction.toLong())).toDouble()
    val bounded = if (range == null) next else next.coerceIn(range.minimum, range.maximum)
    val result = if (parameter.kind == ApiParameterKind.INTEGER) {
        val integer = if (direction > 0) floor(bounded) else ceil(bounded)
        numberText(integer)
    } else {
        numberText(bounded)
    }
    return result.takeIf { isParameterInputValid(parameter, it, range) }
}

internal fun sliderNumericValue(
    parameter: ApiParameterDescriptor,
    fraction: Float,
    range: ApiNumericRange
): String {
    val number = range.minimum * (1.0 - fraction.toDouble()) + range.maximum * fraction.toDouble()
    return if (parameter.kind == ApiParameterKind.INTEGER) {
        numberText(round(number).coerceIn(ceil(range.minimum), floor(range.maximum)))
    } else {
        // Match the API's Float precision rather than displaying interpolation noise.
        number.toFloat().toString()
    }
}

internal fun numberText(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
