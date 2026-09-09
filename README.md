<div align="center">

# ByePass

**Rootless, zero-latency Android local DPI & DNS circumvention engine.**  
*GoodbyeDPI / ByeDPI alternative designed for Android environments.*

[![Release](https://img.shields.io/github/v/release/akifkr/ByePass?label=Latest%20Release&color=blue)](https://github.com/akifkr/ByePass/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-brightgreen)](https://github.com/akifkr/ByePass)
[![Language](https://img.shields.io/badge/Language-Kotlin-purple)](https://github.com/akifkr/ByePass)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[📥 Download APK](https://github.com/akifkr/ByePass/releases/latest) • [🇬🇧 English](#-english) • [🇹🇷 Türkçe](#-türkçe)

---

</div>

### Comparison / Karşılaştırma

| Özellik / Feature | Standart VPN | ByePass |
| :--- | :---: | :---: |
| **Ek Gecikme / Ping** | +50 - 200 ms | **0 ms (Sıfır Ping)** |
| **Hız Kaybı / Speed Drop** | %30 - %70 Düşüş | **Hız Kaybı Yok (%100 Hız)** |
| **Uzak Sunucu / Remote Server**| Var (Veriler oradan geçer) | **Yok (Cihaz içi yerel döngü)** |
| **Root Gereksinimi** | Yok | **Yok** |
| **Pil Tüketimi / Battery Impact** | Yüksek | **Çok Düşük** |

---

## 🇬🇧 English

ByePass is an open-source Android networking utility designed to bypass Deep Packet Inspection (DPI) filtering and DNS poisoning locally on the device without routing traffic to external third-party servers.

### Technical Overview
- **TLS SNI Fragmentation:** Slices initial HTTPS `ClientHello` packets at the record boundary (`0x16 0x03`). Stateless middlebox DPI engines fail to inspect the target host, letting the traffic through transparently.
- **Port 1253 DNS Resolution:** Bypasses ISP transparent UDP port 53 interception by querying dedicated DNS endpoints over alternative ports, preventing poisoned redirect responses.
- **Local Loopback:** All packet operations run strictly inside `127.0.0.1:10808` without altering your IP address or sending payloads to external proxies.

### Quick Setup
1. Download `ByePass-v1.0.0.apk` from the [Releases](https://github.com/akifkr/ByePass/releases/latest) section.
2. Install the APK on your Android device (Android 7.0+ supported).
3. Open the app and tap **"Bypass Başlat"**.
4. *(For Android 7–9)*: Configure your Wi-Fi proxy once to `127.0.0.1:10808`. *(Android 10+ routes automatically).*

---

## 🇹🇷 Türkçe

ByePass; Android cihazlarda root yetkisi gerektirmeden, trafiği harici bir sunucuya göndermeden ve internet hızında kayıp yaşamadan (0 ms ping artışı) çalışan yerel bir DPI ve DNS filtre aşma aracıdır. 

Windows tarafındaki GoodbyeDPI ve ByeDPI araçlarının Android ortamı için optimize edilmiş hafif bir alternatifidir.

### Teknik Çalışma Mantığı
- **TLS SNI Parçalama:** Giden HTTPS bağlantılarındaki ilk `ClientHello` paketini 2-byte seviyesinden böler. Operatör tarafındaki DPI cihazları paketleri birleştiremediği için alan adını tespit edemez ve bağlantıyı kesemez.
- **Port 1253 DNS Sorgusu:** Operatörlerin standart UDP 53 portundaki DNS zehirlemelerini ve BTK uyarı yönlendirmelerini aşmak için alternatif port üzerinden doğrudan gerçek IP adresini çözer.
- **Yerel Döngü:** Verileriniz asla yabancı bir VPN sunucusuna gitmez; tüm paket ayrıştırma cihazın kendi hafızasında gerçekleşir.

### Sıkça Sorulan Sorular (SSS)
- **Bu bir VPN mi?**  
  Hayır, klasik bir VPN değildir. IP adresinizi değiştirmez, yurt dışı sunucularına bağlanmaz. Bu nedenle oyunlarda veya günlük kullanımda ping artışı yaratmaz.
- **Verilerim veya şifrelerim güvende mi?**  
  Evet. ByePass trafiğin içeriğini çözmez (HTTPS şifrelemesi aynen kalır) ve hiçbir veriyi dışarıya iletmez. Kodları tamamen açık kaynaklıdır ve incelenebilir.
- **Hangi Android sürümleri destekleniyor?**  
  Android 7.0 (API 24) ve üzeri tüm sürümlerle uyumludur.

---

## 📄 License
This project is open-source and licensed under the [MIT License](LICENSE). Free for educational and network research purposes.
