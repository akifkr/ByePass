# ByePass

Rootless, zero-latency Android local DPI and DNS circumvention utility. An alternative to GoodbyeDPI and ByeDPI designed for Android environments.

[![Latest Release](https://img.shields.io/github/v/release/akifkr/ByePass?label=Release)](https://github.com/akifkr/ByePass/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android-blue)](https://github.com/akifkr/ByePass)
[![Language](https://img.shields.io/badge/Language-Kotlin-purple)](https://github.com/akifkr/ByePass)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

[Download APK](https://github.com/akifkr/ByePass/releases/latest) | [English](#english) | [Türkçe](#türkçe)

---

## 🇬🇧 English

ByePass is an open-source Android networking utility designed to bypass Deep Packet Inspection (DPI) filtering and DNS poisoning locally on the device.

Unlike conventional VPN services, ByePass does not tunnel your traffic to any external or remote servers. It does not alter your IP address or add connection latency (0 ms added ping). All packet fragmentation occurs locally via a loopback proxy interface.

### Technical Mechanism
- **TLS SNI Segmentation:** Intercepts outbound HTTPS connections and segments the initial `ClientHello` handshake at the record boundary (`0x16 0x03`). Stateless and lightweight middlebox DPI engines fail to extract the SNI domain name from fragmented segments.
- **Port 1253 DNS Resolution:** Bypasses ISP transparent UDP port 53 interception by querying dedicated DNS endpoints over alternative ports, preventing poisoned redirect responses.
- **Pure Local Loopback:** Operates on `127.0.0.1:10808` without routing payload through external proxies.

### Requirements & Setup
- Supports Android 7.0 (API 24) and higher.
- Download `ByePass-v1.0.0.apk` from the Releases section and install it.
- Launch the application and tap **"Bypass Başlat"**.
- *Note for Android 7–9:* Configure manual Wi-Fi proxy to `127.0.0.1:10808` once. On Android 10+, proxy configuration is handled automatically by the system.

---

##🇹🇷Türkçe

ByePass; Android cihazlarda root yetkisi gerektirmeden, trafiği üçüncü parti bir sunucuya göndermeden ve internet hızında kayba yol açmadan (0 ms ping artışı) çalışan bir yerel DPI ve DNS filtre aşma aracıdır.

GoodbyeDPI ve ByeDPI araçlarının Android için tasarlanmış hafif bir alternatifidir.

### Teknik Çalışma Mantığı
- **TLS SNI Parçalama:** Giden HTTPS bağlantılarındaki ilk `ClientHello` paketini standart `0x16 0x03` sınırından böler. Operatörlerin derin paket inceleme (DPI) cihazları akış birleştirmesi yapamadığı için alan adını (SNI) tespit edemez.
- **Port 1253 DNS Sorgusu:** Operatörlerin standart 53 numaralı UDP portundaki DNS zehirleme ve yönlendirmelerini aşmak için alternatif port üzerinden sorgu göndererek gerçek IP adresini çözer.
- **Yerel Döngü:** Trafiği harici bir VPN sunucusuna aktarmaz; tüm işlemler cihazın kendi hafızasında (`127.0.0.1:10808`) gerçekleşir.

### Kurulum ve Kullanım
- Android 7.0 (API 24) ve üzeri sürümlerle uyumludur.
- Releases sekmesinden güncel `.apk` dosyasını indirip kurun.
- Uygulamayı açarak **"Bypass Başlat"** butonuna basın.
- *Android 7–9 kullanıcıları için:* Wi-Fi ayarlarından proxy adresini bir kez `127.0.0.1:10808` olarak ayarlayın. Android 10 ve üzeri sürümlerde bu işlem sistem tarafından otomatik olarak yönetilir.

---

## License
Distributed under the MIT License. Developed for educational and network research purposes.
