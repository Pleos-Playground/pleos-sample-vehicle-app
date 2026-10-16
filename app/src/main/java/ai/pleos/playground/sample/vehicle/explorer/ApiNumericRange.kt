package ai.pleos.playground.sample.vehicle.explorer

import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.MirrorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WheelArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig

data class ApiNumericRange(val minimum: Double, val maximum: Double)

internal fun capabilityNumericRanges(values: List<Any?>): Map<String, ApiNumericRange> {
    val config = values.filterIsInstance<CapabilityConfig>().firstOrNull() ?: return emptyMap()
    if (config.accessType == AccessType.NONE) return emptyMap()

    return buildMap {
        values.forEach { value ->
            when (value) {
                is AreaBounds<*> -> value.numericRange()?.let { put("GLOBAL", it) }
                is Map<*, *> -> value.forEach areaEntry@ { (area, bounds) ->
                    val range = (bounds as? AreaBounds<*>)?.numericRange() ?: return@areaEntry
                    val codes = when (area) {
                        is DoorArea -> area.values.map { it.name }
                        is MirrorArea -> area.values.map { it.name }
                        is SeatArea -> area.values.map { it.name }
                        is WheelArea -> area.values.map { it.name }
                        is WindowArea -> area.values.map { it.name }
                        else -> emptyList()
                    }
                    codes.forEach { put(it, range) }
                }
            }
        }
    }
}

private fun AreaBounds<*>.numericRange(): ApiNumericRange? {
    val minimum = (minValue as? Number)?.toDouble() ?: return null
    val maximum = (maxValue as? Number)?.toDouble() ?: return null
    return if (minimum.isFinite() && maximum.isFinite() && minimum <= maximum) {
        ApiNumericRange(minimum, maximum)
    } else {
        null
    }
}
