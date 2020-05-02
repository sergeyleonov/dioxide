package com.sleonov.dioxide.types

class Value {

    val carbonDioxidePpm: Int
    val temperature: Double

    constructor(carbonDioxidePpm: Int, temperature: Double) {
        this.carbonDioxidePpm = carbonDioxidePpm
        this.temperature = temperature
    }

}
