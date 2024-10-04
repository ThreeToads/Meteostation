package com.example.esp32_ahtx2

import android.os.Bundle
import android.os.StrictMode
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Найдем TextView для отображения температуры и влажности
        temperatureTextView = findViewById(R.id.temperatureTextView)
        humidityTextView = findViewById(R.id.humidityTextView)

        // Разрешить выполнение сетевых операций в основном потоке (не рекомендуется для production)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // Стартуем фоновый поток для получения данных с ESP32
        thread {
            while (true) {
                getESP32Data()
                Thread.sleep(500) // Обновление данных каждые 5 секунд
            }
        }
    }

    // Функция для получения данных с ESP32
    private fun getESP32Data() {
        val url = "http://192.168.175.206/data" // Адрес вашего ESP32

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
                        temperatureTextView.text = "Temperature: ${data["temp"]}°C"
                        humidityTextView.text = "Humidity: ${data["hum"]}%"
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
