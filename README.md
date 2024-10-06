# ESP32 AHTX0 MeteoStation


## Requirements

- ESP32 development board
- AHTX0 sensor
- PlatformIO
- Arduino framework

## Code: Создайте эти 2 файла ( в Platformio VS Code or Arduino IDE) и загрузите в модуль ESP32 & AHT2x

### `main.cpp`

```cpp
#include <Arduino.h>
#include <WiFi.h>
#include <Wire.h>

#include <Adafruit_AHTX0.h>
#include <BluetoothSerial.h>
#include <ESPAsyncWebServer.h>

const char* ssid = "your_name_wifi";
const char* password = "password_wifi";

Adafruit_AHTX0 aht;
AsyncWebServer server(80);

void setup() {
  Serial.begin(115200);

  // Подключение к Wi-Fi
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("Connected to WiFi");
  Serial.println(WiFi.localIP());

  // Инициализация датчика AHT20
  if (!aht.begin()) {
    Serial.println("Failed to find AHT sensor!");
    while (1) delay(10);
  }

  // Настройка веб-сервера для отправки данных
  server.on("/data", HTTP_GET, [](AsyncWebServerRequest *request) {
    sensors_event_t humidity, temp;
    aht.getEvent(&humidity, &temp);
    String data = "temp:" + String(temp.temperature) + ",hum:" + String(humidity.relative_humidity);
    request->send(200, "text/plain", data);
  });

  server.begin();
}

void loop() {

}
```
### `platform.ini`
```ini
[env:esp32dev]
platform = espressif32
board = esp32dev
framework = arduino
monitor_speed = 115200
lib_deps =
  me-no-dev/ESP Async WebServer @ ^1.2.3
  https://github.com/adafruit/Adafruit_AHTX0
    
