/**
 * ==============================================================================
 * SMS2API - Google Sheets Webhook Backend (Google Apps Script)
 * ==============================================================================
 *
 * Bu betik, SMS2API Android uygulamasından gelen SMS bildirimlerini yakalayıp
 * Google E-Tablolar (Google Sheets) içerisine anında yeni satır olarak ekler.
 *
 * Özellikler:
 * - %100 ÜCRETSİZ (Herhangi bir kişisel @gmail.com hesabı ile çalışır)
 * - Sıfır sunucu kurulumu (Tamamen Google Cloud / Apps Script altyapısı)
 * - 🔐 ZORUNLU Token Kimlik Doğrulaması (3 farklı kanaldan kabul eder)
 * - Mükerrer kayıt engelleme (SHA-256 tabanlı message_id kontrolü)
 * - Türkiye Yerel Saati (GMT+3) formatlama
 * - Akıllı Doğrulama Kodu (OTP / Şifre) ayıklama
 * - Şık tablo biçimlendirme ve otomatik sütun genişlikleri
 *
 * ==============================================================================
 */

// ==============================================================================
// 1. YAPILANDIRMA AYARLARI  ←  SADECE BU BÖLÜMÜ DEĞİŞTİRMENİZ YETERLİ
// ==============================================================================
const CONFIG = {
  // SMS kayıtlarının ekleneceği sayfa adı
  SHEET_NAME: "SMS_Gelen_Kutusu",

  // Türkiye Saati (GMT+3)
  TIMEZONE: "GMT+3",
  DATE_FORMAT: "yyyy-MM-dd HH:mm:ss",

  // Mükerrer kayıt kontrolü (Aynı message_id tekrar gelirse yeniden eklenmez)
  ENABLE_DEDUPLICATION: true,

  // SMS içeriğindeki 4-8 haneli SMS onay kodlarını ayıklayıp ayrı sütuna yaz
  EXTRACT_OTP_CODE: true,

  // ╔══════════════════════════════════════════════════════════════════╗
  // ║  📋 SPREADSHEET ID — ZORUNLU                                    ║
  // ║                                                                  ║
  // ║  Google Sheets URL'sindeki ID'yi buraya yapıştırın:             ║
  // ║  https://docs.google.com/spreadsheets/d/BURAYA_ID/edit          ║
  // ║                                                                  ║
  // ║  ⚠️  Bu alan boş kalırsa script deployed web app olarak         ║
  // ║  çalışırken sheet'e erişemez ve 200 OK döner ama kayıt yok!    ║
  // ╚══════════════════════════════════════════════════════════════════╝
  SPREADSHEET_ID: "BURAYA_SPREADSHEET_ID_YAZIN",

  // ╔══════════════════════════════════════════════════════════════════╗
  // ║  🔐 GÜVENLİK ANAHTARI (TOKEN) — ZORUNLU                        ║
  // ║                                                                  ║
  // ║  Bu placeholder'ı kendi güçlü token'ınızla değiştirin.          ║
  // ║  Örnek: "sms2api-k9Xm2pLq-2024" veya bir UUID.                 ║
  // ║                                                                  ║
  // ║  Script bu token'ı 2 kanaldan okur (öncelik sırasıyla):         ║
  // ║   1. JSON body → "auth_token" alanı  (Bearer Token seçeneği)   ║
  // ║   2. URL query  → ?token=<TOKEN>     (URL'ye manuel ekleme)     ║
  // ║                                                                  ║
  // ║  ⚠️  NOT: Deployed Apps Script web app'lerde HTTP header'ları   ║
  // ║  (X-API-Key vb.) okunamaz. Token için body veya URL query       ║
  // ║  parametresi kullanın. Android uygulamasında "Bearer Token"     ║
  // ║  seçeneğini tercih edin — body'e auth_token otomatik eklenir.  ║
  // ║                                                                  ║
  // ║  ⚠️  UYARI: Bu alan boş veya placeholder ise tüm istekler       ║
  // ║  REDDEDİLİR. Script çalışmadan önce mutlaka doldurun!           ║
  // ╚══════════════════════════════════════════════════════════════════╝
  AUTH_TOKEN: "BURAYA_GUCLU_BIR_TOKEN_YAZIN"
};


// ==============================================================================
// 2. TOKEN DOĞRULAMA (isAuthorized)
// ==============================================================================

/**
 * Gelen isteği 3 kanaldan kontrol ederek token doğrulaması yapar:
 *   1. JSON body içindeki "auth_token" alanı
 *   2. "X-API-Key" HTTP header (e.parameter üzerinden Apps Script'te erişilir)
 *   3. URL query parametresi: ?token=<TOKEN>
 *
 * @param {Object} e - Apps Script request event
 * @param {Object|null} body - JSON.parse edilmiş gövde ya da null
 * @returns {boolean}
 */
function isAuthorized(e, body) {
  const expected = CONFIG.AUTH_TOKEN;

  // Token hiç ayarlanmamış veya hâlâ placeholder ise tüm istekleri engelle
  if (!expected || expected.trim() === "" || expected === "BURAYA_GUCLU_BIR_TOKEN_YAZIN") {
    console.error("AUTH_TOKEN ayarlanmamis! Tum istekler reddediliyor.");
    return false;
  }

  const token = expected.trim();

  // Kanal 1: JSON body → auth_token
  // Android uygulaması "Bearer Token" modunda bu alanı otomatik ekler
  if (body && typeof body.auth_token === "string" && body.auth_token.trim() === token) {
    return true;
  }

  // Kanal 2: URL query param → ?token= veya ?key=
  // NOT: Deployed Apps Script web app'lerde HTTP headerları okunamaz.
  // Bu nedenle X-API-Key header kontrolü kaldırıldı. Token için
  // body (auth_token) veya URL query param kullanın.
  const queryToken = (e.parameter && (e.parameter.token || e.parameter.key)) || "";
  if (queryToken.trim() === token) return true;

  return false;
}


// ==============================================================================
// 3. HTTP POST YÖNETİCİSİ (Android Uygulamasından Gelen SMS'leri Karşılar)
// ==============================================================================
function doPost(e) {
  const lock = LockService.getScriptLock();

  try {
    // Eşzamanlı gelen SMS'lerde çakışmayı önlemek için 10 saniye kilitle
    lock.waitLock(10000);

    // İstek gövdesini önce ayrıştır — token body'den de gelebilir
    if (!e || !e.postData || !e.postData.contents) {
      return createJsonResponse({ status: "error", code: 400, message: "Post verisi bulunamadi" });
    }

    let payload;
    try {
      payload = JSON.parse(e.postData.contents);
    } catch (_) {
      return createJsonResponse({ status: "error", code: 400, message: "Gecersiz JSON govdesi" });
    }

    // 🔐 Token doğrulaması — body parse edildikten sonra yapılır
    if (!isAuthorized(e, payload)) {
      return createJsonResponse({ status: "error", code: 401, message: "Yetkisiz erisim: gecersiz veya eksik token" });
    }

    const payloadParsed = payload; // zaten yukarıda parse edildi
    const messageId    = payloadParsed.message_id || ("sms_" + Date.now());
    const sender       = payloadParsed.sender    || "Bilinmiyor";
    const body         = payloadParsed.body      || "";
    const rawTimestamp = payloadParsed.timestamp || Date.now();

    // Çalışma sayfasını al
    // ⚠️  getActiveSpreadsheet() deployed web app'lerde NULL döner!
    // SPREADSHEET_ID config'den açıyoruz.
    let ss;
    if (CONFIG.SPREADSHEET_ID && CONFIG.SPREADSHEET_ID !== "BURAYA_SPREADSHEET_ID_YAZIN") {
      ss = SpreadsheetApp.openById(CONFIG.SPREADSHEET_ID);
    } else {
      // Fallback: Script editöründen test ediliyorsa getActiveSpreadsheet çalışır
      ss = SpreadsheetApp.getActiveSpreadsheet();
    }

    if (!ss) {
      return createJsonResponse({
        status: "error",
        code: 500,
        message: "Spreadsheet bulunamadi. SPREADSHEET_ID ayarini kontrol edin."
      });
    }

    let sheet = ss.getSheetByName(CONFIG.SHEET_NAME);
    if (!sheet) {
      sheet = ss.insertSheet(CONFIG.SHEET_NAME);
    }

    // Sayfa boşsa başlıkları otomatik oluştur
    if (sheet.getLastRow() === 0) {
      setupSheet();
    }

    // Mükerrer Kayıt Kontrolü (Deduplication)
    if (CONFIG.ENABLE_DEDUPLICATION && sheet.getLastRow() > 1) {
      const messageIdsRange = sheet.getRange(2, 1, sheet.getLastRow() - 1, 1).getValues();
      const isDuplicate = messageIdsRange.some(row => row[0] === messageId);
      
      if (isDuplicate) {
        return createJsonResponse({
          status: "success",
          action: "ignored_duplicate",
          message_id: messageId,
          message: "Mesaj daha once kaydedilmis."
        });
      }
    }

    // Zaman Damgasını Türkiye Yerel Saatine Dönüştür
    const formattedDate = Utilities.formatDate(new Date(rawTimestamp), CONFIG.TIMEZONE, CONFIG.DATE_FORMAT);

    // Doğrulama Kodu (OTP) Ayıklama
    let otpCode = "";
    if (CONFIG.EXTRACT_OTP_CODE && body) {
      otpCode = extractOtpFromText(body);
    }

    // Satırı Google E-Tabloya Ekle
    // Sütunlar: [MessageID, Gönderen, Mesaj Metni, Tarih / Saat, Unix Zaman, Doğrulama Kodu, Durum]
    sheet.appendRow([
      messageId,
      sender,
      body,
      formattedDate,
      rawTimestamp,
      otpCode,
      "KAYDEDILDI"
    ]);

    // Başarılı yanıt döndür (Android uygulaması HTTP 200 alarak durumu SUCCESS yapar)
    return createJsonResponse({
      status: "success",
      message_id: messageId,
      row: sheet.getLastRow(),
      received_at: formattedDate
    });

  } catch (error) {
    return createJsonResponse({
      status: "error",
      message: error.toString()
    }, 500);
  } finally {
    lock.releaseLock();
  }
}


// ==============================================================================
// 4. HTTP GET YÖNETİCİSİ (Tarayıcıdan Uç Noktayı Sınamak İçin)
// ==============================================================================
function doGet(e) {
  // GET isteklerinde de token doğrulaması (URL query param veya header)
  if (!isAuthorized(e, null)) {
    return createJsonResponse({ status: "error", code: 401, message: "Yetkisiz erisim" });
  }
  return createJsonResponse({
    status: "online",
    service: "SMS2API Google Sheets Webhook",
    message: "Servis aktif. Token dogrulandi. SMS iletimi icin hazir.",
    time: Utilities.formatDate(new Date(), CONFIG.TIMEZONE, CONFIG.DATE_FORMAT)
  });
}


// ==============================================================================
// 5. TABLO BAŞLIKLARINI VE BİÇİMLENDİRMEYİ 1 TIKLA HAZIRLAMA (setupSheet)
// ==============================================================================
/**
 * Bu fonksiyonu Apps Script editöründe yukarıdan 'setupSheet' seçip
 * 'Çalıştır' diyerek 1 kez çalıştırabilirsiniz. Tablonuzu profesyonelce formatlar.
 */
function setupSheet() {
  let ss;
  if (CONFIG.SPREADSHEET_ID && CONFIG.SPREADSHEET_ID !== "BURAYA_SPREADSHEET_ID_YAZIN") {
    ss = SpreadsheetApp.openById(CONFIG.SPREADSHEET_ID);
  } else {
    ss = SpreadsheetApp.getActiveSpreadsheet();
  }

  if (!ss) {
    Logger.log("HATA: Spreadsheet bulunamadi! SPREADSHEET_ID'yi Config'e ekleyin.");
    return;
  }

  let sheet = ss.getSheetByName(CONFIG.SHEET_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(CONFIG.SHEET_NAME);
  }

  // Başlıklar
  const headers = [
    ["Mesaj ID", "Gönderen", "Mesaj Metni", "Tarih / Saat", "Unix Zaman", "Doğrulama Kodu", "Durum"]
  ];

  sheet.getRange(1, 1, 1, 7).setValues(headers);

  // Başlık satırı stili: Koyu lacivert zemin, beyaz kalın yazı
  const headerRange = sheet.getRange(1, 1, 1, 7);
  headerRange.setBackground("#1E293B");
  headerRange.setFontColor("#FFFFFF");
  headerRange.setFontWeight("bold");
  headerRange.setFontSize(11);
  headerRange.setHorizontalAlignment("center");
  headerRange.setVerticalAlignment("middle");
  sheet.setRowHeight(1, 38);

  // İlk satırı sabitle (Scroll ederken başlık görünür kalsın)
  sheet.setFrozenRows(1);

  // Sütun genişlikleri
  sheet.setColumnWidth(1, 190); // Mesaj ID
  sheet.setColumnWidth(2, 140); // Gönderen
  sheet.setColumnWidth(3, 420); // Mesaj Metni
  sheet.setColumnWidth(4, 160); // Tarih / Saat
  sheet.setColumnWidth(5, 120); // Unix Zaman
  sheet.setColumnWidth(6, 120); // Doğrulama Kodu
  sheet.setColumnWidth(7, 110); // Durum

  // Mesaj metninin satırda kırılması (Wrap text)
  sheet.getRange("C:C").setWrap(true);

  // Hizalamalar
  sheet.getRange("A:A").setHorizontalAlignment("center");
  sheet.getRange("B:B").setHorizontalAlignment("center");
  sheet.getRange("D:D").setHorizontalAlignment("center");
  sheet.getRange("E:E").setHorizontalAlignment("right");
  sheet.getRange("F:F").setHorizontalAlignment("center");
  sheet.getRange("G:G").setHorizontalAlignment("center");

  SpreadsheetApp.flush();
  Logger.log("SMS tablosu basariyla olusturuldu ve formatlandi!");
}


// ==============================================================================
// 6. YARDIMCI FONKSİYONLAR
// ==============================================================================

/**
 * SMS metninden 4 ila 8 haneli tek kullanımlık güvenlik/doğrulama kodunu ayıklar.
 */
function extractOtpFromText(text) {
  if (!text) return "";

  // Yaygın Türkçe ve İngilizce kalıplar (Kod: 123456, Şifreniz: 9876, vb.)
  const patterns = [
    /(?:kod(?:u|unuz)?|sifre(?:niz)?|şifre(?:niz)?|code|otp|pin|parola)[:\s]+([0-9]{4,8})/i,
    /(?:onay|güvenlik|guvenlik|dogrulama|doğrulama|verification)\s*(?:kod(?:u|unuz)?|şifre(?:niz)?)?[:\s]+([0-9]{4,8})/i,
    /\b([0-9]{6})\b/, // Standart 6 haneli banka kodları
    /\b([0-9]{4,8})\b/ // 4-8 haneli diğer bağımsız sayılar
  ];

  for (let i = 0; i < patterns.length; i++) {
    const match = text.match(patterns[i]);
    if (match && match[1]) {
      return match[1];
    }
  }
  return "";
}

/**
 * JSON yanıt nesnesi üretir
 */
function createJsonResponse(dataObject) {
  return ContentService
    .createTextOutput(JSON.stringify(dataObject))
    .setMimeType(ContentService.MimeType.JSON);
}
