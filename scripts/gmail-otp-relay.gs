/**
 * Gola Grocery - free OTP email relay (Google Apps Script, sends from your Gmail, ~100 emails/day).
 *
 * Setup (5 minutes):
 *  1. https://script.google.com -> New project -> replace everything with this file -> Save (name it "Gola OTP relay").
 *  2. Project Settings (gear) -> Script properties -> Add property:
 *        RELAY_SECRET = <any long random text you invent>
 *  3. Deploy -> New deployment -> type "Web app" -> Execute as: Me, Who has access: Anyone -> Deploy -> Authorize.
 *  4. Copy the Web app URL (ends with /exec).
 *  5. Render -> gola-grocery-api -> Environment:
 *        MAIL_RELAY_URL    = the /exec URL
 *        MAIL_RELAY_SECRET = the same RELAY_SECRET text
 *     Save, rebuild, and deploy. Then set OTP_DEV_ECHO=false once the OTP email arrives.
 */
function doPost(e) {
  try {
    var body = JSON.parse(e.postData.contents);
    var secret = PropertiesService.getScriptProperties().getProperty('RELAY_SECRET');
    if (!secret || body.secret !== secret) return reply({ ok: false, error: 'unauthorized' });
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(body.to || '')) return reply({ ok: false, error: 'bad recipient' });
    if (!/Gola Grocery OTP$/.test(body.subject || '')) return reply({ ok: false, error: 'only OTP emails are allowed' });
    MailApp.sendEmail({ to: body.to, subject: body.subject, htmlBody: body.html, name: 'Gola Grocery' });
    return reply({ ok: true, remainingQuota: MailApp.getRemainingDailyQuota() });
  } catch (err) {
    return reply({ ok: false, error: String(err) });
  }
}

function reply(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
