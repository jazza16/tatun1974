package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CompassState(
    val azimuthDegrees: Float = 0f,
    val cardinalDirection: String = "Utara",
    val cardinalDirectionShort: String = "U",
    val hasHardwareSensor: Boolean = false,
    val targetEvacuationHeadingDegrees: Float = 65f, // Arah Dataran Tinggi / TES Tsunami Mukomuko (~Timur Laut)
    val evacuationPointName: String = "TES Tsunami Dataran Tinggi Mukomuko",
    val distanceKmToEvacuationPoint: Float = 1.4f
)

/**
 * Sensor-based digital compass utility that tracks real device orientation
 * using SensorManager (Rotation Vector / Accelerometer + Magnetometer).
 */
class CompassSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _compassState = MutableStateFlow(
        CompassState(hasHardwareSensor = rotationSensor != null || (accelerometer != null && magnetometer != null))
    )
    val compassState: StateFlow<CompassState> = _compassState.asStateFlow()

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    fun startListening() {
        if (sensorManager == null) return

        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            }
            if (magnetometer != null) {
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    fun setManualHeading(degrees: Float) {
        val normalized = (degrees % 360f + 360f) % 360f
        val (cardinal, short) = getCardinal(normalized)
        _compassState.value = _compassState.value.copy(
            azimuthDegrees = normalized,
            cardinalDirection = cardinal,
            cardinalDirectionShort = short
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val azimuthInRadians = orientation[0]
                var azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
                if (azimuthInDegrees < 0) {
                    azimuthInDegrees += 360f
                }
                updateHeading(azimuthInDegrees, hasSensor = true)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravity, 0, 3)
                hasGravity = true
                if (hasGravity && hasGeomagnetic) {
                    computeOrientationFromGravityAndGeo()
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                hasGeomagnetic = true
                if (hasGravity && hasGeomagnetic) {
                    computeOrientationFromGravityAndGeo()
                }
            }
        }
    }

    private fun computeOrientationFromGravityAndGeo() {
        val r = FloatArray(9)
        val i = FloatArray(9)
        if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(r, orientation)
            var azimuthInDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (azimuthInDegrees < 0) {
                azimuthInDegrees += 360f
            }
            updateHeading(azimuthInDegrees, hasSensor = true)
        }
    }

    private fun updateHeading(degrees: Float, hasSensor: Boolean) {
        val (cardinal, short) = getCardinal(degrees)
        _compassState.value = _compassState.value.copy(
            azimuthDegrees = degrees,
            cardinalDirection = cardinal,
            cardinalDirectionShort = short,
            hasHardwareSensor = hasSensor
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        fun getCardinal(degrees: Float): Pair<String, String> {
            val deg = (degrees % 360 + 360) % 360
            return when {
                deg >= 337.5 || deg < 22.5 -> "Utara" to "U"
                deg < 67.5 -> "Timur Laut" to "TL"
                deg < 112.5 -> "Timur" to "T"
                deg < 157.5 -> "Tenggara" to "TG"
                deg < 202.5 -> "Selatan" to "S"
                deg < 247.5 -> "Barat Daya" to "BD"
                deg < 292.5 -> "Barat" to "B"
                else -> "Barat Laut" to "BL"
            }
        }
    }
}
