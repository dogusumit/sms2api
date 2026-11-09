# Power Automate Akışı (Cloud Flow) Kurulum Kılavuzu

Bu kılavuz, **SMS2API** Android uygulamasından gelen SMS bildirimlerini yakalayarak **Microsoft Office Excel Online** çalışma kitabına anlık olarak satır ekleyen bir Power Automate akışının nasıl kurulacağını adım adım anlatır.

---

## 🏗️ Mimari Akış Şeması

```
[Android Telefon]
      │
      │ HTTP POST (JSON Payload)
      ▼
┌─────────────────────────────────────────────────────────────┐
│ Power Automate: Bir HTTP isteği alındığında (Trigger)        │
└─────────────────────────────────────────────────────────────┘
      │
      │ 1. Zaman damgası dönüştürme (Timestamp -> Türkiye Saati)
      ▼
┌─────────────────────────────────────────────────────────────┐
│ Excel Online (Business): Tabloya satır ekle (Add row)       │
│ Dosya: SMS_Arsivi.xlsx | Tablo: SmsTable                    │
└─────────────────────────────────────────────────────────────┘
      │
      │ 2. Başarılı yazma teyidi
      ▼
┌─────────────────────────────────────────────────────────────┐
│ HTTP Yanıt: 200 OK                                          │
│ {"status": "success", "message_id": "..."}                  │
└─────────────────────────────────────────────────────────────┘
      │
      ▼
[Android Telefon: Teslimat Başarılı (Status: SUCCESS)]
```

---

## 📋 Adım Adım Kurulum

### 1. Power Automate Portalına Giriş
1. Tarayıcınızda [make.powerautomate.com](https://make.powerautomate.com) adresine gidin ve Microsoft 365 kurumsal veya bireysel hesabınızla giriş yapın.
2. Sol menüden **Oluştur** (Create) sekmesine tıklayın.
3. **Anında bulut akışı** (Instant cloud flow) seçeneğine tıklayın.
4. Çıkan pencerede:
   - **Akış adı** (Flow name): `SMS2API_Excel_Aktarici`
   - **Tetikleyici seçin**: Listeden **Bir HTTP isteği alındığında** (When an HTTP request is received) seçeneğini işaretleyin.
   - **Oluştur** (Create) butonuna tıklayın.

---

### 2. HTTP Tetikleyicisini Yapılandırma
1. **Bir HTTP isteği alındığında** kartına tıklayın.
2. **Kimler tetikleyebilir?** (Who can trigger the flow):
   - `Kiracımdaki herkes` veya `Herhangi biri` (Anyone) olarak seçin (Android telefonun erişebilmesi için *Herhangi biri* önerilir).
3. **İstek Gövdesi JSON Şeması** (Request Body JSON Schema) alanına tıklayın.
4. Aşağıdaki şemayı doğrudan yapıştırın:

```json
{
  "type": "object",
  "properties": {
    "message_id": {
      "type": "string"
    },
    "sender": {
      "type": "string"
    },
    "body": {
      "type": "string"
    },
    "timestamp": {
      "type": "integer"
    }
  },
  "required": [
    "message_id",
    "sender",
    "body",
    "timestamp"
  ]
}
```

5. **Gelişmiş parametreleri göster** (Show advanced options) seçeneğine tıklayarak **Yöntem** (Method) olarak `POST` seçildiğinden emin olun.

---

### 3. Excel Online Eylemini Ekleme
1. Tetikleyicinin altındaki **+ Yeni Adım** (+ New step) butonuna tıklayın.
2. Arama kutusuna `Excel Online` yazın ve **Excel Online (Business)** bağlayıcısını seçin.
3. Eylemler listesinden **Tabloya satır ekle** (Add a row into a table) seçeneğini seçin.
4. Alanları şu şekilde doldurun:
   - **Konum** (Location): `OneDrive for Business` (veya dosyanız SharePoint'te ise ilgili SharePoint sitesi).
   - **Belge Kitaplığı** (Document Library): `OneDrive`.
   - **Dosya** (File): Klasör simgesine tıklayıp hazırladığınız `SMS_Arsivi.xlsx` dosyasını seçin.
   - **Tablo** (Table): Açılır listeden `SmsTable` tablosunu seçin.

5. Tablo seçildikten sonra Excel sütunlarınız otomatik olarak dinamik alanlar şeklinde belirecektir. Bu alanları şu şekilde eşleştirin:

| Sütun Adı | Dinamik Değer / İfade (Expression) | Açıklama |
|---|---|---|
| **MessageID** | `triggerBody()?['message_id']` | Dinamik içerikten `message_id` seçin |
| **Sender** | `triggerBody()?['sender']` | Dinamik içerikten `sender` seçin |
| **Body** | `triggerBody()?['body']` | Dinamik içerikten `body` seçin |
| **ReceivedDate** | `convertFromUtc(addSeconds('1970-01-01T00:00:00Z', div(triggerBody()?['timestamp'], 1000)), 'Turkey Standard Time', 'yyyy-MM-dd HH:mm:ss')` | **İfade (Expression)** sekmesine yapıştırın |
| **Timestamp** | `triggerBody()?['timestamp']` | Dinamik içerikten `timestamp` seçin |
| **OTPCode** | Boş bırakabilir veya formülle kod çekebilirsiniz | *(Opsiyonel)* |
| **Status** | `KAYDEDILDI` | Sabit metin |

> [!TIP]
> **ReceivedDate** alanına formül eklerken: Alanın içine tıklayın, sağda açılan kutudan **İfade** (Expression) sekmesini açın ve formülü oraya yapıştırıp **Ekle** (Add) deyin.

---

### 4. HTTP Yanıtı (Response) Eylemi Ekleme
Excel'e kayıt başarıyla yapıldıktan sonra Android uygulamasına işlemin tamamlandığını bildirmek ve Android tarafında SMS durumunun anında `SUCCESS` olmasını sağlamak için yanıt eylemi ekleyin:

1. Excel eyleminin altına **+ Yeni Adım** deyin.
2. Arama kutusuna `Yanıt` (Response) yazın ve **İstek** (Request) bağlayıcısı altındaki **Yanıt** (Response) eylemini seçin.
3. Parametreleri doldurun:
   - **Durum Kodu** (Status Code): `200`
   - **Üstbilgiler** (Headers):
     - Anahtar: `Content-Type`
     - Değer: `application/json`
   - **Gövde** (Body):
     ```json
     {
       "status": "success",
       "message_id": "@{triggerBody()?['message_id']}",
       "received_at": "@{utcNow()}"
     }
     ```

---

### 5. Kaydetme ve Tetikleyici URL'sini Alma
1. Sağ üstteki **Kaydet** (Save) butonuna tıklayın.
2. Kaydetme tamamlandıktan sonra en üstteki **Bir HTTP isteği alındığında** kutusuna tekrar tıklayın.
3. **HTTP POST URL** alanında Microsoft tarafından otomatik üretilen uzun bir URL göreceksiniz:
   ```
   https://prod-xx.westeurope.logic.azure.com:443/workflows/.../triggers/manual/paths/invoke?...
   ```
4. Yanındaki **Kopyala** simgesine tıklayarak bu URL'yi kopyalayın.

---

## 📱 Android Uygulaması (SMS2API) Yapılandırması

1. Android telefonunuzda **SMS2API** uygulamasını açın.
2. Alt menüden **Ayarlar** (Settings) sekmesine geçin.
3. **Target REST API Configuration** bölümüne gidin:
   - **Webhook Endpoint URL**: Power Automate'den kopyaladığınız uzun HTTP POST URL'sini yapıştırın.
   - **Kimlik Doğrulama** (Authentication): `None` (Power Automate URL'si zaten güvenli `sig` parametresi ile imzalıdır).
4. **Test Connection** (Bağlantıyı Sına) butonuna dokunun.
5. Bildirim alanında yeşil onay göreceksiniz. Excel Online çalışma sayfanızı yenileyin; test satırının eklendiğini göreceksiniz!
