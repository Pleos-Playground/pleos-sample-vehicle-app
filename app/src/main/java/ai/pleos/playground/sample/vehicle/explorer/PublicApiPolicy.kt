package ai.pleos.playground.sample.vehicle.explorer

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

internal object PublicApiPolicy {

    private val blockedApis = setOf(
        "CarInfo.getCapabilityInfo",
        "Safety.checkTelltalesCapabilities",
        "Safety.getTelltalesInfo",
        "Safety.registerTelltales",
        "Safety.unregisterTelltales",
        "Tire.checkTirePressureCapabilities",
        "Tire.getTirePressure",
        "Tire.registerTirePressure",
        "Tire.unregisterTirePressure"
    )

    fun isSupported(function: KFunction<*>): Boolean {
        val owner = function.parameters
            .firstOrNull { it.kind == KParameter.Kind.INSTANCE }
            ?.type
            ?.classifier as? KClass<*>
        val apiKey = "${owner?.simpleName}.${function.name}"
        return function.annotations.none { it is Deprecated } && apiKey !in blockedApis
    }
}

internal object VehiclePublicApiCatalog {

    val domains: Map<VehicleDomain, KClass<*>> = mapOf(
        VehicleDomain.BRAKE to ai.pleos.playground.vehicle.api.Brake::class,
        VehicleDomain.CAR_INFO to ai.pleos.playground.vehicle.api.CarInfo::class,
        VehicleDomain.DISPLAY to ai.pleos.playground.vehicle.api.Display::class,
        VehicleDomain.DOOR to ai.pleos.playground.vehicle.api.Door::class,
        VehicleDomain.DRIVING_MODE to ai.pleos.playground.vehicle.api.DrivingMode::class,
        VehicleDomain.EV_BATTERY to ai.pleos.playground.vehicle.api.EvBattery::class,
        VehicleDomain.FUEL to ai.pleos.playground.vehicle.api.Fuel::class,
        VehicleDomain.HVAC to ai.pleos.playground.vehicle.api.HVAC::class,
        VehicleDomain.LIGHT to ai.pleos.playground.vehicle.api.Light::class,
        VehicleDomain.ODOMETER to ai.pleos.playground.vehicle.api.Odometer::class,
        VehicleDomain.SAFETY to ai.pleos.playground.vehicle.api.Safety::class,
        VehicleDomain.SEAT to ai.pleos.playground.vehicle.api.Seat::class,
        VehicleDomain.SIDE_MIRROR to ai.pleos.playground.vehicle.api.SideMirror::class,
        VehicleDomain.STEERING_WHEEL to ai.pleos.playground.vehicle.api.SteeringWheel::class,
        VehicleDomain.TIRE to ai.pleos.playground.vehicle.api.Tire::class,
        VehicleDomain.TOLL to ai.pleos.playground.vehicle.api.Toll::class,
        VehicleDomain.TURN_SIGNAL to ai.pleos.playground.vehicle.api.TurnSignal::class,
        VehicleDomain.WINDOW to ai.pleos.playground.vehicle.api.Window::class,
        VehicleDomain.WIPER to ai.pleos.playground.vehicle.api.Wiper::class
    )
}
