<div align="center">

# ByePass

**Rootless, zero-latency Android local DPI & DNS circumvention engine.**  
*GoodbyeDPI / ByeDPI alternative for Android devices.*

[![Release](https://img.shields.io/github/v/release/akifkr/ByePass?color=2ea44f&label=Latest%20Release)](https://github.com/akifkr/ByePass/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-blue?logo=android&logoColor=white)](https://github.com/akifkr/ByePass)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%20100%25-7F52FF?logo=kotlin&logoColor=white)](https://github.com/akifkr/ByePass)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

[Download Latest APK](https://github.com/akifkr/ByePass/releases/latest) • [English](#-english) • [Türkçe](#-türkçe)

---

</div>

### Architecture & Traffic Flow

```
[ Browser / App ]
       │
       ▼ (Port 10808)
[ ByePass Local Loopback ]
       ├── 1. TLS ClientHello ──► Segmented at 2-byte boundary (0x16 0x03)
       └── 2. Domain Lookup   ──► Dedicated Port 1253 (No UDP 53 Poisoning)
       │
       ▼ (Direct Socket)
[ Remote Destination Server ] (0 ms Added Latency)
```

---

## 🇬🇧 English

ByePass is an open-source Android networking utility designed to circumvent Deep Packet Inspection (DPI) censorship and ISP DNS poisoning entirely on the local device.

Unlike traditional VPNs, **ByePass does not route your traffic through external servers**. It operates with **0 ms added latency** (no ping penalty) by intercepting and fragmenting handshake packets locally on `127.0.0.1:10808`.

### Key Capabilities
- **Zero Added Latency:** Preserves native peer-to-peer connection speed; no remote proxies or bandwidth bottlenecks.
- **TLS SNI Segmentation:** Divides the TLS `ClientHello` payload right after the record header (`0x16 0x03`). Middleboxes cannot parse fragmented segments and fail to enforce SNI blocks.
- **Port 1253 DNS Resolution:** Queries unfiltered DNS servers over alternative ports, bypassing transparent UDP port 53 redirection used by ISPs.
- **Lightweight Compose UI:** Modern, dark-mode native Android interface with real-time packet event logging.
- **Broad Compatibility:** Operates smoothly across Android 7.0 (API 24) through the latest Android versions.

### Installation & Usage
1. Grab the latest `ByePass-v1.0.0.apk` from the [Releases](https://github.com/akifkr/ByePass/releases/latest) tab.
2. Install the APK on your Android device.
3. Open ByePass and tap **"Bypass Başlat"**.
4. *(Android 7–9 only):* Set your Wi-Fi manual proxy to `127.0.0.1:10808` once. On Android 10+, proxy routing is handled by the system automatically.

---

## 🇹🇷 Türkçe

ByePass; Android cihazlarda root iznine ihtiyaç duymadan, internet hızını yavaşlatmadan ve ping eklemeden (0 ms gecikme) DPI (Derin Paket İncelemesi) ve DNS engellerini aşan açık kaynaklı bir yerel ağ aracıdır.

Bilgisayarlarda kullanılan GoodbyeDPI ve ByeDPI araçlarının Android ortamı için optimize edilmiş hafif bir alternatifidir.

### Öne Çıkan Nitelikler
- **Sıfır Ping & Tam Hız:** Trafiği harici bir VPN sunucusuna yönlendirmez. Operatörünüzün sağladığı orijinal internet hızı ve ping değeri korunur.
- **TLS SNI Parçalama:** Giden HTTPS el sıkışma paketlerini ilk 2 byte sınırından böler. Operatör tarafındaki DPI donanımları akışı birleştiremediği için alan adını (SNI) tespit edemez.
- **Port 1253 DNS Çözümü:** Standart UDP 53 portundaki yönlendirme ve zehirlemeleri aşmak için alternatif port üzerinden doğrudan gerçek IP adresini çözer (Discord vb. servisler için optimize edilmiştir).
- **Yerel Döngü Güvenliği:** Tüm işlemler cihazın kendi hafızasında (`127.0.0.1:10808`) gerçekleşir; verileriniz hiçbir üçüncü şahıs sunucuya aktarılmaz.

### Kurulum ve Kullanım Rehberi
1. [Releases (Sürümler)](https://github.com/akifkr/ByePass/releases/latest) sayfasından en güncel `.apk` dosyasını indirin.
2. Cihazınıza kurup uygulamayı açın ve **"Bypass Başlat"** butonuna dokunun.
3. *(Android 7–9 kullananlar için):* Wi-Fi proxy ayarını bir defaya mahsus `127.0.0.1:10808` yapın. Android 10 ve daha güncel telefonlarda bu işlem otomatik olarak gerçekleşir.

---

## ⚖️ License
Distributed under the [MIT License](https://opensource.org/licenses/MIT). Developed for educational, research, and privacy purposes.
