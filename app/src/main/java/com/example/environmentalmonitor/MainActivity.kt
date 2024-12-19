package com.example.environmentalmonitor

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Modifier
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import androidx.compose.ui.graphics.toArgb
import okhttp3.Request
import org.json.JSONObject
import com.example.environmentalmonitor.ui.theme.EnvironmentalMonitorTheme


class MainActivity : ComponentActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContent {
            EnvironmentalMonitorTheme {
                MainScreen()
            }
        }
    }
}

class TimeValueFormatter(private val timestamps: List<Long>) : ValueFormatter() {
    private val dateFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())

    override fun getFormattedValue(value: Float): String {
        val index = value.toInt()
        return if (index in timestamps.indices) {
            dateFormat.format(timestamps[index])
        } else {
            "" //empty if index is out of bounds
        }
    }
}

@Composable
fun LightIntensityGraph(lightData: List<Float>, timestamps: List<Long>) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {

                val dataSet = LineDataSet(lightData.mapIndexed { index, value ->
                    com.github.mikephil.charting.data.Entry(index.toFloat(), value)
                }, "Light Intensity").apply {
                    color = Color(0xFF0000FF).toArgb() //blue
                    valueTextColor = Color(0xFFFF0000).toArgb() //red
                }
                val lineData = LineData(dataSet)
                this.data = lineData

                //graph labels and legend
                xAxis.apply {
                    valueFormatter = TimeValueFormatter(timestamps) //custom formatter
                    position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                    textColor = Color(0xFFC0A000).toArgb() //X-Axis label color
                    granularity = 1f
                }

                axisLeft.apply {
                    textColor = Color(0xFFC0A000).toArgb() //Y-Axis label color
                }

                axisRight.apply {
                    isEnabled = false //Disable right Y-Axis
                }

                legend.apply {
                    textColor = Color(0xFFC0A000).toArgb() //legend text color
                }

                invalidate()
            }
        },
        update = { chart ->
            val dataSet = LineDataSet(lightData.mapIndexed { index, value ->
                com.github.mikephil.charting.data.Entry(index.toFloat(), value)
            }, "Light Intensity").apply {
                color = Color(0xFF0000FF).toArgb() //blue
                valueTextColor = Color(0xFFFF0000).toArgb() //red
            }
            val lineData = LineData(dataSet)
            chart.data = lineData

            chart.xAxis.apply {
                valueFormatter = TimeValueFormatter(timestamps)
                granularity = 1f
            }

            chart.invalidate() //refresh chart
        },
        modifier = Modifier.fillMaxWidth().height(300.dp)
    )
}

@Composable
fun MainScreen() {
    val coroutineScope = rememberCoroutineScope()
    var temperature by remember { mutableStateOf("--°C") }
    var humidity by remember { mutableStateOf("--%") }
    var lux by remember { mutableStateOf("-- lx") }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val steelBlue = Color(0xFF4682B4)
    val olive = Color(0xFFC0A000)
    val timestamps = remember { mutableStateListOf<Long>() }
    val lightData = remember { mutableStateListOf<Float>() }


    val fetchData = {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = fetchSensorData()
                val jsonResponse = JSONObject(response)

                val temp = jsonResponse.getDouble("temperature")
                val hum = jsonResponse.getDouble("humidity")
                val light = jsonResponse.getDouble("lux")

                coroutineScope.launch(Dispatchers.Main) {
                    temperature = "$temp°C"
                    humidity = "$hum%"
                    lux = "$light lx"

                    lightData.add(light.toFloat())
                    timestamps.add(System.currentTimeMillis())

                    // graph's last x readings
                    if (lightData.size > 10) {
                        lightData.removeAt(0)
                        timestamps.removeAt(0)
                    }

                    if (isLoading) {
                        isLoading = false //hide loading indicator after first fetch
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                coroutineScope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to fetch sensor data: ${e.message}", Toast.LENGTH_LONG).show()
                }
                if (isLoading) {
                    isLoading = false //hide loading on error
                }
            }
        }
    }


    //fetch data
    LaunchedEffect(Unit) {
        fetchData()
        while (true) {
            delay(1000) //milliseconds
            fetchData()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Temperature Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(100.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = MaterialTheme.shapes.medium.copy(CornerSize(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Red)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Thermostat,
                        contentDescription = "Temperature",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(text = "Temperature", style = MaterialTheme.typography.titleLarge)
                        Text(text = temperature, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Humidity Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(100.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = MaterialTheme.shapes.medium.copy(CornerSize(16.dp)),
                colors = CardDefaults.cardColors(containerColor = steelBlue)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cloud,
                        contentDescription = "Humidity",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(text = "Humidity", style = MaterialTheme.typography.titleLarge)
                        Text(text = humidity, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Light Intensity Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(100.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = MaterialTheme.shapes.medium.copy(CornerSize(16.dp)),
                colors = CardDefaults.cardColors(containerColor = olive)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = "Light Intensity",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(text = "Light Intensity", style = MaterialTheme.typography.titleLarge)
                        Text(text = lux, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Light Intensity Graph
            LightIntensityGraph(lightData, timestamps)

            Spacer(modifier = Modifier.height(16.dp))

            //refresh
            Button(
                onClick = {
                    isLoading = true
                    fetchData()
                },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(text = if (isLoading) "Loading..." else "Refresh")
            }
        }
    }
}


fun fetchSensorData(): String {
    val esp8266Url = "http://192.168.1.105/getData"
    val client = OkHttpClient.Builder()
        .callTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    val request = Request.Builder()
        .url(esp8266Url)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("Unexpected response: ${response.code}")
        return response.body?.string() ?: throw Exception("Empty response body")
    }
}
