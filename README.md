<div align="center">

# 🛡️ ByePass

**Rootless, zero-latency Android local DPI & DNS circumvention engine.**  
*GoodbyeDPI / ByeDPI alternative for Android devices.*

[![Release](https://img.shields.io/github/v/release/akifkr/ByePass?color=34D399&label=Latest%20Release)](https://github.com/akifkr/ByePass/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-blue?logo=android)](https://github.com/akifkr/ByePass)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%20100%25-7F52FF?logo=kotlin)](https://github.com/akifkr/ByePass)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[📥 **Download Latest APK**](https://github.com/akifkr/ByePass/releases/latest) • [🇬🇧 English](#-english) • [🇹🇷 Türkçe](#-türkçe)

---

</div>

## 🇬🇧 English

ByePass is an open-source Android utility designed to bypass Deep Packet Inspection (DPI) censorship and ISP DNS poisoning locally on your device. 

Unlike traditional VPNs, **ByePass does NOT route your traffic through remote servers**. It adds **0 ms latency (zero ping)** and operates entirely in a local loopback on your device by manipulating outbound TCP/TLS handshakes.

### ✨ Features
- ⚡ **Zero Added Latency:** No middleman VPN servers; direct peer-to-peer connection with your normal internet speed.
- 🧩 **TLS SNI Fragmentation:** Slices the `ClientHello` packet right at the record boundary (`0x16 0x03`), preventing DPI middleboxes from inspecting the domain name.
- 🌐 **Anti-Poisoning DNS:** Queries dedicated DNS endpoints over alternative ports (e.g., Port 1253) to completely bypass transparent UDP 53 redirection.
- 📱 **Wide Compatibility:** Tested and functional on Android 7.0+ (API 24) through modern Android versions.
- 🎨 **Jetpack Compose UI:** Minimal, lightweight, dark-themed diagnostic interface with live traffic logging.

### 🚀 Quick Start
1. Download the latest `ByePass-v1.0.0.apk` from the [**Releases**](https://github.com/akifkr/ByePass/releases/latest) section.
2. Install the APK on your Android phone or tablet.
3. Open the app and tap **"Bypass Başlat"**.
4. *(For Android 7–9)*: Configure your Wi-Fi proxy once to `127.0.0.1:10808`. *(Android 10+ handles routing automatically).*

---

## 🇹🇷 Türkçe

ByePass; Android cihazlar için geliştirilmiş, root yetkisi gerektirmeyen, internet hızını yavaşlatmadan (sıfır ping artışı ile) çalışan yerel bir DPI ve DNS filtre aşma motorudur (GoodbyeDPI / ByeDPI Android mantığıyla çalışır).

Trafiğinizi uzak bir sunucuya göndermez; tüm paket parçalama işlemlerini cihazın kendi içinde (yerel döngüde) gerçekleştirir.

### ✨ Öne Çıkan Özellikler
- ⚡ **Sıfır Gecikme (0 ms Ping):** Harici bir VPN sunucusu kullanmaz. İnternet hızınız neyse aynı hız ve ping ile bağlanırsınız.
- 🧩 **TLS SNI Parçalama:** Yasaklı sitelerin açılmasını sağlamak için `ClientHello` paketlerini 2 byte seviyesinde böler; operatörlerin DPI filtreleri domaini okuyamaz.
- 🌐 **Zehirlenmeyen DNS:** Standart 53 portundaki operatör engellerini aşmak için alternatif port üzerinden (Port 1253) engelsiz sorgu yapar (Discord vb. kısıtlı servisler için optimize edilmiştir).
- 🔋 **Pil Dostu:** Arka planda gereksiz CPU/RAM harcamayan saf soket aktarımı.

### 📲 Kurulum ve Kullanım
1. [**Releases (Sürümler)**](https://github.com/akifkr/ByePass/releases/latest) sekmesinden en güncel `.apk` dosyasını indirin.
2. Telefonunuza kurup uygulamayı açın ve **"Bypass Başlat"** butonuna basın.
3. Keyfini çıkarın!

---

## ⚖️ License
This project is open-source and licensed under the [MIT License](LICENSE). Developed for educational and privacy research purposes.
