// ESP32 DevKit + potenciômetro para uma leitura de nível simulada.
#include <Arduino.h>
#include <WiFi.h>
#include <esp_http_client.h>
#include <esp_crt_bundle.h>
#include <time.h>

const int SENSOR = 34;
const int ALERTA = 25;
const char* BASE = "https://nexus-verde-java-gs2026-2-tprk.onrender.com";
String ponto, chave, pendente;
unsigned long tentativa = 0;
int enviado = -1, nivelPendente = -1;

String evento() {
  uint8_t id[16];
  esp_fill_random(id, sizeof(id));
  id[6] = (id[6] & 15) | 64;
  id[8] = (id[8] & 63) | 128;
  char resultado[37];
  snprintf(resultado,sizeof(resultado),"%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
           id[0],id[1],id[2],id[3],id[4],id[5],id[6],id[7],id[8],id[9],id[10],id[11],id[12],id[13],id[14],id[15]);
  return String(resultado);
}

int transmitir() {
  String url = String(BASE)+"/api/iot/pontos/"+ponto+"/leituras";
  esp_http_client_config_t cfg = {};
  cfg.url=url.c_str();
  cfg.crt_bundle_attach=esp_crt_bundle_attach;
  cfg.timeout_ms=15000;
  esp_http_client_handle_t http=esp_http_client_init(&cfg);
  if (!http) return -1;
  esp_http_client_set_method(http,HTTP_METHOD_POST);
  esp_http_client_set_header(http,"Content-Type","application/json");
  esp_http_client_set_header(http,"X-Sensor-Key",chave.c_str());
  esp_http_client_set_post_field(http,pendente.c_str(),pendente.length());
  int status=esp_http_client_perform(http)==ESP_OK ? esp_http_client_get_status_code(http) : -1;
  esp_http_client_cleanup(http);
  return status;
}

void setup() {
  Serial.begin(115200);
  pinMode(ALERTA, OUTPUT);
  WiFi.begin("Wokwi-GUEST","",6);
  configTime(0,0,"pool.ntp.org","time.google.com");
  Serial.println("Cole a linha de pareamento do painel: ID do ponto e chave temporaria.");
}

void loop() {
  int nivel=round(analogRead(SENSOR)*100.0/4095.0);
  digitalWrite(ALERTA,nivel<=15 ? HIGH : LOW);
  if (Serial.available()) {
    String linha=Serial.readStringUntil('\n');
    linha.trim();
    int espaco=linha.indexOf(' ');
    String id=linha.substring(0,espaco);
    String nova=linha.substring(espaco+1);
    bool valido=espaco>0 && nova.length()==72;
    for (size_t i=0; i<id.length(); i++) valido=valido && isDigit(id[i]);
    if (valido) {
      ponto=id; chave=nova; pendente=""; enviado=-1;
      Serial.println("Ponto pareado. Aguardando WiFi e horario UTC.");
    }
  }
  if (millis()-tentativa<5000 || !chave.length() || WiFi.status()!=WL_CONNECTED) { delay(50); return; }
  tentativa=millis();
  time_t agora=time(nullptr);
  if (agora<1704067200) return;
  if (pendente.isEmpty()) {
    if (enviado==nivel) return;
    char utc[25];
    struct tm data;
    gmtime_r(&agora,&data);
    strftime(utc,sizeof(utc),"%Y-%m-%dT%H:%M:%SZ",&data);
    nivelPendente=nivel;
    pendente=String("{\"evento\":\"")+evento()+"\",\"medidaEm\":\""+utc+"\",\"percentual\":"+String(nivel)+"}";
    Serial.println(pendente);
  }
  int status=transmitir();
  Serial.printf("HTTP %d\n",status);
  if (status==204) { enviado=nivelPendente; pendente=""; }
  if (status==400 || status==409) pendente="";
  if (status==401 || status==403 || status==404) { chave=""; pendente=""; Serial.println("Confira o ponto e gere novo pareamento."); }
}
