import argparse
import json
import subprocess
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--client', type=Path, default=ROOT / 'build/telegram-client')
client = parser.parse_args().client
java_root = client / 'TMessagesProj/src/main/java'
syntax_files = [java_root / 'org/telegram/ui' / (name + '.java') for name in ['LoginActivity', 'CountrySelectActivity', 'SettingsActivity', 'UserInfoActivity', 'SessionsActivity', 'ProfileActivity']]
syntax_files += list((java_root / 'io/github/lubara7/tgmax/telegram').glob('*.java'))
subprocess.run(['java', str(ROOT / 'scripts/CheckPreparedJava.java'), *map(str, syntax_files)], check=True)
expected = json.loads((ROOT / 'integrations/telegram/login-countries.json').read_text())
actual = [line.split(';') for line in (client / 'TMessagesProj/src/main/assets/countries.txt').read_text().splitlines()]
assert len(expected) == len(actual) == 44
assert [(row[1], row[0], row[2]) for row in actual] == [(row['iso'], row['code'], row['name']) for row in expected]
assert [row[1] for row in actual if row[0] == '7'] == ['RU', 'KZ']
assert [row[1] for row in actual if row[0] == '1'] == ['GD', 'KN']
resources = client / 'TMessagesProj/src/main/res'
text = {s.get('name'): s.text for s in ET.parse(resources / 'values-ru/strings.xml').findall('string')}
assert text['StartText'] == 'Проверьте код страны и введите свой номер телефона из аккаунта MAX.'
source = (client / 'TMessagesProj/src/main/java/org/telegram/ui/CountrySelectActivity.java').read_text()
assert 'countries.put("", entries);' in source
assert source.index('countries.put("", entries);') < source.index('Collections.sort(sortedCountries')
login = (client / 'TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java').read_text()
assert 'setCaptchaHandler(maxCaptchaHandler)' in login
assert 'clearCaptchaHandler(maxCaptchaHandler)' in login
assert 'TL_help_getNearestDc req' not in login
assert (client / 'TMessagesProj/src/main/assets/tgmax-captcha.html').is_file()
print('44 ordered countries, shared codes, Russian login copy and CAPTCHA lifecycle checked')

assert text['YourNumber'] == 'Ваш номер телефона'
assert text['Country'] == 'Страна'
assert 'R.drawable.maxgram_help' in login
assert 'https://legal.max.ru/recsysrules' in login
assert 'TgMaxCallActivity' in login
for qualifier in ['values', 'values-ru']:
    strings = {s.get('name'): s.text for s in ET.parse(resources / qualifier / 'strings.xml').findall('string')}
    assert all(strings.get(key) for key in ['YourNumber', 'Country', 'TgMaxLanguageTitle', 'TgMaxCallTitle', 'TgMaxPrivacy', 'TgMaxTerms', 'TgMaxRecommendations'])
    assert strings['AppName'] == 'MAXGRAM'
print('MAXGRAM name, RU/EN login translations, help, legal footer and call screen preparation checked')

phone = login[login.index('public class PhoneView'):login.index('public class LoginActivitySmsView')]
assert 'syncContacts = false;' in phone
assert 'syncContactsBox = new CheckBoxCell' not in phone
assert 'testBackendCheckBox = new CheckBoxCell' not in phone
assert 'showDebugMenu();' not in phone
assert 'setPadding(0, dp(32), 0, dp(152));' in phone
assert 'layoutMaxPhoneContent();' in login
assert '@mipmap/maxgram_launcher' in (resources / 'values-v31/styles.xml').read_text()
for style in resources.rglob('styles.xml'):
    if 'windowSplashScreenAnimatedIcon' in style.read_text():
        assert '@mipmap/maxgram_launcher' in style.read_text()
print('Login footer space, removed Telegram debug/contact controls and MAX splash artwork checked')

app_build = (client / 'TMessagesProj/build.gradle').read_text()
assert 'path "jni/CMakeLists.txt"\n            version "3.31.6"' in app_build
backend_build = (client / 'tgmax-backend/build.gradle.kts').read_text()
assert "implementation 'com.github.luben:zstd-jni:1.5.7-7@aar'" in app_build
assert 'compileOnly("com.github.luben:zstd-jni:1.5.7-7")' in backend_build
assert 'testImplementation("com.github.luben:zstd-jni:1.5.7-7")' in backend_build
assert 'implementation("com.github.luben:zstd-jni:' not in backend_build
assert '-keep class com.github.luben.zstd.** { *; }' in (client / 'TMessagesProj/proguard-rules.pro').read_text()
print('Official Android Zstandard AAR, desktop JAR exclusion and JNI keep rules checked')

assert 'TgMaxQrActivity' not in login
assert 'VIEW_QR_LOGIN' not in login
assert 'maxQrButton' not in login
assert 'defaultConfig.applicationId = APP_PACKAGE' in app_build
assert 'APP_PACKAGE=io.github.lubara7.tgmax' in (client / 'gradle.properties').read_text()
assert 'jvmTarget = JavaVersion.VERSION_17.toString()' in app_build
assert 'splits.abi { enable false; reset(); universalApk true }' in app_build
assert 'LAST_DB_VERSION = 181' in (client / 'TMessagesProj/src/main/java/org/telegram/messenger/MessagesStorage.java').read_text()
assert 'TgMaxCacheMigration.migrate(database, version)' in (client / 'TMessagesProj/src/main/java/org/telegram/messenger/DatabaseMigrationHelper.java').read_text()
settings = (client / 'TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java').read_text()
fill = settings[settings.index('final boolean walletAbove'):settings.index('items.add(UItem.asHeader(getString(R.string.SettingsHelp)))')]
assert all(label not in fill for label in ['R.string.TelegramPremium', 'R.string.TelegramStars', 'R.string.GramEarnings', 'R.string.SendAGift'])
assert 'https://business.max.ru/' in settings
assert 'TgMaxSettingsActivity' in settings
assert (client / 'TMessagesProj/src/main/java/tw/nekomimi').exists()
for qualifier in ['values', 'values-ru']:
    strings = {s.get('name'): s.text for s in ET.parse(resources / qualifier / 'strings.xml').findall('string')}
    assert all(strings.get(key) for key in ['TgMaxQrTitle', 'TgMaxGhost', 'TgMaxUnread', 'TgMaxNoTyping', 'TgMaxUnknownUser'])
print('Pinned Nagram base, no login QR, local privacy settings and financial menu removal checked')

assert 'TgMaxDisclaimer.showIfNeeded(this)' in (client / "TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java").read_text()

account = (client / 'TMessagesProj/src/main/java/org/telegram/ui/UserInfoActivity.java').read_text()
assert 'TgMaxAccountControls.showInactivity(this, currentAccount)' in account
assert 'TgMaxPrivacyActivity(currentAccount)' in settings
assert 'TgMaxSessionActivity' in (client / 'TMessagesProj/src/main/java/io/github/lubara7/tgmax/telegram/TgMaxPrivacyActivity.java').read_text()
assert 'TgMaxTokenActivity' in login
print('MAX account fields, privacy/session screens and explicit own-token login prepared')

manifest=(client / 'TMessagesProj/src/main/AndroidManifest.xml').read_text()
assert 'android.permission.READ_CONTACTS' in manifest
assert 'android.permission.WRITE_CONTACTS' not in manifest
assert 'MaxPhonebookNames' in (java_root / 'io/github/lubara7/tgmax/telegram/TgMaxRuntime.java').read_text()
