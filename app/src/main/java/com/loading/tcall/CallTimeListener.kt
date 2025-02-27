package com.loading.tcall

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class CallTimeListener(private val context: Context) : PhoneStateListener() {
    private var callStartTime: Long = 0
    private val sharedPref = context.getSharedPreferences("CallTracker", Context.MODE_PRIVATE)

    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Call started
                callStartTime = System.currentTimeMillis()
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // Call ended
                if (callStartTime > 0) {
                    val callEndTime = System.currentTimeMillis()
                    val duration = callEndTime - callStartTime
                    saveCallTime(duration)
                    callStartTime = 0
                }
            }
        }
    }

    private fun saveCallTime(duration: Long) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val totalCallDuration = sharedPref.getLong("totalCallDuration", 0) + duration
        val dailyCallDuration = sharedPref.getLong(date, 0) + duration

        sharedPref.edit()
            .putLong("totalCallDuration", totalCallDuration)
            .putLong(date, dailyCallDuration)
            .apply()
    }

    fun getTotalCallTime(): Long {
        return sharedPref.getLong("totalCallDuration", 0)
    }

    fun getDailyCallTime(date: String): Long {
        return sharedPref.getLong(date, 0)
    }

    fun getCallHistory(): Map<String, Long> {
        return sharedPref.all.filterKeys { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            .mapValues { it.value as Long }
    }
}
