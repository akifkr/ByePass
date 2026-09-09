<div align="center">

# ByePass

**Rootless, zero-overhead Android local DPI & DNS circumvention engine.**  
*GoodbyeDPI / ByeDPI alternative for Android devices.*

[![Release](https://img.shields.io/github/v/release/akifkr/ByePass?color=2ea44f&label=Latest%20Release)](https://github.com/akifkr/ByePass/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-blue?logo=android&logoColor=white)](https://github.com/akifkr/ByePass)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%20100%25-7F52FF?logo=kotlin&logoColor=white)](https://github.com/akifkr/ByePass)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![VirusTotal](https://img.shields.io/badge/VirusTotal-0%2F67%20Clean-brightgreen?logo=virustotal&logoColor=white)](https://www.virustotal.com/gui/file/acaf39f580767172edfcdc0aa5e22923647a0bc2ea54c8a6c36bfcfafc7a1265/)

[Download Latest APK](https://github.com/akifkr/ByePass/releases/latest) • [English](#-english) • [Türkçe](#-türkçe)

---

</div>

### Architecture & Traffic Flow

```text
[ Browser / App ]
       │
       ▼ (Port 10808 Loopback)
[ ByePass Engine ]
       ├── 1. TLS ClientHello ──► Segmented at 2-byte boundary (0x16 0x03)
       ├── 2. Plain HTTP      ──► Case Mutation (hOSt:) & Fragmentation
       └── 3. Domain Lookup   ──► Dedicated Port 1253 (No UDP 53 Poisoning)
       │
       ▼ (Direct Native Sockets)
[ Remote Destination Server ] (Preserves Carrier Native Ping)
```

---

## 🇬🇧 English

ByePass is an open-source Android networking utility designed to circumvent Deep Packet Inspection (DPI) censorship and ISP DNS poisoning directly on the local device.

Unlike traditional VPNs, **ByePass does not route your traffic through external proxy servers**. It operates with **zero remote server latency** by segmenting handshake packets locally on `127.0.0.1:10808`.

### Key Capabilities
- **Direct Carrier Speed:** No remote VPN routing or bandwidth throttling; preserves native mobile connection ping and speed.
- **TLS SNI Segmentation:** Divides the TLS `ClientHello` payload right after the record header (`0x16 0x03`). DPI engines without TCP reassembly capabilities fail to enforce SNI blocks.
- **HTTP Header Mutation:** Intercepts plain HTTP requests and mutates the `Host:` header into `hOSt:` to bypass simple middlebox filters.
- **Port 1253 Alternative DNS:** Bypasses ISP UDP 53 redirection/poisoning by querying upstream DNS over alternative ports.
- **Scope & Limitations:** Focuses on TCP-based Web, HTTPS, and application traffic. *Raw UDP traffic (such as Discord Voice WebRTC channels or mobile game UDP packets) is not proxied.*

### Installation & Usage
1. Download the latest `ByePass.apk` from the [Releases](https://github.com/akifkr/ByePass/releases/latest) section.
2. Install and launch the app, then tap **"Bypass Başlat"**.
3. *(Android 7–9 only):* Set your Wi-Fi manual proxy to `127.0.0.1:10808`. On Android 10+, system proxy configuration is applied automatically.

---

## 🇹🇷 Türkçe

ByePass; Android cihazlarda root yetkisine ihtiyaç duymadan, internet hızını yavaşlatmadan ve harici sunucu gecikmesi (VPN pingi) eklemeden DPI (Derin Paket İncelemesi) ve DNS engellerini aşan açık kaynaklı bir yerel ağ aracıdır.

Bilgisayarlarda yaygın olarak kullanılan GoodbyeDPI ve ByeDPI araçlarının mantığını Android ortamına taşıyan hafif ve verimli bir alternatiftir.

### Öne Çıkan Nitelikler
- **Harici Sunucusuz / Saf Hat Pingi:** Trafiği uzak bir VPN sunucusuna yönlendirmez. Operatörünüzün orijinal hız ve ping değerleri korunur.
- **TLS SNI Parçalama:** Giden HTTPS el sıkışma paketlerini ilk 2 baytlık sınırından böler. Operatör tarafındaki DPI cihazları akışı birleştiremediği için hedef alan adını (SNI) tespit edemez.
- **Düz HTTP Mutasyonu:** Düz metin HTTP bağlantılarında `Host:` başlığını `hOSt:` şeklinde modifiye ederek filtreleri atlatır.
- **Port 1253 DNS Çözümü:** Standart UDP 53 portundaki yönlendirme ve zehirlemeleri aşmak için alternatif port üzerinden güvenli DNS çözümü yapar.
- **Kapsam ve İstisnalar:** Web tarayıcıları, sosyal medya uygulamaları ve TCP/HTTPS tabanlı bağlantıları kapsar. *Discord ses kanalları (WebRTC/UDP) ve doğrudan UDP kullanan çevrim içi mobil oyun paketleri proxy kapsamı dışındadır.*

### Kurulum ve Kullanım Rehberi
1. [Releases](https://github.com/akifkr/ByePass/releases/latest) sayfasından en güncel `.apk` dosyasını indirin ve kurun.
2. Uygulamayı açıp **"Bypass Başlat"** butonuna dokunun.
3. *(Android 7–9 kullananlar için):* Wi-Fi proxy ayarını bir defaya mahsus `127.0.0.1:10808` yapın. Android 10 ve üzeri cihazlarda bu yönlendirme sistem tarafından otomatik yönetilir.

---

## ⚖️ License
Distributed under the [MIT License](https://opensource.org/licenses/MIT). Developed for research, testing, and educational purposes.
