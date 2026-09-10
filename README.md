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

[İndir](https://github.com/akifkr/ByePass/releases/latest) • [VirusTotal Taraması](https://www.virustotal.com/gui/file/44838f7fcffa676883ebab11a8f8d053364c84e8846401cfc82213b62f706f82) • [English](#english) • [Türkçe](#türkçe)

---

## Nasıl çalışıyor

Uygulama cihaz üzerinde `127.0.0.1:10808`'de bir yerel proxy açıyor ve Android 10+'ta bunu sistem proxy'si olarak otomatik ayarlıyor. Dışarıya trafiği bir sunucudan geçirmiyor; sadece cihaz üzerinde, giden bağlantının ilk paketlerini biraz oynayarak DPI'nin işini zorlaştırıyor:

- **HTTPS (CONNECT üzerinden):** TLS ClientHello paketini kayıt başlığından sonra ikiye bölüp iki ayrı TCP yazımı olarak gönderiyor. Tek paketi inceleyip SNI'ya bakan DPI cihazları hedef alan adını tam göremiyor.
- **Düz HTTP:** `Host:` başlığını `hOSt:` yapıp isteği request-line ile Host header'ı ayrı TCP segmentlerine bölerek gönderiyor. Basit string-match yapan filtrelerin çoğu bunu yakalamıyor.
- **DNS:** Yandex'in 77.88.8.8 sunucusuna, standart 53 yerine 1253 portundan soruyor. Bu port rastgele değil — Yandex bu servisi tam olarak bazı operatörlerin hedefe bakmadan UDP 53'e giden her şeyi yakalayıp zehirlediği (poisoning) senaryoyu atlatmak için ayrıca çalıştırıyor (GoodbyeDPI-Turkey fork'unun da kullandığı yöntem bu). Cevaplar transaction ID ile doğrulanıyor; Yandex'e ulaşılamazsa sistem DNS'ine düşülüyor ve bu durum uygulama günlüğünde ayrıca belirtiliyor.

Kapsam dışı: ham UDP trafiği (Discord sesli sohbet, bazı oyunların UDP paketleri) proxy'den geçmiyor, sadece TCP tabanlı web/HTTPS trafiği kapsanıyor.

---

<a name="english"></a>
## English

ByePass is a small Android networking tool that fragments the outgoing TLS handshake and rewrites plain-HTTP headers locally, to make life harder for DPI-based blocking — without routing your traffic through a remote server. It also resolves DNS directly against a chosen resolver instead of whatever your carrier hands you, since carrier resolvers are a common place for blocking to happen.

**What it does:**
- Splits the TLS ClientHello right after the record header, across two TCP writes.
- Mutates the `Host:` header (`Host:` → `hOSt:`) and fragments plain-HTTP requests the same way.
- Queries Yandex's 77.88.8.8 over port 1253 instead of the standard 53 — Yandex runs this specifically so it can't be swept up by ISPs that transparently hijack/poison anything sent to UDP 53 regardless of destination (the same trick used by the GoodbyeDPI-Turkey fork). Replies are checked against the query's transaction ID; if Yandex is unreachable it falls back to the system resolver and logs that fact.
- Runs entirely on-device — no remote VPN hop, so no added latency from that.

**What it doesn't do:**
- No UDP proxying (voice chat, some game traffic won't be routed).
- No protection against DPI that does full TCP stream reassembly — this targets middleboxes that only look at the first packet or two.

**Install:**
1. Grab the APK from [Releases](https://github.com/akifkr/ByePass/releases/latest).
2. Install, open the app, tap "Bypass Başlat".
3. Android 7–9: set your Wi-Fi proxy manually to `127.0.0.1:10808`. Android 10+ does this for you automatically — and reverts it automatically too when you stop the tunnel.
4. Android 7–9 only: when you're done, remove the manual Wi-Fi proxy setting yourself. Unlike Android 10+, it won't revert on its own, and leaving it set once the app is closed will block your internet access.

---

<a name="türkçe"></a>
## Türkçe

ByePass, root istemeden, TLS el sıkışmasını ve düz HTTP başlıklarını cihaz üzerinde parçalayıp değiştirerek DPI tabanlı engellemeleri zorlaştırmayı hedefleyen küçük bir Android aracı. Trafiği uzak bir sunucuya yönlendirmiyor, bu yüzden ek bir VPN gecikmesi eklemiyor. DNS tarafında da operatörün verdiği resolver yerine seçtiğin bir sunucuya doğrudan sorgu atıyor — çünkü engelleme genelde tam da operatör resolver'ında oluyor.

**Ne yapıyor:**
- TLS ClientHello'yu kayıt başlığından hemen sonra ikiye bölüp iki ayrı TCP yazımı olarak gönderiyor.
- `Host:` başlığını `hOSt:` yapıp düz HTTP isteklerini de benzer şekilde parçalıyor.
- Yandex'in 77.88.8.8 sunucusuna standart 53 yerine 1253 portundan soruyor — bu, bazı operatörlerin UDP 53'e giden trafiği hedefe bakmadan yakalayıp zehirlemesini atlatmak için Yandex'in kasıtlı olarak açtığı bir alternatif; GoodbyeDPI-Turkey fork'unun da kullandığı yöntem bu. Transaction ID kontrolü ile sahte cevaplar eleniyor; Yandex'e ulaşılamazsa sistem DNS'ine düşülüyor ve bu durum günlükte ayrıca belirtiliyor.
- Tamamen cihaz üzerinde çalışıyor, harici sunucu yok.

**Ne yapmıyor:**
- UDP trafiğini proxy'lemiyor (sesli sohbet, bazı oyun paketleri bundan geçmiyor).
- Tam TCP akış birleştirmesi yapan gelişmiş DPI'lara karşı garanti vermiyor; hedef, sadece ilk bir-iki paketi inceleyen orta katman cihazlar.

**Kurulum:**
1. [Releases](https://github.com/akifkr/ByePass/releases/latest) sayfasından apk'yı indir.
2. Kur, aç, "Bypass Başlat"a bas.
3. Android 7–9 kullananlar Wi-Fi proxy ayarını manuel olarak `127.0.0.1:10808` yapsın. Android 10+'ta bu otomatik yapılır — tüneli durdurunca da otomatik geri alınır.
4. Sadece Android 7–9: işiniz bitince manuel Wi-Fi proxy ayarını **elle kaldırın**. Android 10+'ın aksine bu kendiliğinden geri dönmez; uygulamayı kapattıktan sonra ayarı öylece bırakırsanız internetiniz kesilir.

---

## Geri bildirim / sorun bildirme

Bir site açılmıyorsa, uygulama çöküyorsa ya da beklenmedik bir davranış görüyorsanız lütfen [Issues](https://github.com/akifkr/ByePass/issues) sekmesinden bildirin. Mümkünse Android sürümünüzü, operatörünüzü ve uygulama içindeki günlük (log) çıktısını da eklemeniz sorunu çözmemi çok kolaylaştırır.

---

## Lisans
[MIT](https://opensource.org/licenses/MIT). Araştırma, test ve eğitim amaçlı geliştirildi.

