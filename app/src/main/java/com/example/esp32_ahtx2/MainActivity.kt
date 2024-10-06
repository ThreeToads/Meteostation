package com.example.esp32_ahtx2

import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var temperatureTextView: TextView
    private lateinit var humidityTextView: TextView
    private lateinit var temperatureProgressBar: ProgressBar
    private lateinit var humidityProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        temperatureTextView = findViewById(R.id.temperatureTextView)
        humidityTextView = findViewById(R.id.humidityTextView)
        temperatureProgressBar = findViewById(R.id.temperatureProgressBar)
        humidityProgressBar = findViewById(R.id.humidityProgressBar)
        // Разрешить выполнение сетевых операций в основном потоке
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // Стартуем фоновый поток для получения данных с ESP32
        thread {
            while (true) {
                getESP32Data()
                Thread.sleep(500) // Обновление данных каждые 0.5 секунд
            }
        }
    }

    // Функция для получения данных с ESP32
    private fun getESP32Data() {
        val url = "http://192.168.175.206/data"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    Log.d("ESP32 Response", responseData)

                    // Парсинг данных из строки "temp:<температура>,hum:<влажность>"
                    val data = parseESP32Response(responseData)

                    // Обновляем UI на основном потоке
                    runOnUiThread {
                        // Обновляем текстовые поля температуры и влажности
                        temperatureTextView.text = "${data["temp"]}°C"
                        humidityTextView.text = "${data["hum"]}%"


                        val temperature = data["temp"]?.toFloat() ?: 0f
                        val humidity = data["hum"]?.toFloat() ?: 0f

                        // Обновляем прогресс бары
                        temperatureProgressBar.progress = temperature.toInt().coerceIn(0, 50)
                        humidityProgressBar.progress = humidity.toInt().coerceIn(0, 100)
                    }
                } else {
                    Log.e("ESP32 Response", "Response body is null")
                }
            } else {
                Log.e("ESP32 Response", "Unsuccessful HTTP response")
            }
        } catch (e: IOException) {
            Log.e("ESP32 Request", "Failed to connect to ESP32", e)
        }
    }


    // Функция для парсинга строки вида "temp:26.81,hum:34.63"
    private fun parseESP32Response(response: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val parts = response.split(",")

        for (part in parts) {
            val keyValue = part.split(":")
            if (keyValue.size == 2) {
                val key = keyValue[0].trim()
                val value = keyValue[1].trim()
                result[key] = value
            }
        }

        return result
    }
}
