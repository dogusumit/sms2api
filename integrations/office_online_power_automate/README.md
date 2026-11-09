# Microsoft Office Online (Excel Online) Entegrasyonu

Bu dizin, **SMS2API** Android uygulamasının yakaladığı SMS'leri gerçek zamanlı ve sunucusuz (serverless) olarak **Microsoft Office 365 (Excel Online)** çalışma tablosuna aktarmak için gereken tüm bileşenleri, şemaları, formülleri ve kurulum adımlarını içerir.

---

## 🎯 Özellikler ve Avantajlar

* **Tamamen Sunucusuz (Zero-Server):** Harici bir backend sunucu, VPS veya veritabanı kurma ihtiyacı yoktur. Doğrudan Microsoft 365 bulut altyapısı kullanılır.
* **Excel Online Canlı Senkronizasyon:** Telefona gelen her SMS saniyeler içerisinde Excel çalışma kitabında yeni bir satıra dönüşür.
* **Kesintisiz Teslimat Güvencesi (At-Least-Once Delivery):** Android uygulaması Room veritabanı kuyruğu ve `WorkManager` sayesinde, internet kesilse bile bağlantı geldiğinde iletilmeyen tüm SMS'leri sırayla Excel'e aktarır.
* **Doğrulanmış SHA-256 Kimliği:** Her SMS için `message_id` sütunu sayesinde yinelenen kayıtlar engellenir veya filtrelenebilir.
* **Otomatik Tarih Formatlama:** Unix zaman damgası doğrudan Türkiye saatine (UTC+3) formatlanarak kaydedilir.
* **Kurumsal Güvenlik:** Microsoft SAS token korumalı uç nokta ve isteğe bağlı `X-API-Key` özel başlık (Custom Header) doğrulaması.

---

## 📂 Dizin İçeriği

```
integrations/office_online_power_automate/
├── README.md                               # Bu dokümantasyon
├── excel_template/
│   ├── SMS_Arsivi_Sablon.csv               # Örnek sütun başlıkları ve veri içeren CSV şablonu
│   └── SetupTable_OfficeScript.ts          # Excel Online'da tabloyu 1 tıkla oluşturan Office Betiği
├── schemas/
│   ├── sms_request_schema.json             # Power Automate HTTP tetikleyicisi JSON Şeması
│   └── sample_payload.json                 # Test için örnek veri yükü
└── flow/
    ├── power_automate_guide.md             # Adım adım görsel akış kurulum rehberi
    └── flow_expressions.txt                # Kopyala-yapıştır Power Automate ifadeleri/formülleri
```

---

## ⚡ Hızlı Başlangıç (3 Adımda Kurulum)

### Adım 1: Excel Online Çalışma Kitabını Hazırlama
1. [office.com](https://www.office.com) veya OneDrive/SharePoint hesabınıza giriş yapın.
2. Yeni bir Excel Çalışma Kitabı oluşturun ve adını `SMS_Arsivi.xlsx` koyun.
3. Tabloyu oluşturmak için iki seçenekten birini kullanın:
   - **Seçenek A (Kolay):** İlk satıra (A1:G1) şu başlıkları yazın ve ardından tüm satırı seçip **Tablo Olarak Biçimlendir** deyin, tablo adını `SmsTable` yapın:
     `MessageID` | `Sender` | `Body` | `ReceivedDate` | `Timestamp` | `OTPCode` | `Status`
   - **Seçenek B (Otomatik - Office Script):** Üst menüden **Otomatikleştir** > **Yeni Betik** deyin, [`SetupTable_OfficeScript.ts`](excel_template/SetupTable_OfficeScript.ts) kodunu yapıştırıp **Çalıştır**'a basın. Tablo tüm sütun genişlikleriyle otomatik hazır olacaktır.

### Adım 2: Power Automate Akışını Oluşturma
1. [make.powerautomate.com](https://make.powerautomate.com) adresine gidin.
2. **Oluştur** > **Anında bulut akışı** seçin.
3. Tetikleyici olarak **Bir HTTP isteği alındığında** (When an HTTP request is received) seçin.
4. İstek JSON Şeması alanına [`schemas/sms_request_schema.json`](schemas/sms_request_schema.json) içeriğini yapıştırın.
5. **Excel Online (Business)** bağlayıcısından **Tabloya satır ekle** eylemini seçip `SMS_Arsivi.xlsx` dosyanızı ve `SmsTable` tablonuzu seçin.
6. Ayrıntılı formüller ve yanıt eylemi için [`flow/power_automate_guide.md`](flow/power_automate_guide.md) kılavuzunu takip edin.
7. Akışı kaydedip oluşturulan **HTTP POST URL** değerini kopyalayın.

### Adım 3: Android SMS2API Uygulamasına Bağlama
1. Telefonda **SMS2API** uygulamasını açın.
2. **Ayarlar (Settings)** ekranına gelin.
3. **Webhook Endpoint URL** alanına Power Automate'den kopyaladığınız URL'yi yapıştırın.
4. **Test Connection** butonuna basın.
5. Excel Online dosyanızı kontrol edin; test satırının anında eklendiğini göreceksiniz!
