<div align="center">
  <img src="assets/logo.png" alt="ByePass" width="200">
</div>

# ByePass

**Root gerektirmeyen, Android için yerel DPI/DNS engel aşma motoru.**
*GoodbyeDPI / ByeDPI mantığının Android'e taşınmış hali.*

[![Release](https://img.shields.io/github/v/release/akifkr/ByePass?color=2ea44f&label=Latest%20Release)](https://github.com/akifkr/ByePass/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-blue?logo=android&logoColor=white)](https://github.com/akifkr/ByePass)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%20100%25-7F52FF?logo=kotlin&logoColor=white)](https://github.com/akifkr/ByePass)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![VirusTotal](https://img.shields.io/badge/VirusTotal-0%2F67%20Clean-brightgreen?logo=virustotal&logoColor=white)](https://www.virustotal.com/gui/file/44838f7fcffa676883ebab11a8f8d053364c84e8846401cfc82213b62f706f82)

[İndir](https://github.com/akifkr/ByePass/releases/latest) • [VirusTotal Taraması](https://www.virustotal.com/gui/file/cdc72c586448ccb7b2b1017ad38c0a1dbc09c630c0661d0c2cd5b19520092572) • [English](#english) • [Türkçe](#türkçe)

</div>

---

## Nasıl çalışıyor

Uygulama cihaz üzerinde `127.0.0.1:10808`'de bir yerel proxy açıyor ve Android 10+'ta bunu sistem proxy'si olarak otomatik ayarlıyor. Dışarıya trafiği bir sunucudan geçirmiyor; sadece cihaz üzerinde, giden bağlantının ilk paketlerini biraz oynayarak DPI'nin işini zorlaştırıyor:

- **HTTPS (CONNECT üzerinden):** TLS ClientHello paketini kayıt başlığından sonra ikiye bölüp iki ayrı TCP yazımı olarak gönderiyor. Tek paketi inceleyip SNI'ya bakan DPI cihazları hedef alan adını tam göremiyor.
- **Düz HTTP:** `Host:` başlığını `hOSt:` yapıp isteği request-line ile Host header'ı ayrı TCP segmentlerine bölerek gönderiyor. Basit string-match yapan filtrelerin çoğu bunu yakalamıyor.
- **DNS:** Sistem/operatör DNS'i yerine doğrudan 77.88.8.8'e (Yandex) standart 53 portundan sorgu atıyor, transaction ID'yi doğruluyor. Amaç DNS zehirlemesini/yönlendirmesini atlamak — bunun sırrı port değil, operatörün araya girdiği resolver'ı hiç kullanmamak.

Kapsam dışı: ham UDP trafiği (Discord sesli sohbet, bazı oyunların UDP paketleri) proxy'den geçmiyor, sadece TCP tabanlı web/HTTPS trafiği kapsanıyor.

---

<a name="english"></a>
## English

ByePass is a small Android networking tool that fragments the outgoing TLS handshake and rewrites plain-HTTP headers locally, to make life harder for DPI-based blocking — without routing your traffic through a remote server. It also resolves DNS directly against a chosen resolver instead of whatever your carrier hands you, since carrier resolvers are a common place for blocking to happen.

**What it does:**
- Splits the TLS ClientHello right after the record header, across two TCP writes.
- Mutates the `Host:` header (`Host:` → `hOSt:`) and fragments plain-HTTP requests the same way.
- Resolves domains against 77.88.8.8 over standard DNS (port 53) instead of the carrier's resolver, with transaction-ID validation to reject spoofed replies.
- Runs entirely on-device — no remote VPN hop, so no added latency from that.

**What it doesn't do:**
- No UDP proxying (voice chat, some game traffic won't be routed).
- No protection against DPI that does full TCP stream reassembly — this targets middleboxes that only look at the first packet or two.

**Install:**
1. Grab the APK from [Releases](https://github.com/akifkr/ByePass/releases/latest).
2. Install, open the app, tap "Bypass Başlat".
3. Android 7–9: set your Wi-Fi proxy manually to `127.0.0.1:10808`. Android 10+ does this for you.

---

<a name="türkçe"></a>
## Türkçe

ByePass, root istemeden, TLS el sıkışmasını ve düz HTTP başlıklarını cihaz üzerinde parçalayıp değiştirerek DPI tabanlı engellemeleri zorlaştırmayı hedefleyen küçük bir Android aracı. Trafiği uzak bir sunucuya yönlendirmiyor, bu yüzden ek bir VPN gecikmesi eklemiyor. DNS tarafında da operatörün verdiği resolver yerine seçtiğin bir sunucuya doğrudan sorgu atıyor — çünkü engelleme genelde tam da operatör resolver'ında oluyor.

**Ne yapıyor:**
- TLS ClientHello'yu kayıt başlığından hemen sonra ikiye bölüp iki ayrı TCP yazımı olarak gönderiyor.
- `Host:` başlığını `hOSt:` yapıp düz HTTP isteklerini de benzer şekilde parçalıyor.
- 77.88.8.8'e standart 53 portundan DNS sorgusu atıyor, transaction ID kontrolü ile sahte cevapları eliyor.
- Tamamen cihaz üzerinde çalışıyor, harici sunucu yok.

**Ne yapmıyor:**
- UDP trafiğini proxy'lemiyor (sesli sohbet, bazı oyun paketleri bundan geçmiyor).
- Tam TCP akış birleştirmesi yapan gelişmiş DPI'lara karşı garanti vermiyor; hedef, sadece ilk bir-iki paketi inceleyen orta katman cihazlar.

**Kurulum:**
1. [Releases](https://github.com/akifkr/ByePass/releases/latest) sayfasından apk'yı indir.
2. Kur, aç, "Bypass Başlat"a bas.
3. Android 7–9 kullananlar Wi-Fi proxy ayarını manuel olarak `127.0.0.1:10808` yapsın. Android 10+'ta bu otomatik.

---

## Lisans
[MIT](https://opensource.org/licenses/MIT). Araştırma, test ve eğitim amaçlı geliştirildi.

---

