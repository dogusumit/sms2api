# Google E-Tablolar (Google Sheets) Kurulum Kılavuzu

Bu kılavuz, **SMS2API** Android uygulamasından gelen SMS'leri hiçbir harici sunucuya veya ücretli lisansa ihtiyaç duymadan doğrudan **Google E-Tablolar (Google Sheets)** tablonuza anlık olarak kaydetmeyi adım adım açıklar.

> [!NOTE]
> Bu yöntem **%100 ücretsizdir** ve standart bir `@gmail.com` kişisel Google hesabı ile de tam yetkiyle çalışır.

---

## 🏗️ Mimari Şema

```
[Android Telefon]
      │
      │ HTTP POST (JSON Payload)
      ▼
┌─────────────────────────────────────────────────────────────┐
│ Google Apps Script Web App (doPost)                         │
│ URL: https://script.google.com/macros/s/.../exec            │
└─────────────────────────────────────────────────────────────┘
      │
      │ 1. Mükerrer Kontrolü (message_id SHA-256)
      │ 2. Tarih Dönüştürme (GMT+3 Türkiye Saati)
      │ 3. OTP / Şifre Ayıklama
      ▼
┌─────────────────────────────────────────────────────────────┐
│ Google Sheets (E-Tablo: SMS_Gelen_Kutusu)                   │
│ Satır Ekleme (appendRow)                                    │
└─────────────────────────────────────────────────────────────┘
      │
      │ HTTP 200 OK
      ▼
[Android Telefon: Teslimat Başarılı (Status: SUCCESS)]
```

---

## 📋 Adım Adım Kurulum (5 Dakika)

### 1. Yeni Bir Google E-Tablo Oluşturma
1. Tarayıcınızda [sheets.new](https://sheets.new) adresine gidin (veya Google Drive'da yeni bir Google E-Tablo açın).
2. Tablo başlığına istediğiniz bir ad verin (Örn: `SMS_Gelen_Kutusu` veya `Gelen_SMS_Arsivi`).

---

### 2. Apps Script Kodunu Eklemek
1. Üst menüden **Uzantılar** (Extensions) > **Apps Script** seçeneğine tıklayın.
2. Açılan kod editöründeki mevcut `function myFunction() {}` kodunu silin.
3. Bu projedeki [`Code.gs`](Code.gs) dosyasının tüm içeriğini kopyalayıp buraya yapıştırın.
4. Üstteki **Kaydet** (Ctrl+S / Disket simgesi) butonuna basın.

---

### 3. Tabloyu Tek Tıkla Hazırlamak (setupSheet)
1. Apps Script editörünün üst araç çubuğundaki fonksiyon açılır kutusundan **setupSheet** seçeneğini seçin.
2. **Çalıştır** (Run) butonuna tıklayın.
3. İlk çalıştırmada Google bir izin penceresi açacaktır:
   - **İzinleri İncele** (Review Permissions) butonuna tıklayın.
   - Google hesabınızı seçin.
   - *"Google bu uygulamayı doğrulamadı"* uyarısı çıkarsa sol alttaki **Gelişmiş** (Advanced) bağlantısına tıklayın.
   - **Untitled project (güvenli değil) sayfasına git** seçeneğine tıklayın ve **İzin Ver** (Allow) deyin.
4. Google E-Tablo sekmenize geri dönüp baktığınızda, başlık satırının lacivert renkli, şık sütun genişlikleriyle otomatik oluştuğunu göreceksiniz!

---

### 4. Web Uygulaması Olarak Dağıtmak (Web App Deployment)
Bu adım Android uygulamasının Google Sheets'e erişebilmesi için gereklidir:

1. Apps Script penceresinin sağ üst köşesindeki mavi **Dağıt** (Deploy) butonuna tıklayın ve **Yeni dağıtım** (New deployment) seçeneğini seçin.
2. Sol taraftaki çark simgesine (⚙️) tıklayıp tür olarak **Web uygulaması** (Web app) seçin.
3. Alanları şu şekilde doldurun:
   - **Açıklama** (Description): `SMS2API Webhook`
   - **Farklı yürüt:** (Execute as): **Ben (<e-posta-adresiniz>)** *(Me)*
   - **Erişimi olanlar:** (Who has access): **Herkes** *(Anyone)* ⚠️ *(Önemli: Android'in giriş yapmadan veri yazabilmesi için "Herkes" seçilmelidir).*
4. **Dağıt** (Deploy) butonuna tıklayın.
5. Karşınıza **Web uygulaması URL'si** (Web app URL) gelecektir:
   ```
   https://script.google.com/macros/s/AKfycbx.../exec
   ```
6. **Kopyala** (Copy) butonuna tıklayarak bu URL'yi kopyalayın.

---

### 5. Android Uygulamasını (SMS2API) Bağlama ve Test
1. Android telefonunuzda **SMS2API** uygulamasını açın.
2. Alt menüden **Ayarlar** (Settings) sekmesine geçin.
3. **Target REST API Configuration** bölümüne gidin:
   - **Webhook Endpoint URL**: Kopyaladığınız `https://script.google.com/macros/s/.../exec` URL'sini yapıştırın.
   - **Kimlik Doğrulama** (Authentication): `None` (Hiçbiri).
4. **Test Connection** (Bağlantıyı Sına) butonuna dokunun.
5. Ekranda yeşil onay mesajını göreceksiniz.
6. Google E-Tablo sayfanızı açın; `TEST_SENDER` tarafından gönderilen test satırının anında tabloya eklendiğini göreceksiniz!

---

## 🔒 Güvenlik Notları ve İsteğe Bağlı API Anahtarı

Standart kurulumda Google Apps Script Web App URL'si uzun, benzersiz ve tahmin edilemez bir hash (`AKfycb...`) içerir.

Dilerseniz ek bir güvenlik katmanı olarak gizli anahtar ekleyebilirsiniz:
1. `Code.gs` dosyasının en üstündeki `API_SECRET_KEY` alanına bir şifre yazın:
   ```javascript
   API_SECRET_KEY: "gizli_sifreniz_123"
   ```
2. Değişikliği kaydedip **Dağıt > Dağıtımları yönet > Düzenle (kalem simgesi) > Sürüm: Yeni sürüm** diyerek yeniden dağıtın.
3. Android uygulamasında URL'nin sonuna `?key=gizli_sifreniz_123` parametresini ekleyin:
   ```
   https://script.google.com/macros/s/.../exec?key=gizli_sifreniz_123
   ```
   *(Alternatif olarak Ayarlar -> Custom Header altından `X-API-Key` ekleyebilirsiniz).*
