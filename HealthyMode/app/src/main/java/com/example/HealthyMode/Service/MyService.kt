package com.example.HealthyMode.Service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.HealthyMode.R
import com.example.HealthyMode.Utils.Constant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate

import kotlin.math.sqrt

class MyService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var notification: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager

    // Custom Step Detection Variables
    private var currentSteps = 0
    private var lastStepTime: Long = 0

    // Tweak this threshold to make the counter more or less sensitive
    private val STEP_THRESHOLD = 5.0f
    // Minimum time between steps (in milliseconds) to prevent double-counting a single step
    private val STEP_COOLDOWN_MS = 300

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        stopSelf()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun start() {
        // Load initial steps from SharedPreferences
        currentSteps = Constant.loadData(this, "step_count", "total_step", "0")?.toInt() ?: 0

        setupNotification()
        startAccelerometer()
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    private fun startAccelerometer() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelSensor == null) {
            Toast.makeText(this, "Accelerometer not available", Toast.LENGTH_SHORT).show()
        } else {
            // SENSOR_DELAY_GAME is slightly faster than NORMAL, good for capturing step spikes
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupNotification() {
        val target = Constant.loadData(this, "myPrefs", "target", "1000").toString()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notification = NotificationCompat.Builder(this, "Stepcount")
            .setContentTitle("Tracking steps...")
            .setContentText("Current Steps : $currentSteps  Target Steps: $target")
            .setSmallIcon(R.drawable.mainlogo) // Make sure this icon exists
            .setOngoing(true)
            .setSilent(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1,
                notification.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(1, notification.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate the total magnitude of movement
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // Subtract standard gravity to isolate the movement force
            val magnitudeDelta = magnitude - SensorManager.GRAVITY_EARTH

            // If the force is greater than our threshold, it's a "spike" (a step)
            if (magnitudeDelta > STEP_THRESHOLD) {
                val currentTime = System.currentTimeMillis()

                // Check if enough time has passed since the last step
                if (currentTime - lastStepTime > STEP_COOLDOWN_MS) {
                    lastStepTime = currentTime
                    registerNewStep()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step counting
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun registerNewStep() {
        currentSteps++

        // Save locally
        Constant.savedata(this, "step_count", "total_step", currentSteps.toString())

        val target = Constant.loadData(this, "myPrefs", "target", "1000").toString()
        val curr_date = LocalDate.now().toString()

        // Update Notification
        notificationManager.notify(1,
            notification.setContentText("Current Steps : $currentSteps  Target Steps: $target").build()
        )

        // Upload to Firebase
        if (Constant.isInternetOn(applicationContext)) {
            dataupload(currentSteps.toString(), curr_date)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun dataupload(currsteps: String, curr_date: String) {
        val steps = hashMapOf(
            "steps" to currsteps,
            "date" to curr_date
        )
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            Firebase.firestore.collection("user").document(currentUser.uid)
                .collection("steps").document(curr_date).set(steps)
        }
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}