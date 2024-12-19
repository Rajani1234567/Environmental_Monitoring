#include <Arduino.h>
#include <ESP8266WebServer.h>
#include <ESP8266WiFi.h>
#include <hp_BH1750.h>
#include <Adafruit_Sensor.h>
#include <DHT.h>
#include <ArduinoJson.h>  

ESP8266WebServer server(80);  

#define DHTPIN D4       
#define DHTTYPE DHT11    

DHT dht(DHTPIN, DHTTYPE);
hp_BH1750 BH1750;  

const char* ssid = "your-SSID ";    
const char* password = "your-PASSWORD";  

void setup() {
  Serial.begin(9600);  
  delay(1000);

   
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(3000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("Connected to Wi-Fi");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());

   
  server.begin();

  
  dht.begin();
  Serial.println("DHT11 initialized.");

  bool avail = BH1750.begin(BH1750_TO_GROUND);
  if (!avail) {
    Serial.println("No BH1750 sensor found!");
    while (true) {};  
  } else {
    Serial.println("BH1750 initialized.");
  }

 
  server.on("/getData", HTTP_GET, handleGetData);
}

void loop() {
  
  server.handleClient();
}


void handleGetData() {
  
  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();

  
  BH1750.start();  
  delay(150);      
  float lux = BH1750.getLux();

 
  if (isnan(temperature) || isnan(humidity)) {
    server.send(500, "text/plain", "Failed to read from DHT11!");
    return;
  }

  if (lux < 0) {
    server.send(500, "text/plain", "Failed to read from BH1750!");
    return;
  }

  
  String jsonResponse = createJsonResponse(temperature, humidity, lux);

  
  server.send(200, "application/json", jsonResponse);
  Serial.println("Data sent to client: ");
  Serial.println(jsonResponse);
}
 

String createJsonResponse(float temperature, float humidity, float lux) {
  StaticJsonDocument<200> doc;
  doc["temperature"] = temperature;
  doc["humidity"] = humidity;
  doc["lux"] = lux;

  
  String json;
  serializeJson(doc, json);
  return json;
}
