package com.example.HealthyMode.Service // Adjust package as needed

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import com.example.HealthyMode.Utils.ActivityClassifier

class SensorTrackingService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var classifier: ActivityClassifier
    private lateinit var activityPrefs: SharedPreferences

    // A buffer to hold sensor readings until we have enough to classify
    private val sensorDataWindow = mutableListOf<FloatArray>()
    private val WINDOW_SIZE = 50 // e.g., 50 readings before classifying

    // Variables to track the current totals in memory before saving
    private var walkCount = 0
    private var upstairCount = 0
    private var downstairCount = 0
    private var sittingTimeMillis = 0L
    private var layingTimeMillis = 0L
    private var sleepingTimeMillis = 0L

    private var lastClassificationTime = System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize the Classifier
        classifier = ActivityClassifier()

        // 2. Open the exact SharedPreferences file the Fragment is reading from
        activityPrefs = getSharedPreferences("ActivityDataPrefs", Context.MODE_PRIVATE)

        // Load the existing data so we don't overwrite it with zeros
        loadExistingData()

        // 3. Start listening to the Sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        // Note: For a real app, this should be promoted to a Foreground Service
        // with a persistent notification, otherwise Android will kill it to save battery.
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            // Add the current X, Y, Z reading to our buffer
            val values = event.values.clone()
            sensorDataWindow.add(values)

            // Once we have enough data, run the SVM
            if (sensorDataWindow.size >= WINDOW_SIZE) {
                val predictedActivity = classifier.classifyActivity(sensorDataWindow)
                updateActivityTotals(predictedActivity)

                // Clear the window to start collecting the next batch of data
                sensorDataWindow.clear()
            }
        }
    }

    private fun updateActivityTotals(predictedActivity: Int) {
        val currentTime = System.currentTimeMillis()
        val timeSpentMillis = currentTime - lastClassificationTime
        lastClassificationTime = currentTime

        // Update local variables based on the SVM prediction
        when (predictedActivity) {
            ActivityClassifier.CLASS_WALK -> walkCount++ // Assuming 1 classification = 1 step (adjust as needed)
            ActivityClassifier.CLASS_UPSTAIR -> upstairCount++
            ActivityClassifier.CLASS_DOWNSTAIR -> downstairCount++
            ActivityClassifier.CLASS_SITTING -> sittingTimeMillis += timeSpentMillis
            ActivityClassifier.CLASS_LAYING -> layingTimeMillis += timeSpentMillis
            ActivityClassifier.CLASS_SLEEPING -> sleepingTimeMillis += timeSpentMillis
        }

        // Save the new totals to SharedPreferences so the Fragment can see them
        saveDataToSharedPreferences()
    }

    private fun saveDataToSharedPreferences() {
        // Convert milliseconds to Hours (Float) for the UI
        val sittingHrs = sittingTimeMillis / 3600000f
        val layingHrs = layingTimeMillis / 3600000f
        val sleepingHrs = sleepingTimeMillis / 3600000f

        // Write to the "Shared Whiteboard"
        activityPrefs.edit().apply {
            putInt("walk_count", walkCount)
            putInt("upstair_count", upstairCount)
            putInt("downstair_count", downstairCount)
            putFloat("sitting_hrs", sittingHrs)
            putFloat("laying_hrs", layingHrs)
            putFloat("sleeping_hrs", sleepingHrs)
            apply() // apply() is asynchronous and safe to call frequently
        }
    }

    private fun loadExistingData() {
        walkCount = activityPrefs.getInt("walk_count", 0)
        upstairCount = activityPrefs.getInt("upstair_count", 0)
        downstairCount = activityPrefs.getInt("downstair_count", 0)
        sittingTimeMillis = (activityPrefs.getFloat("sitting_hrs", 0f) * 3600000).toLong()
        layingTimeMillis = (activityPrefs.getFloat("laying_hrs", 0f) * 3600000).toLong()
        sleepingTimeMillis = (activityPrefs.getFloat("sleeping_hrs", 0f) * 3600000).toLong()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not usually needed for basic accelerometer tracking
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We are using a Started Service, not a Bound Service
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}