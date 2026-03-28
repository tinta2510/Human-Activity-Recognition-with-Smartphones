package com.example.HealthyMode.UI.Home_fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.HealthyMode.R
import com.example.HealthyMode.Utils.Constant
import com.example.HealthyMode.databinding.FragmentHomeFragmentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.O)
class Home_fragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: FragmentHomeFragmentBinding
    private lateinit var dialog: Dialog
    private val handler = Handler(Looper.getMainLooper())

    // The shared preferences file where your teammate will save the data
    private lateinit var activityPrefs: SharedPreferences

    private var userDitails: DocumentReference = Firebase.firestore.collection("user").document(
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    )

    // --- THE CONTRACT KEYS ---
    // You and your teammate must use exactly these keys to save and load data
    companion object {
        const val PREFS_NAME = "ActivityDataPrefs"
        const val KEY_WALK = "walk_count"
        const val KEY_UPSTAIR = "upstair_count"
        const val KEY_DOWNSTAIR = "downstair_count"
        const val KEY_SITTING = "sitting_hrs"
        const val KEY_LAYING = "laying_hrs"
        const val KEY_SLEEPING = "sleeping_hrs"
    }

    private val generalUIRunnable = object : Runnable {
        override fun run() {
            greeting_class()
            if (!Constant.isInternetOn(requireContext())) {
                binding.net.visibility = View.VISIBLE
            } else {
                binding.net.visibility = View.GONE
            }
            handler.postDelayed(this, 60000) // Update every minute
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeFragmentBinding.inflate(inflater, container, false)
        dialog = Dialog(requireActivity())

        // Initialize SharedPreferences
        activityPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load User Name
        userDitails.addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            if (value != null && value.exists()) {
                binding.name.text = value.data?.get("fullname")?.toString() ?: ""
            }
        }

        setTargets()
        generalUIRunnable.run()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // 1. When the user opens the app, fetch the latest data immediately
        loadAllActivityData()

        // 2. Start listening in case the user leaves the app open and walks around
        activityPrefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        // Stop listening when the app goes to the background to save battery
        activityPrefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(generalUIRunnable)
    }

    // --- UI UPDATE LOGIC ---

    // Triggered automatically if the Service updates SharedPreferences while the app is open
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            KEY_WALK -> updateWalkUI()
            KEY_UPSTAIR -> updateUpstairUI()
            KEY_DOWNSTAIR -> updateDownstairUI()
            KEY_SITTING -> updateSittingUI()
            KEY_LAYING -> updateLayingUI()
            KEY_SLEEPING -> updateSleepingUI()
        }
    }

    private fun loadAllActivityData() {
        updateWalkUI()
        updateUpstairUI()
        updateDownstairUI()
        updateSittingUI()
        updateLayingUI()
        updateSleepingUI()
    }

    private fun updateWalkUI() {
        val count = activityPrefs.getInt(KEY_WALK, 0)
        binding.walk.text = count.toString()
        binding.circularProgressBarWalk.progress = count.toFloat()
    }

    private fun updateUpstairUI() {
        val count = activityPrefs.getInt(KEY_UPSTAIR, 0)
        binding.walkUpstair.text = count.toString()
        binding.circularProgressBarUpstair.progress = count.toFloat()
    }

    private fun updateDownstairUI() {
        val count = activityPrefs.getInt(KEY_DOWNSTAIR, 0)
        binding.walkDownstair.text = count.toString()
        binding.circularProgressBarDownstair.progress = count.toFloat()
    }

    @SuppressLint("SetTextI18n")
    private fun updateSittingUI() {
        val hrs = activityPrefs.getFloat(KEY_SITTING, 0f)
        binding.sittingHrs.text = String.format("%.1f", hrs)
        binding.sittingbar.progress = hrs
    }

    @SuppressLint("SetTextI18n")
    private fun updateLayingUI() {
        val hrs = activityPrefs.getFloat(KEY_LAYING, 0f)
        binding.layingHrs.text = String.format("%.1f", hrs)
        binding.layingbar.progress = hrs
    }

    @SuppressLint("SetTextI18n")
    private fun updateSleepingUI() {
        val hrs = activityPrefs.getFloat(KEY_SLEEPING, 0f)
        binding.hrs.text = String.format("%.1f", hrs)
        binding.sleepingbar.progress = hrs
    }

    private fun setTargets() {
        val myPrefs = requireActivity().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val target = myPrefs.getString("target", "1000") ?: "1000"

        binding.circularProgressBarWalk.progressMax = target.toFloat()
        binding.goal.text = target

        binding.circularProgressBarUpstair.progressMax = 1000f
        binding.goalUpstair.text = "1000"

        binding.circularProgressBarDownstair.progressMax = 1000f
        binding.goalDownstair.text = "1000"

        binding.sittingbar.progressMax = 24f
        binding.layingbar.progressMax = 24f
        binding.sleepingbar.progressMax = 24f
    }

    private fun greeting_class() {
        val time: ImageView = binding.weather
        val greeting: TextView = binding.greeting
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        if (hour in 6..17) {
            time.setImageResource(R.drawable.sun)
            greeting.text = when (hour) {
                in 6..12 -> "Good Morning !"
                in 13..14 -> "Good Noon !"
                else -> "Good Afternoon !"
            }
        } else {
            time.setImageResource(R.drawable.moon)
            greeting.text = if (hour in 18..19) "Good Evening !" else "Good Night !"
        }
    }
}