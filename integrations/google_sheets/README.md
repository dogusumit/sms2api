# Google E-Tablolar (Google Sheets) Entegrasyonu

Bu dizin, **SMS2API** Android uygulamasının yakaladığı SMS'leri **Google E-Tablolar (Google Sheets)** çalışma tablosuna anında, kesintisiz ve **%100 ücretsiz** olarak aktarmak için gereken Google Apps Script backend kodunu ve kurulum rehberini içerir.

---

## 🌟 Neden Google Sheets?

| Özellik | Google Sheets (Apps Script) | Power Automate (Office 365) |
|---|---|---|
| **Hesap Türü** | Standart Kişisel (`@gmail.com`) veya Kurumsal | Kurumsal / Okul Hesabı veya Ücretli Lisans |
| **Maliyet** | **%100 Ücretsiz** | HTTP Trigger için Premium/İş Lisansı gerekir |
| **Sunucu İhtiyacı** | Sıfır (Google Cloud üzerinde çalışır) | Sıfır (Microsoft Cloud üzerinde çalışır) |
| **Kurulum Süresi** | ~3-5 dakika | ~5-10 dakika |
| **Özelleştirme** | Tam JavaScript / Apps Script esnekliği | Düşük kodlu akış blokları |

---

## 📂 Dizin İçeriği

```
integrations/google_sheets/
├── README.md                   # Bu tanıtım ve özet belgesi
├── Code.gs                     # Google Apps Script Webhook kodu (doPost, doGet, setupSheet)
└── google_sheets_guide.md      # Adım adım ekran yapılandırması ve dağıtım rehberi
```

---

## ⚡ 3 Dakikada Hızlı Kurulum

1. **E-Tablo Oluşturun:** [sheets.new](https://sheets.new) adresinden yeni bir sayfa açın.
2. **Kodu Yapıştırın:** **Uzantılar > Apps Script** menüsüne girip [`Code.gs`](Code.gs) dosyasını yapıştırın ve kaydedin.
3. **Tabloyu Biçimlendirin:** Fonksiyon listesinden `setupSheet` seçip **Çalıştır**'a basın (Tablo başlıkları otomatik oluşur).
4. **Web Uygulaması Olarak Dağıtın:**
   - Sağ üstten **Dağıt > Yeni Dağıtım** seçin.
   - Tür: **Web uygulaması**.
   - Farklı yürüt: **Ben**.
   - Erişimi olanlar: **Herkes**.
   - Alınan Webhook URL'sini kopyalayın (`https://script.google.com/macros/s/.../exec`).
5. **Android Uygulamasına Girin:**
   - SMS2API Ayarlar sekmesinde **Webhook Endpoint URL** alanına bu adresi yapıştırın.
   - **Test Connection** butonuna basın; test satırınız Google E-Tablonuzda anında belirecektir!

Detaylı rehber ve izin adımları için 👉 **[`google_sheets_guide.md`](google_sheets_guide.md)** belgesine bakabilirsiniz.
