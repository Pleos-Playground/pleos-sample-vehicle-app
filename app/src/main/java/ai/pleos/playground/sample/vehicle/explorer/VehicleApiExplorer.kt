package ai.pleos.playground.sample.vehicle.explorer

import ai.pleos.playground.sample.vehicle.vehicle.VehicleSession
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.MirrorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WheelArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.MirrorValue
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WheelValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import kotlin.Unit
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredMemberFunctions

class VehicleApiExplorer(
    private val session: VehicleSession,
    private val callback: Callback
) {

    interface Callback {
        fun onExplorerEvent(
            message: String,
            isError: Boolean = false,
            methodId: String? = null,
            isResult: Boolean = false,
            numericRanges: Map<String, ApiNumericRange>? = null,
            isValueUpdate: Boolean = false
        )
    }

    private data class DomainTarget(
        val apiClass: KClass<*>,
        val instance: Any
    )

    private data class RegisteredListener(
        val key: String,
        val listener: Any,
        val listenerClass: KClass<*>,
        val parameterValues: Map<String, String>,
        val domain: VehicleDomain,
        val target: Any,
        val unregisterFunction: KFunction<*>?
    )

    private val vehicle: Vehicle = session.vehicle

    private val targets = mapOf(
        VehicleDomain.BRAKE to DomainTarget(
            ai.pleos.playground.vehicle.api.Brake::class,
            vehicle.getBrake()
        ),
        VehicleDomain.CAR_INFO to DomainTarget(
            ai.pleos.playground.vehicle.api.CarInfo::class,
            vehicle.getCarInfo()
        ),
        VehicleDomain.DISPLAY to DomainTarget(
            ai.pleos.playground.vehicle.api.Display::class,
            vehicle.getDisplay()
        ),
        VehicleDomain.DOOR to DomainTarget(
            ai.pleos.playground.vehicle.api.Door::class,
            vehicle.getDoor()
        ),
        VehicleDomain.DRIVING_MODE to DomainTarget(
            ai.pleos.playground.vehicle.api.DrivingMode::class,
            vehicle.getDrivingMode()
        ),
        VehicleDomain.EV_BATTERY to DomainTarget(
            ai.pleos.playground.vehicle.api.EvBattery::class,
            vehicle.getEvBattery()
        ),
        VehicleDomain.FUEL to DomainTarget(
            ai.pleos.playground.vehicle.api.Fuel::class,
            vehicle.getFuel()
        ),
        VehicleDomain.HVAC to DomainTarget(
            ai.pleos.playground.vehicle.api.HVAC::class,
            vehicle.getHVAC()
        ),
        VehicleDomain.LIGHT to DomainTarget(
            ai.pleos.playground.vehicle.api.Light::class,
            vehicle.getLight()
        ),
        VehicleDomain.ODOMETER to DomainTarget(
            ai.pleos.playground.vehicle.api.Odometer::class,
            vehicle.getOdometer()
        ),
        VehicleDomain.SAFETY to DomainTarget(
            ai.pleos.playground.vehicle.api.Safety::class,
            vehicle.getSafety()
        ),
        VehicleDomain.SEAT to DomainTarget(
            ai.pleos.playground.vehicle.api.Seat::class,
            vehicle.getSeat()
        ),
        VehicleDomain.SIDE_MIRROR to DomainTarget(
            ai.pleos.playground.vehicle.api.SideMirror::class,
            vehicle.getSideMirror()
        ),
        VehicleDomain.STEERING_WHEEL to DomainTarget(
            ai.pleos.playground.vehicle.api.SteeringWheel::class,
            vehicle.getSteeringWheel()
        ),
        VehicleDomain.TIRE to DomainTarget(
            ai.pleos.playground.vehicle.api.Tire::class,
            vehicle.getTire()
        ),
        VehicleDomain.TOLL to DomainTarget(
            ai.pleos.playground.vehicle.api.Toll::class,
            vehicle.getToll()
        ),
        VehicleDomain.TURN_SIGNAL to DomainTarget(
            ai.pleos.playground.vehicle.api.TurnSignal::class,
            vehicle.getTurnSignal()
        ),
        VehicleDomain.WINDOW to DomainTarget(
            ai.pleos.playground.vehicle.api.Window::class,
            vehicle.getWindow()
        ),
        VehicleDomain.WIPER to DomainTarget(
            ai.pleos.playground.vehicle.api.Wiper::class,
            vehicle.getWiper()
        )
    )

    private val functionsById = mutableMapOf<String, KFunction<*>>()
    private val capabilityRequests = mutableMapOf<String, Any>()
    private val registeredListeners = mutableMapOf<String, RegisteredListener>()
    private val pendingRegistrations = mutableMapOf<String, RegisteredListener>()
    private val pendingUnregistrations = mutableMapOf<String, String>()

    fun methods(domain: VehicleDomain): List<ApiMethodDescriptor> {
        val target = targets.getValue(domain)
        return target.apiClass.declaredMemberFunctions
            .filter(PublicApiPolicy::isSupported)
            .map { function -> function.toDescriptor(domain) }
            .sortedWith(
                compareBy<ApiMethodDescriptor> { it.operation.ordinal }
                    .thenBy { it.displayName }
            )
    }

    fun invoke(
        domain: VehicleDomain,
        methodId: String,
        parameterValues: Map<String, String>
    ) {
        session.requireInitialized()
        val target = targets.getValue(domain)
        val function = functionsById[methodId]
        if (function == null) {
            callback.onExplorerEvent("Method metadata is unavailable: $methodId", true)
            return
        }

        val capabilityRequest = if (function.name.toOperation() == ApiOperation.CAPABILITY) {
            Any().also { capabilityRequests[methodId] = it }
        } else {
            null
        }
        callback.onExplorerEvent(message = "${function.name} invoked", methodId = methodId)
        try {
            val arguments = mutableMapOf<KParameter, Any?>()
            function.parameters.forEach { parameter ->
                when (parameter.kind) {
                    KParameter.Kind.INSTANCE -> arguments[parameter] = target.instance
                    else -> createArgument(
                        domain = domain,
                        function = function,
                        parameter = parameter,
                        values = parameterValues,
                        capabilityRequest = capabilityRequest
                    )?.let { arguments[parameter] = it }
                }
            }
            val result = function.callBy(arguments)
            val isBooleanResult = function.returnType.classifier == Boolean::class
            if (isBooleanResult && result == false) {
                pendingRegistrations.remove(methodId)
                pendingUnregistrations.remove(methodId)
            } else {
                commitListenerState(methodId, function.name)
            }
            if (isBooleanResult || function.name.toOperation() in setOf(ApiOperation.REGISTER, ApiOperation.UNREGISTER)) {
                callback.onExplorerEvent(
                    message = if (isBooleanResult) {
                        "${function.name} invoked: ${result.formatValue()}"
                    } else {
                        "${function.name} invoked"
                    },
                    isError = isBooleanResult && result == false,
                    methodId = methodId,
                    isResult = true
                )
            }
        } catch (exception: Exception) {
            pendingRegistrations.remove(methodId)
            pendingUnregistrations.remove(methodId)
            val cause = (exception as? InvocationTargetException)?.targetException ?: exception
            callback.onExplorerEvent(
                message = "${function.name} failed: ${cause.message ?: cause.javaClass.simpleName}",
                isError = true,
                methodId = methodId,
                isResult = true
            )
        }
    }

    fun release() {
        capabilityRequests.clear()
        pendingRegistrations.clear()
        pendingUnregistrations.clear()
        registeredListeners.values.forEach { registration ->
            val unregisterFunction = registration.unregisterFunction ?: return@forEach
            runCatching {
                val arguments = mutableMapOf<KParameter, Any?>()
                unregisterFunction.parameters.forEach { parameter ->
                    when {
                        parameter.kind == KParameter.Kind.INSTANCE -> {
                            arguments[parameter] = registration.target
                        }
                        parameter.type.classifier == registration.listenerClass -> {
                            arguments[parameter] = registration.listener
                        }
                        else -> createArgument(
                            domain = registration.domain,
                            function = unregisterFunction,
                            parameter = parameter,
                            values = registration.parameterValues
                        )?.let { arguments[parameter] = it }
                    }
                }
                unregisterFunction.callBy(arguments)
            }
        }
        registeredListeners.clear()
    }

    private fun KFunction<*>.toDescriptor(domain: VehicleDomain): ApiMethodDescriptor {
        val id = buildString {
            append(domain.name)
            append(':')
            append(name)
            parameters
                .filter { it.kind == KParameter.Kind.VALUE }
                .forEach {
                    append(':')
                    append(it.type)
                }
        }
        functionsById[id] = this
        return ApiMethodDescriptor(
            id = id,
            name = name,
            displayName = name.humanize(),
            operation = name.toOperation(),
            parameters = parameters.mapNotNull(::toInputDescriptor),
            executionMode = apiExecutionMode(domain, name)
        )
    }

    private fun toInputDescriptor(parameter: KParameter): ApiParameterDescriptor? {
        if (parameter.kind != KParameter.Kind.VALUE) return null
        val classifier = parameter.type.classifier as? KClass<*> ?: return null
        val name = parameter.name ?: return null

        if (classifier.isCallback() || classifier.isListener()) return null

        val enumOptions = classifier.java.enumConstants
            ?.map { (it as Enum<*>).name }
            .orEmpty()
        return when {
            classifier in AREA_CLASSES -> {
                val options = areaOptions(classifier)
                ApiParameterDescriptor(
                    name = name,
                    kind = ApiParameterKind.AREA,
                    defaultValue = preferredOption(options),
                    options = options
                )
            }
            classifier == Boolean::class -> ApiParameterDescriptor(
                name,
                ApiParameterKind.BOOLEAN,
                "false",
                listOf("false", "true")
            )
            classifier == Int::class -> ApiParameterDescriptor(
                name,
                ApiParameterKind.INTEGER,
                "0"
            )
            classifier == Float::class -> ApiParameterDescriptor(
                name,
                ApiParameterKind.FLOAT,
                "0.0"
            )
            enumOptions.isNotEmpty() -> ApiParameterDescriptor(
                name,
                ApiParameterKind.ENUM,
                preferredOption(enumOptions),
                enumOptions
            )
            else -> null
        }
    }

    private fun createArgument(
        domain: VehicleDomain,
        function: KFunction<*>,
        parameter: KParameter,
        values: Map<String, String>,
        capabilityRequest: Any? = null
    ): Any? {
        val classifier = parameter.type.classifier as? KClass<*> ?: return null
        val parameterName = parameter.name.orEmpty()
        val value = values[parameterName]

        return when {
            classifier.isCallback() -> callbackArgument(
                methodId = function.methodId(domain),
                methodName = function.name,
                parameterName = parameterName,
                classifier = classifier,
                capabilityRequest = capabilityRequest
            )
            classifier.isListener() -> listenerArgument(
                domain,
                function,
                classifier,
                values
            )
            classifier in AREA_CLASSES -> createArea(classifier, value)
            classifier == Boolean::class -> value?.toBooleanStrictOrNull() ?: false
            classifier == Int::class -> value?.toIntOrNull() ?: 0
            classifier == Float::class -> value?.toFloatOrNull() ?: 0f
            classifier.java.isEnum -> classifier.java.enumConstants
                ?.firstOrNull { (it as Enum<*>).name == value }
            else -> null
        }
    }

    private fun callbackArgument(
        methodId: String,
        methodName: String,
        parameterName: String,
        classifier: KClass<*>,
        capabilityRequest: Any?
    ): Any {
        val isFailure = parameterName.contains("fail", ignoreCase = true)
        fun report(values: List<Any?>) {
            if (capabilityRequest != null && capabilityRequests[methodId] !== capabilityRequest) return
            val throwable = values.firstOrNull() as? Throwable
            val isCapability = methodName.toOperation() == ApiOperation.CAPABILITY
            val unsupported = isCapability && values.filterIsInstance<CapabilityConfig>()
                .any { it.accessType == AccessType.NONE }
            val isError = isFailure || throwable != null || unsupported
            val message = if (throwable != null) {
                throwable.message ?: throwable.javaClass.simpleName
            } else {
                values.joinToString { it.formatValue() }
            }
            callback.onExplorerEvent(
                message = "$methodName · $parameterName: $message",
                isError = isError,
                methodId = methodId,
                isResult = true,
                numericRanges = if (isCapability) {
                    if (isError) emptyMap() else capabilityNumericRanges(values)
                } else {
                    null
                },
                isValueUpdate = methodName.toOperation() in setOf(ApiOperation.GET, ApiOperation.OTHER)
            )
        }

        return when (classifier) {
            Function0::class -> {
                val function: () -> Unit = { report(emptyList()) }
                function
            }
            Function1::class -> {
                val function: (Any?) -> Unit = { first -> report(listOf(first)) }
                function
            }
            Function2::class -> {
                val function: (Any?, Any?) -> Unit = { first, second ->
                    report(listOf(first, second))
                }
                function
            }
            Function3::class -> {
                val function: (Any?, Any?, Any?) -> Unit = { first, second, third ->
                    report(listOf(first, second, third))
                }
                function
            }
            else -> error("Unsupported callback type: $classifier")
        }
    }

    private fun listenerArgument(
        domain: VehicleDomain,
        function: KFunction<*>,
        listenerClass: KClass<*>,
        values: Map<String, String>
    ): Any {
        val listenerKey = listenerKey(domain, function, listenerClass)
        if (function.name.startsWith("unregister")) {
            val registration = registeredListeners[listenerKey]
                ?: error("Register this listener before unregistering it.")
            pendingUnregistrations[function.methodId(domain)] = listenerKey
            return registration.listener
        }
        check(listenerKey !in registeredListeners &&
            pendingRegistrations.values.none { it.key == listenerKey }
        ) { "Listener is already registered for this property." }

        val listener = Proxy.newProxyInstance(
            listenerClass.java.classLoader,
            arrayOf(listenerClass.java)
        ) { proxy, method, arguments ->
            when (method.name) {
                "equals" -> proxy === arguments?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "VehicleApiListener(${listenerClass.simpleName})"
                else -> {
                    val valuesText = arguments.orEmpty()
                        .joinToString { it.formatValue() }
                    callback.onExplorerEvent(
                        message = "${listenerClass.simpleName}.${method.name}: $valuesText",
                        isError = method.name.contains("fail", ignoreCase = true),
                        methodId = function.methodId(domain),
                        isResult = true,
                        isValueUpdate = true
                    )
                    Unit
                }
            }
        }
        val target = targets.getValue(domain)
        val unregisterName = "unregister${function.name.removePrefix("register")}"
        val unregisterFunction = target.apiClass.declaredMemberFunctions.firstOrNull {
            it.name == unregisterName
        }
        val registration = RegisteredListener(
            key = listenerKey,
            listener = listener,
            listenerClass = listenerClass,
            parameterValues = values,
            domain = domain,
            target = target.instance,
            unregisterFunction = unregisterFunction
        )
        pendingRegistrations[function.methodId(domain)] = registration
        return listener
    }

    private fun createArea(areaClass: KClass<*>, selected: String?): Any =
        when (areaClass) {
            DoorArea::class -> DoorArea(enumValue<DoorValue>(selected))
            MirrorArea::class -> MirrorArea(enumValue<MirrorValue>(selected))
            SeatArea::class -> SeatArea(enumValue<SeatValue>(selected))
            WheelArea::class -> WheelArea(enumValue<WheelValue>(selected))
            WindowArea::class -> WindowArea(enumValue<WindowValue>(selected))
            else -> error("Unsupported area: ${areaClass.simpleName}")
        }

    private fun areaOptions(areaClass: KClass<*>): List<String> =
        when (areaClass) {
            DoorArea::class -> DoorValue.entries.map { it.name }
            MirrorArea::class -> MirrorValue.entries.map { it.name }
            SeatArea::class -> SeatValue.entries.map { it.name }
            WheelArea::class -> WheelValue.entries.map { it.name }
            WindowArea::class -> WindowValue.entries.map { it.name }
            else -> emptyList()
        }

    private inline fun <reified T : Enum<T>> enumValue(name: String?): T {
        val values = enumValues<T>()
        return values.firstOrNull { it.name == name }
            ?: values.firstOrNull { it.name in PREFERRED_OPTIONS }
            ?: values.first()
    }

    private fun KClass<*>.isCallback(): Boolean =
        this in setOf(Function0::class, Function1::class, Function2::class, Function3::class)

    private fun KClass<*>.isListener(): Boolean =
        qualifiedName?.startsWith("ai.pleos.playground.vehicle.listener.") == true

    private fun String.toOperation(): ApiOperation = when {
        startsWith("check") -> ApiOperation.CAPABILITY
        startsWith("get") || startsWith("is") -> ApiOperation.GET
        startsWith("set") || startsWith("move") -> ApiOperation.SET
        startsWith("register") -> ApiOperation.REGISTER
        startsWith("unregister") -> ApiOperation.UNREGISTER
        else -> ApiOperation.OTHER
    }

    private fun String.listenerPropertyKey(): String =
        removePrefix("register").removePrefix("unregister")

    private fun listenerKey(
        domain: VehicleDomain,
        function: KFunction<*>,
        listenerClass: KClass<*>
    ): String = "${domain.name}:${function.name.listenerPropertyKey()}:${listenerClass.qualifiedName}"

    private fun commitListenerState(methodId: String, methodName: String) {
        when {
            methodName.startsWith("register") -> {
                pendingRegistrations.remove(methodId)?.let { registration ->
                    registeredListeners[registration.key] = registration
                }
            }
            methodName.startsWith("unregister") -> {
                pendingUnregistrations.remove(methodId)?.let { key ->
                    registeredListeners.remove(key)
                }
            }
        }
    }

    private fun KFunction<*>.methodId(domain: VehicleDomain): String = buildString {
        append(domain.name)
        append(':')
        append(name)
        parameters
            .filter { it.kind == KParameter.Kind.VALUE }
            .forEach {
                append(':')
                append(it.type)
            }
    }

    private fun String.humanize(): String =
        replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .replaceFirstChar { it.uppercase() }

    private fun Any?.formatValue(): String = when (this) {
        null -> "null"
        is Array<*> -> contentDeepToString()
        is BooleanArray -> contentToString()
        is FloatArray -> contentToString()
        is IntArray -> contentToString()
        is LongArray -> contentToString()
        is Map<*, *> -> entries.joinToString(
            prefix = "{",
            postfix = "}"
        ) { "${it.key.formatValue()}=${it.value.formatValue()}" }
        is Iterable<*> -> joinToString(prefix = "[", postfix = "]") { it.formatValue() }
        else -> toString()
    }

    private fun preferredOption(options: List<String>): String =
        options.firstOrNull { it in PREFERRED_OPTIONS } ?: options.firstOrNull().orEmpty()

    companion object {
        private val AREA_CLASSES = setOf(
            DoorArea::class,
            MirrorArea::class,
            SeatArea::class,
            WheelArea::class,
            WindowArea::class
        )

        private val PREFERRED_OPTIONS = setOf(
            "ROW_1_LEFT",
            "FRONT_LEFT",
            "FRONT_WINDSHIELD",
            "CELSIUS",
            "STOP",
            "OFF"
        )

    }
}
