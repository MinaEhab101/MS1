package com.example.ui.screens.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sqrt

/**
 * Sensor-based Shake-to-Roll & Physics Controller.
 * Monitors device accelerometer to detect realistic physical shaking of the phone
 * to trigger dice throws with natural impulse.
 */
class DicePhysicsController(
    private val context: Context,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastTime = 0L
    private var shakeCount = 0
    private var lastShakeTimestamp = 0L

    companion object {
        private const val SHAKE_THRESHOLD = 12.5f
        private const val SHAKE_SLOP_TIME_MS = 250
        private const val SHAKE_COUNT_RESET_TIME_MS = 1200
        private const val MIN_SHAKES_TO_TRIGGER = 2
    }

    fun startListening() {
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val now = System.currentTimeMillis()
        if (now - lastTime < 50) return // Throttle checks to 20Hz

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        if (lastTime != 0L) {
            val deltaX = x - lastX
            val deltaY = y - lastY
            val deltaZ = z - lastZ

            val speed = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()

            if (speed > SHAKE_THRESHOLD) {
                if (now - lastShakeTimestamp > SHAKE_SLOP_TIME_MS) {
                    shakeCount++
                    lastShakeTimestamp = now

                    if (shakeCount >= MIN_SHAKES_TO_TRIGGER) {
                        shakeCount = 0
                        onShakeDetected()
                    }
                }
            } else if (now - lastShakeTimestamp > SHAKE_COUNT_RESET_TIME_MS) {
                shakeCount = 0
            }
        }

        lastX = x
        lastY = y
        lastZ = z
        lastTime = now
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}

/**
 * Composable hook to activate sensor shake-to-roll physics whenever the user is in ROLL_DICE phase.
 */
@Composable
fun RememberShakeDetector(
    isEnabled: Boolean,
    onShake: () -> Unit
) {
    val context = LocalContext.current
    val controller = remember(context, isEnabled) {
        DicePhysicsController(context) {
            if (isEnabled) {
                onShake()
            }
        }
    }

    DisposableEffect(controller, isEnabled) {
        if (isEnabled) {
            controller.startListening()
        }
        onDispose {
            controller.stopListening()
        }
    }
}

/**
 * 3D Physics Trajectory parameters for rolling dice on the wooden/felt board.
 */
data class DicePhysicsState(
    val posX: Float,
    val posY: Float,
    val heightZ: Float,
    val rotX: Float,
    val rotY: Float,
    val rotZ: Float,
    val scale: Float,
    val shadowAlpha: Float,
    val shadowOffset: Float
)

/**
 * Calculates physically accurate 3D bounce trajectory, dampening, and shadow projection.
 */
fun computeDicePhysics(
    progress: Float, // 0f -> 1f
    launchX: Float,
    launchY: Float,
    targetX: Float,
    targetY: Float,
    restAngle: Float,
    restJitterX: Float,
    restJitterY: Float
): DicePhysicsState {
    // 1. Horizontal Trajectory (Ease-out ballistic travel)
    val travelProgress = kotlin.math.sin(progress * (kotlin.math.PI / 2f).toFloat())
    val currX = launchX + (targetX + restJitterX - launchX) * travelProgress
    val currY = launchY + (targetY + restJitterY - launchY) * travelProgress

    // 2. Vertical Height Z (Dual-stage physical bounce)
    // 0.0 -> 0.45: Primary flight arc up to apex and down to felt
    // 0.45 -> 0.75: Secondary bounce arc (restitution ~ 0.42)
    // 0.75 -> 0.90: Micro vibration bounce
    // 0.90 -> 1.0: Settled at rest
    val zHeight: Float = when {
        progress < 0.45f -> {
            val t = progress / 0.45f
            kotlin.math.sin(t * kotlin.math.PI.toFloat()) * 1.0f
        }
        progress < 0.75f -> {
            val t = (progress - 0.45f) / 0.30f
            kotlin.math.sin(t * kotlin.math.PI.toFloat()) * 0.38f
        }
        progress < 0.90f -> {
            val t = (progress - 0.75f) / 0.15f
            kotlin.math.sin(t * kotlin.math.PI.toFloat()) * 0.12f
        }
        else -> 0f
    }

    // 3. 3D Tumbling Angular Momentum
    val spinDecay = (1f - progress).coerceAtLeast(0f)
    val rotX = spinDecay * 720f
    val rotY = spinDecay * 540f
    val rotZ = (spinDecay * 360f) + restAngle

    // 4. Perspective Scale and Dynamic Contact Shadows
    val scale = 1.0f + (zHeight * 0.35f)
    val shadowAlpha = (0.55f * (1f - zHeight * 0.45f)).coerceIn(0.2f, 0.6f)
    val shadowOffset = 3f + (zHeight * 22f)

    return DicePhysicsState(
        posX = currX,
        posY = currY,
        heightZ = zHeight,
        rotX = rotX,
        rotY = rotY,
        rotZ = rotZ,
        scale = scale,
        shadowAlpha = shadowAlpha,
        shadowOffset = shadowOffset
    )
}
