/**
 * SMS2API Excel Online Tablo Hazırlama Betiği (Office Script)
 * 
 * Bu betik Excel Online (Microsoft 365) çalışma sayfanızda SMS kayıtlarının
 * ekleneceği 'SmsTable' isimli tabloyu ve gerekli sütun başlıklarını otomatik oluşturur.
 * 
 * Kullanım:
 * 1. Excel Online'da boş bir çalışma kitabı açın (veya var olan sayfada).
 * 2. Üst menüden 'Otomatikleştir' (Automate) sekmesine tıklayın.
 * 3. 'Yeni Betik' (New Script) seçeneğine tıklayın.
 * 4. Bu dosyadaki tüm kodları yapıştırın ve 'Çalıştır' (Run) butonuna basın.
 */
function main(workbook: ExcelScript.Workbook) {
  let sheet = workbook.getActiveWorksheet();

  // Sütun başlıkları
  let headers = [
    ["MessageID", "Sender", "Body", "ReceivedDate", "Timestamp", "OTPCode", "Status"]
  ];

  // Başlıkları A1:G1 aralığına yerleştir
  let headerRange = sheet.getRange("A1:G1");
  headerRange.setValues(headers);

  // Tabloyu oluştur (varsa yeniden adlandırır veya yeni ekler)
  let table = sheet.getTables().find(t => t.getName() === "SmsTable");
  if (!table) {
    table = sheet.addTable("A1:G1", true);
    table.setName("SmsTable");
  }

  // Modern koyu mavi tablo stili uygula
  table.setPredefinedTableStyle("TableStyleMedium9");

  // Sütun genişlikleri ve metin kaydırma ayarları
  sheet.getRange("A:A").getFormat().setColumnWidth(170); // MessageID
  sheet.getRange("B:B").getFormat().setColumnWidth(130); // Sender
  sheet.getRange("C:C").getFormat().setColumnWidth(380); // Body
  sheet.getRange("D:D").getFormat().setColumnWidth(150); // ReceivedDate
  sheet.getRange("E:E").getFormat().setColumnWidth(120); // Timestamp
  sheet.getRange("F:F").getFormat().setColumnWidth(100); // OTPCode
  sheet.getRange("G:G").getFormat().setColumnWidth(110); // Status

  // Mesaj metni sütununda uzun metinlerin alt satıra geçmesi için (Wrap Text)
  sheet.getRange("C:C").getFormat().setWrapText(true);

  // Hizalama ayarları
  sheet.getRange("A:A").getFormat().setHorizontalAlignment(ExcelScript.HorizontalAlignment.center);
  sheet.getRange("B:B").getFormat().setHorizontalAlignment(ExcelScript.HorizontalAlignment.center);
  sheet.getRange("D:D").getFormat().setHorizontalAlignment(ExcelScript.HorizontalAlignment.center);
  sheet.getRange("E:E").getFormat().setHorizontalAlignment(ExcelScript.HorizontalAlignment.right);
  sheet.getRange("F:F").getFormat().setHorizontalAlignment(ExcelScript.HorizontalAlignment.center);
  sheet.getRange("G:G").getFormat().setHorizontalAlignment(ExcelScript.HorizontalAlignment.center);

  console.log("SMS tablosu 'SmsTable' başarıyla oluşturuldu ve biçimlendirildi.");
}
