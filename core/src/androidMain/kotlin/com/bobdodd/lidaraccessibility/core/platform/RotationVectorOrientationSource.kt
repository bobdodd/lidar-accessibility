package com.bobdodd.lidaraccessibility.core.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Android actual for [DeviceOrientationSource].
 *
 * Uses `TYPE_ROTATION_VECTOR` which is already tilt-compensated and
 * fused by the platform. The core [HeadingSmoother] applies additional
 * filtering on top; tilt compensation is deliberately not redone here
 * (see architecture.md § "HeadingSmoother").
 */
class RotationVectorOrientationSource(context: Context) : DeviceOrientationSource {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _updates = MutableSharedFlow<Orientation>(extraBufferCapacity = 64)
    override val updates = _updates.asSharedFlow()

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Convert radians → degrees
            // [0] = azimuth (yaw), [1] = pitch, [2] = roll
            val yawDeg = Math.toDegrees(orientationAngles[0].toDouble())
            val pitchDeg = Math.toDegrees(orientationAngles[1].toDouble())
            val rollDeg = Math.toDegrees(orientationAngles[2].toDouble())

            // Normalize yaw to 0–360 clockwise from north
            val yawNormalized = (yawDeg + 360.0) % 360.0

            val accuracy = when (event.accuracy) {
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> OrientationAccuracy.HIGH
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> OrientationAccuracy.MEDIUM
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> OrientationAccuracy.LOW
                else -> OrientationAccuracy.UNRELIABLE
            }

            _updates.tryEmit(
                Orientation(
                    yawDeg = yawNormalized,
                    pitchDeg = pitchDeg,
                    rollDeg = rollDeg,
                    accuracy = accuracy,
                    timestampMs = System.currentTimeMillis(),
                )
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun start() {
        rotationVector?.let {
            sensorManager.registerListener(
                listener, it, SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun stop() {
        sensorManager.unregisterListener(listener)
    }
}
