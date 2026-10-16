package ai.pleos.playground.sample.vehicle.explorer

enum class VehicleDomain(
    val title: String,
    val description: String,
    val publicApiCount: Int
) {
    BRAKE("Brake", "Brake pedal status", 4),
    CAR_INFO("Car info", "Vehicle identity and capability information", 2),
    DISPLAY("Display", "Display units", 4),
    DOOR("Door", "Door position, locks, and glove box", 15),
    DRIVING_MODE("Driving mode", "Drive mode and cruise control", 12),
    EV_BATTERY("EV battery", "Charging and electric energy", 30),
    FUEL("Fuel", "Fuel level and economy", 20),
    HVAC("HVAC", "Climate, temperature, and airflow", 72),
    LIGHT("Light", "Exterior, cabin, and ambient lighting", 71),
    ODOMETER("Odometer", "Distance and trip statistics", 34),
    SAFETY("Safety", "Safety indicators and warnings", 33),
    SEAT("Seat", "Seat position, belt, heat, and ventilation", 86),
    SIDE_MIRROR("Side mirror", "Mirror position and heating", 5),
    STEERING_WHEEL("Steering wheel", "Angle, heat, and controls", 9),
    TIRE("Tire", "Public tire capability information", 2),
    TOLL("Toll", "ETCS settings", 5),
    TURN_SIGNAL("Turn signal", "Turn signal state", 4),
    WINDOW("Window", "Window, blind, and smart glass", 26),
    WIPER("Wiper", "Wiper state and switch", 7)
}
