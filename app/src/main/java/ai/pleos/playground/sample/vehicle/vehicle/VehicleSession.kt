package ai.pleos.playground.sample.vehicle.vehicle

import ai.pleos.playground.vehicle.Vehicle

interface VehicleSession {
    val vehicle: Vehicle

    fun requireInitialized()
}
