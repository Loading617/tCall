package com.loading.tcall

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var callTimeListener: CallTimeListener
    private lateinit var callChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val totalTimeTextView = findViewById<TextView>(R.id.total_time)
        val todayTimeTextView = findViewById<TextView>(R.id.today_time)
        val exportButton = findViewById<Button>(R.id.export_button)
        callChart = findViewById(R.id.callChart)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALL_LOG), 1)
        } else {
            startCallTracking()
        }

        callTimeListener = CallTimeListener(this)
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        totalTimeTextView.text = "Total Call Time: ${callTimeListener.getTotalCallTime() / 1000} sec"
        todayTimeTextView.text = "Today's Call Time: ${callTimeListener.getDailyCallTime(todayDate) / 1000} sec"

        exportButton.setOnClickListener {
            exportCallData()
        }

        setupChart()
    }

    private fun startCallTracking() {
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as android.telephony.TelephonyManager
        callTimeListener = CallTimeListener(this)
        telephonyManager.listen(callTimeListener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun setupChart() {
        val callHistory = callTimeListener.getCallHistory()
        val entries = mutableListOf<BarEntry>()
        val dates = callHistory.keys.sorted() // Sort dates chronologically
        val dateLabels = mutableListOf<String>()

        dates.forEachIndexed { index, date ->
            entries.add(BarEntry(index.toFloat(), (callHistory[date] ?: 0) / 1000f))
            dateLabels.add(date)
        }

        val dataSet = BarDataSet(entries, "Call Duration (seconds)")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()

        val barData = BarData(dataSet)
        callChart.data = barData
        callChart.description.isEnabled = false
        callChart.invalidate() // Refresh chart

        // Format X-Axis
        val xAxis = callChart.xAxis
        xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(dateLabels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
    }

    private fun exportCallData() {
        val callHistory = callTimeListener.getCallHistory()
        val data = StringBuilder("Date,Duration (sec)\n")

        for ((date, duration) in callHistory) {
            data.append("$date,${duration / 1000}\n")
        }

        val fileName = "call_history.csv"
        val file = getExternalFilesDir(null)?.resolve(fileName)

        file?.writeText(data.toString())

        if (file != null) {
            showToast("Exported to: ${file.absolutePath}")
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
