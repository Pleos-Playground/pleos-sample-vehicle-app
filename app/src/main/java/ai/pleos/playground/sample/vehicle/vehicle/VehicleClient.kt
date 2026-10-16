package ai.pleos.playground.sample.vehicle.vehicle

import ai.pleos.playground.sample.vehicle.explorer.ApiNumericRange
import ai.pleos.playground.sample.vehicle.explorer.capabilityNumericRanges
import ai.pleos.playground.sample.vehicle.vehicle.VehicleClient.DoorOperation
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.Door
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import android.content.Context

class VehicleClient(
    context: Context,
    private val callback: Callback
) : VehicleSession {

    enum class DoorOperation {
        CAPABILITY,
        READ,
        SET,
        REGISTER,
        UNREGISTER
    }

    interface Callback {
        fun onCapabilities(accessType: String, ranges: Map<String, ApiNumericRange>)
        fun onDoorPositionRead(position: Int?)
        fun onDoorPositionSet(position: Int?)
        fun onDoorPositionChanged(position: Int?)
        fun onError(exception: Exception)
        fun onDoorOperationError(operation: DoorOperation, exception: Exception) {
            onError(exception)
        }
    }

    override val vehicle = Vehicle(context.applicationContext)
    private val doorRequests = DoorPositionRequests(vehicle.getDoor(), callback, ::requireInitialized)
    private var isInitialized = false

    fun initialize(): Boolean {
        if (isInitialized) return true

        return try {
            vehicle.initialize()
            isInitialized = true
            true
        } catch (exception: Exception) {
            callback.onError(exception)
            false
        }
    }

    fun inspectDoorPositionCapabilities() = doorRequests.inspectDoorPositionCapabilities()

    fun readDoorPosition(area: String) = doorRequests.readDoorPosition(area)

    fun setDoorPosition(area: String, position: Int) = doorRequests.setDoorPosition(area, position)

    fun startDoorPositionUpdates(area: String): Boolean = doorRequests.startDoorPositionUpdates(area)

    fun stopDoorPositionUpdates(area: String? = null): Boolean = doorRequests.stopDoorPositionUpdates(area)

    fun release() {
        if (!isInitialized) return

        stopDoorPositionUpdates()
        runSdkCall {
            vehicle.release()
            isInitialized = false
        }
    }

    override fun requireInitialized() {
        check(isInitialized) { "VehicleClient must be initialized before use." }
    }

    private inline fun runSdkCall(action: () -> Unit) {
        try {
            action()
        } catch (exception: Exception) {
            callback.onError(exception)
        }
    }
}

internal class DoorPositionRequests(
    private val door: Door,
    private val callback: VehicleClient.Callback,
    private val requireInitialized: () -> Unit = {}
) {
    private var subscribedArea: DoorArea? = null
    private var capabilityRequest: Any? = null
    private val listener = object : DoorPositionListener {
        override fun onPositionUpdated(pos: Int?) = callback.onDoorPositionChanged(pos)
        override fun onFailed(e: Exception) = callback.onError(e)
    }

    fun inspectDoorPositionCapabilities(): Boolean {
        val request = Any().also { capabilityRequest = it }
        return runCall(DoorOperation.CAPABILITY) {
            door.checkDoorPositionCapabilities(
                onComplete = { config, areaBounds ->
                    if (capabilityRequest === request) {
                        callback.onCapabilities(config.accessType.name, capabilityNumericRanges(listOf(config, areaBounds)))
                    }
                },
                onFailed = {
                    if (capabilityRequest === request) callback.onDoorOperationError(DoorOperation.CAPABILITY, it)
                }
            )
        }
    }

    fun readDoorPosition(area: String) = runCall(DoorOperation.READ) {
        door.getCurrentDoorPosition(
            area = doorArea(area),
            onComplete = callback::onDoorPositionRead,
            onFailed = { callback.onDoorOperationError(DoorOperation.READ, it) }
        )
    }

    fun setDoorPosition(area: String, position: Int) = runCall(DoorOperation.SET) {
        door.setDoorPosition(
            area = doorArea(area),
            value = position,
            onComplete = { _, value -> callback.onDoorPositionSet(value) },
            onFailed = { callback.onDoorOperationError(DoorOperation.SET, it) }
        )
    }

    fun startDoorPositionUpdates(area: String): Boolean {
        return runCall(DoorOperation.REGISTER) {
            val selectedArea = doorArea(area)
            if (subscribedArea != selectedArea) {
                if (!stopDoorPositionUpdates()) return false
                door.registerDoorPosition(selectedArea, listener)
                subscribedArea = selectedArea
            }
        }
    }

    fun stopDoorPositionUpdates(area: String? = null): Boolean {
        val registeredArea = subscribedArea ?: return true
        return runCall(DoorOperation.UNREGISTER) {
            check(area == null || doorArea(area) == registeredArea) {
                "Select the registered Door area before unregistering updates."
            }
            door.unregisterDoorPosition(registeredArea, listener)
            subscribedArea = null
        }
    }

    private fun doorArea(code: String): DoorArea = DoorArea(DoorValue.valueOf(code))

    private inline fun runCall(operation: DoorOperation, action: () -> Unit): Boolean = try {
        requireInitialized()
        action()
        true
    } catch (exception: Exception) {
        callback.onDoorOperationError(operation, exception)
        false
    }
}
