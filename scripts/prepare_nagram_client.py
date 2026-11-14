#!/usr/bin/env python3
import argparse
import json
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA_PREFIX = "io.github.lubara7.tgmax.nagram."


def run(*args, cwd=None):
    return subprocess.check_output(args, cwd=cwd, text=True).strip()


def change(path, old, new, count=1):
    text = path.read_text()
    actual = text.count(old)
    if actual != count:
        raise RuntimeError(f"Upstream changed: {path.name}: expected {count} occurrences, found {actual}")
    path.write_text(text.replace(old, new))


def add_after_opening(path, signature, code):
    text = path.read_text()
    match = re.search(signature, text)
    if match is None:
        raise RuntimeError(f"Upstream method missing in {path}")
    position = text.index("{", match.start()) + 1
    path.write_text(text[:position] + "\n" + code + text[position:])


def guard_mtproto(target):
    java = target / "TMessagesProj/src/main/java"
    connection = java / "org/telegram/tgnet/ConnectionsManager.java"
    original = connection.read_text()
    declarations = list(re.finditer(r"public static native (\w+) (native_\w+)\(([^;]*)\);", original))
    if len(declarations) < 35:
        raise RuntimeError("ConnectionsManager native API changed")
    local_only = {"native_setJava", "native_isGoodPrime", "native_test_AuthAuthorization"}
    names = {m[2] for m in declarations} - local_only
    declaration_text = {}
    masked = original
    for i, m in reversed(list(enumerate(declarations))):
        marker = f"TGMAX_NATIVE_DECLARATION_{i}"
        declaration_text[marker] = m[0]
        masked = masked[:m.start()] + marker + masked[m.end():]
    for name in names:
        masked = re.sub(r"\b" + name + r"\s*\(", "tgmax_" + name + "(", masked)
    for marker, declaration in declaration_text.items():
        masked = masked.replace(marker, declaration)
    connection.write_text(masked)
    for path in (target / "TMessagesProj/src").rglob("*"):
        if path == connection or path.suffix not in (".java", ".kt"):
            continue
        text = path.read_text()
        for name in names:
            text = re.sub(r"(ConnectionsManager(?:\.getInstance\([^()]*\))?\s*\.\s*)" + name + r"\s*\(", r"\1tgmax_" + name + "(", text)
            if re.search(r"import\s+(?:static\s+)?org\.telegram\.tgnet\.ConnectionsManager\." + name + r"\b", text):
                text = re.sub(r"\b" + name + r"\b", "tgmax_" + name, text)
        if text != path.read_text():
            path.write_text(text)
    wrappers = []
    runtime = JAVA_PREFIX + "TgMaxRuntime"
    for declaration in declarations:
        result, name, parameters = declaration[1], declaration[2], declaration[3]
        if name not in names:
            continue
        body = {"void": "return;", "int": "return 0;", "long": "return 0L;", "boolean": "return false;"}[result]
        if name == "native_sendRequest": body = 'throw new IllegalStateException("MTProto is disabled in TG-MAX");'
        if name == "native_getCurrentTime": body = "return (int) (System.currentTimeMillis() / 1000);"
        if name == "native_getCurrentTimeMillis": body = "return System.currentTimeMillis();"
        if name == "native_getConnectionState": body = f"return {runtime}.connectionState(currentAccount);"
        if name == "native_cancelRequest": body = f"{runtime}.get(currentAccount).cancel(token);"
        if name == "native_failNotRunningRequest": body = f"{runtime}.get(currentAccount).cancel(token);"
        if name == "native_cancelRequestsForGuid": body = f"{runtime}.get(currentAccount).cancelGuid(guid);"
        if name == "native_bindRequestToGuid": body = f"{runtime}.get(currentAccount).bind(requestToken, guid);"
        if name == "native_cleanUp": body = f"{runtime}.get(currentAccount).cleanup(resetKeys);"
        if name == "native_checkProxy": body = "if (requestTimeDelegate != null) requestTimeDelegate.run(-1); return 0L;"
        wrappers.append(f"    public static {result} tgmax_{name}({parameters}) {{ {body} }}")
    text = connection.read_text()
    position = text.rfind("}")
    connection.write_text(text[:position] + "\n" + "\n".join(wrappers) + "\n" + text[position:])
    add_after_opening(connection, r"private void sendRequestInternal\([^)]*\)", f"""        if ({runtime}.enabled()) {{
            {runtime}.get(currentAccount).send(requestToken, object, onCompleteOrig,
                    onCompleteTimestamp, onQuickAck, onWriteToSocket);
            return;
        }}
""")
    return names


def restrict_languages(target):
    base = target / "TMessagesProj/src/main"
    controller = base / "java/org/telegram/messenger/LocaleController.java"
    helper = JAVA_PREFIX + "TgMaxLocales"
    change(controller, "        systemDefaultLocale = Locale.getDefault();", f"        {helper}.install(this);\n        systemDefaultLocale = Locale.getDefault();")
    change(controller, "currentInfo = getLanguageFromDict(systemDefaultLocale.getLanguage());",
           f"currentInfo = getLanguageFromDict({helper}.code(systemDefaultLocale.toLanguageTag()));")
    change(controller, "currentInfo = getLanguageFromDict(lang);",
           f"currentInfo = getLanguageFromDict({helper}.code(lang));")
    add_after_opening(controller, r"public void loadRemoteLanguages\(final int currentAccount, boolean applyCurrent\)",
                      f"        if ({JAVA_PREFIX}TgMaxRuntime.enabled()) {{ {helper}.install(this); return; }}\n")
    add_after_opening(controller, r"private void loadOtherLanguages\(\)",
                      f"        if ({JAVA_PREFIX}TgMaxRuntime.enabled()) return;\n")
    allowed = {"ru", "en", "es", "uz", "pt", "fr", "be"}
    removed = []
    for directory in (base / "res").glob("values-*"):
        match = re.match(r"values-([a-z]{2,3})(?:-|$)", directory.name)
        if match and match[1] not in allowed:
            shutil.rmtree(directory)
            removed.append(directory.name)
    sessions = base / "java/org/telegram/ui/SessionsActivity.java"
    change(sessions, "if (currentType == 0 && getMessagesController().qrLoginCamera)",
           f"if (!{JAVA_PREFIX}TgMaxRuntime.enabled() && currentType == 0 && getMessagesController().qrLoginCamera)")
    change(sessions, "return position == terminateAllSessionsRow || position >= otherSessionsStartRow",
           f"if ({JAVA_PREFIX}TgMaxRuntime.enabled() && currentType == 0) return position == terminateAllSessionsRow;\n            return position == terminateAllSessionsRow || position >= otherSessionsStartRow")
    login = base / "java/org/telegram/ui/LoginActivity.java"
    add_after_opening(login, r"public void saveSelfArgs\(Bundle outState\)",
                      f"        if ({JAVA_PREFIX}TgMaxRuntime.enabled()) {{ clearCurrentState(); return; }}\n")
    install_auth_translations(base / "res")
    return removed


def install_auth_translations(resources):
    mapping = {
        "language_title": ["Language"], "phone_title": ["YourPhone", "YourNumber"],
        "phone_description": ["StartText"], "phone_label": ["PhoneNumber"],
        "country_title": ["Country"], "country_search": ["SearchCountry"],
        "captcha_title": ["TgMaxCaptchaTitle"], "captcha_load_error": ["TgMaxCaptchaLoadError"],
        "captcha_webview_update": ["TgMaxCaptchaWebViewUpdate"], "captcha_rejected": ["TgMaxCaptchaRejected"],
        "action_continue": ["Next", "Continue"], "action_sign_in": ["StartMessaging"],
        "action_cancel": ["Cancel"], "action_back": ["Back"],
        "code_title": ["SentSmsCodeTitle"], "code_label": ["Code"],
        "code_description": ["SentSmsCode"], "action_resend": ["ResendCode"],
        "action_edit_phone": ["WrongNumber"], "error_invalid_phone": ["InvalidPhoneNumber"],
        "error_invalid_code": ["InvalidCode"], "error_expired_code": ["CodeExpired"],
        "error_unknown": ["ErrorOccurred"], "action_sign_out": ["LogOut"],
    }
    names = {"values": "English", "values-ru": "Русский", "values-es": "Español",
             "values-uz": "O‘zbek", "values-pt": "Português (Portugal)",
             "values-pt-rBR": "Português (Brasil)", "values-fr": "Français", "values-be": "Беларуская"}
    navigation = json.loads((ROOT / "integrations/nagram/navigation-strings.json").read_text())
    auth = json.loads((ROOT / "integrations/nagram/auth-strings.json").read_text())
    for qualifier, native_name in names.items():
        source = ROOT / "core/localization/src/main/res" / qualifier / "strings.xml"
        root = ET.parse(source).getroot()
        strings = {item.attrib["name"]: "".join(item.itertext()) for item in root.findall("string")}
        translations = {key: strings[name] for name, keys in mapping.items() for key in keys}
        translations.update(navigation[qualifier])
        translations.update(auth.get(qualifier, auth["values-pt-rBR"] if qualifier == "values-pt" else {}))
        translations.update(AppName="MAXGRAM CLIENT", AppNameBeta="MAXGRAM CLIENT", LanguageName=native_name)
        destination = resources / qualifier / "strings.xml"
        destination.parent.mkdir(parents=True, exist_ok=True)
        text = destination.read_text() if destination.exists() else '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n</resources>\n'
        for key, value in translations.items():
            element = ET.Element("string", {"name": key})
            element.text = value
            replacement = ET.tostring(element, encoding="unicode", short_empty_elements=False)
            pattern = r'<string\s+name="' + key + r'"[^>]*>[\s\S]*?</string>'
            if re.search(pattern, text):
                text = re.sub(pattern, lambda m: replacement, text, count=1)
            else:
                text = text.replace("</resources>", "    " + replacement + "\n</resources>")
        ET.fromstring(text)
        destination.write_text(text)


def configure_build(target):
    app = target / "TMessagesProj/build.gradle"
    change(app, "    id 'com.google.gms.google-services'\n", "")
    change(app, "    id 'com.google.firebase.crashlytics'\n", "")
    change(app, "dependencies {", "dependencies {\n    implementation project(':tgmax-backend')\n    implementation 'com.github.luben:zstd-jni:1.5.7-28@aar'")
    change(app, "    defaultConfig.applicationId = APP_PACKAGE", "    defaultConfig.applicationId = APP_PACKAGE\n    defaultConfig.resourceConfigurations += ['en', 'ru', 'es', 'uz', 'pt', 'pt-rBR', 'fr', 'be']")
    text = app.read_text()
    start = text.index("        // obtain your own keys")
    stop = text.index("        buildConfigField 'String', 'BUILD_VERSION_STRING'", start)
    text = text[:start] + "        def appId = '0'\n        def appHash = ''\n\n" + text[stop:]
    debug = text.index("        debug {", text.index("    buildTypes"))
    signing = text.index("signingConfig = signingConfigs.release", debug)
    text = text[:signing] + text[signing:].replace("signingConfig = signingConfigs.release", "signingConfig = signingConfigs.debug", 1)
    text += '\nif (System.getenv("TGMAX_ALPHA_KEYSTORE")) {\n    android.signingConfigs.debug.storeFile = file(System.getenv("TGMAX_ALPHA_KEYSTORE"))\n}\n'
    release = json.loads((ROOT / "release.json").read_text())
    text = re.sub(r"def verCode = \d+", f"def verCode = {release['versionCode']}", text, count=1)
    app.write_text(text)
    settings = target / "settings.gradle"
    settings.write_text(settings.read_text() + "\ninclude ':tgmax-backend'\n")
    shutil.copytree(ROOT / "max-backend", target / "tgmax-backend", ignore=shutil.ignore_patterns("build", ".gradle"))
    # Android must package the official AAR, not the desktop JVM JAR with its native binaries.
    change(target / "tgmax-backend/build.gradle.kts",
           '    implementation("com.github.luben:zstd-jni:1.5.7-28")',
           '    compileOnly("com.github.luben:zstd-jni:1.5.7-28")\n    testRuntimeOnly("com.github.luben:zstd-jni:1.5.7-28")')
    rules = target / "TMessagesProj/proguard-rules.pro"
    rules.write_text(rules.read_text() + '\n# JNI resolves these names from the Android Zstandard library.\n-keep class com.github.luben.zstd.** { *; }\n')
    properties = target / "gradle.properties"
    text = properties.read_text()
    text = re.sub(r"(?m)^APP_PACKAGE=.*$", "APP_PACKAGE=io.github.lubara7.tgmax", text)
    version = release["version"]
    text = re.sub(r"(?m)^APP_VERSION_NAME=.*$", "APP_VERSION_NAME=" + version, text)
    properties.write_text(text)
    variables = target / "TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java"
    change(variables, "USE_CLOUD_STRINGS = true", "USE_CLOUD_STRINGS = false")
    change(variables, "CHECK_UPDATES = true", "CHECK_UPDATES = false")
    change(variables, "SUPPORTS_PASSKEYS = Build.VERSION.SDK_INT >= 34", "SUPPORTS_PASSKEYS = false")
    add_after_opening(variables, r"public static synchronized void loadLogsEnabled\(Context context\)",
                      f"        if ({JAVA_PREFIX}TgMaxRuntime.enabled()) {{ LOGS_ENABLED = false; return; }}\n")
    manifest = target / "TMessagesProj/src/main/AndroidManifest.xml"
    text = manifest.read_text()
    text = re.sub(r'android:allowBackup="[^"]*"', 'android:allowBackup="false"', text)
    text = text.replace("<application", '<application android:allowBackup="false"', 1) if 'android:allowBackup=' not in text else text
    manifest.write_text(text)
    values = target / "TMessagesProj/src/main/res/values/strings.xml"
    text = values.read_text()
    text = re.sub(r'(<string name="AppName"[^>]*>).*?(</string>)', r'\g<1>MAXGRAM CLIENT\2', text)
    values.write_text(text)


def patch_password_login(target):
    path = target / "TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java"
    text = path.read_text()
    runtime = JAVA_PREFIX + "TgMaxRuntime"
    pattern = r'(if \((?:error\.text|err|errorText)[^\n]*contains\("SESSION_PASSWORD_NEEDED"\)\) \{)'
    def route(match):
        return match[0] + f'''
                if ({runtime}.get(currentAccount).needsPassword()) {{
                    showDoneButton(false, true);
                    Bundle maxParams = new Bundle();
                    maxParams.putBoolean("max_password", true);
                    setPage(VIEW_PASSWORD, true, maxParams, false);
                    return;
                }}'''
    text, count = re.subn(pattern, route, text)
    if count < 3:
        raise RuntimeError("Password routing anchors changed")
    start = text.index("public class LoginActivityPasswordView")
    end = text.index("public class ", start + 20)
    section = text[start:end]
    anchor = "            if (params.isEmpty()) {"
    assert section.count(anchor) == 1
    section = section.replace(anchor, f'''            if ({runtime}.enabled()) {{
                currentParams = params;
                nextPressed = false;
                codeField.setText("");
                codeField.setHint({runtime}.get(currentAccount).passwordHint());
                cancelButton.setVisibility(GONE);
                return;
            }}
{anchor}''')
    anchor = "            if (currentPassword == null) {"
    assert section.count(anchor) == 1
    section = section.replace(anchor, f'''            if ({runtime}.enabled()) {{
                String maxPassword = codeField.getText().toString();
                if (maxPassword.isEmpty()) {{ onPasscodeError(false); return; }}
                nextPressed = true;
                needShowProgress(0);
                {runtime}.get(currentAccount).checkPassword(maxPassword).whenComplete((auth, failure) ->
                    AndroidUtilities.runOnUIThread(() -> {{
                        nextPressed = false;
                        needHideProgress(false);
                        if (getParentActivity() == null || currentViewNum != VIEW_PASSWORD) return;
                        if (failure == null) {{
                            codeField.setText("");
                            AndroidUtilities.hideKeyboard(codeField);
                            onAuthSuccess(auth);
                        }} else {{
                            onPasscodeError(true);
                            needShowAlert(getString(R.string.ErrorOccurred), {runtime}.errorText(failure));
                        }}
                    }}));
                return;
            }}
{anchor}''')
    path.write_text(text[:start] + section + text[end:])


def isolate_client_services(target):
    java = target / "TMessagesProj/src/main/java"
    runtime = JAVA_PREFIX + "TgMaxRuntime"
    loader = java / "org/telegram/messenger/ApplicationLoader.java"
    change(loader, '        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!NaConfig.INSTANCE.getDisableCrashlyticsCollection().Bool());',
           f'        if (!{runtime}.enabled()) FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);')
    add_after_opening(loader, r"private static void initPushServices\(\)", f"        if ({runtime}.enabled()) return;\n")
    add_after_opening(loader, r"private static void startPushServiceInternal\(\)", f"        if ({runtime}.enabled()) return;\n")
    launch = java / "org/telegram/ui/LaunchActivity.java"
    add_after_opening(launch, r"private BaseFragment getClientNotActivatedFragment\(\)",
                      f"        if ({runtime}.enabled()) return new LoginActivity();\n")
    drawer = java / "org/telegram/ui/Adapters/DrawerLayoutAdapter.java"
    add_after_opening(drawer, r"public static Item buildItemForId\(int id\)",
                      f"        if ({runtime}.enabled() && (id == nkbtnQrLogin || id == 15 || id == nkbtnFeed)) return null;\n")


def restrict_max_ui(target):
    java = target / "TMessagesProj/src/main/java/org/telegram"
    ui = java / "ui"
    phone = ui / "LoginActivity.java"
    src = phone.read_text()
    start = src.index("        moreButtonView.setIcon(R.drawable.ic_ab_other);")
    end = src.index("        sizeNotifierFrameLayout.addView(moreButtonView", start)
    src = src[:start] + """        moreButtonView.setIcon(R.drawable.maxgram_help);
        moreButtonView.setContentDescription(getString(R.string.TgMaxHelp));
        moreButtonView.setOnClickListener(view -> org.telegram.messenger.browser.Browser.openUrl(getParentActivity(), "https://max.ru/support"));
""" + src[end:]
    phone.write_text(src)
    change(phone, "moreButtonView.setVisibility(activityMode == MODE_LOGIN && currentViewNum == VIEW_PHONE_INPUT ? View.VISIBLE : View.GONE);",
           "moreButtonView.setVisibility(activityMode == MODE_LOGIN && currentViewNum == VIEW_PHONE_INPUT ? View.VISIBLE : View.GONE); updateMaxLoginControls();", 3)
    runtime = JAVA_PREFIX + "TgMaxRuntime"
    add_after_opening(phone, r"public void fillNumber\(\)", f"        if ({runtime}.enabled()) return;\n")
    add_after_opening(phone, r"public void setPage\(@ViewNumber int page, boolean animated, Bundle params, boolean back\)",
                      "        if (page == VIEW_QR_LOGIN) return;\n")
    anchor = "            TelephonyManager tm = "
    src = phone.read_text()
    start = src.index("public void onNextPressed(String code)", src.index("public class PhoneView"))
    pos = src.index(anchor, start)
    src = src[:pos] + f"""            if ({runtime}.enabled() && activityMode == MODE_LOGIN) {{
                String digits = PhoneFormat.stripExceptNumbers(codeField.getText().toString())
                        + PhoneFormat.stripExceptNumbers(phoneField.getText().toString());
                if (codeField.length() == 0 || phoneField.length() == 0 || currentCountry == null || !digits.matches("[1-9][0-9]{{7,14}}")) {{
                    onFieldError(phoneOutlineView, false);
                    return;
                }}
                {runtime}.get(currentAccount).preparePhoneAuth(android.webkit.WebSettings.getDefaultUserAgent(getParentActivity()));
                Bundle params = new Bundle();
                params.putString("phoneFormated", digits);
                params.putString("phone", "+" + digits);
                params.putString("ephone", "+" + digits);
                if (currentCountry != null) params.putString("country", currentCountry.code);
                TLRPC.TL_auth_sendCode request = new TLRPC.TL_auth_sendCode();
                request.phone_number = "+" + digits;
                nextPressed = true;
                int token = ConnectionsManager.getInstance(currentAccount).sendRequest(request, (response, error) ->
                        AndroidUtilities.runOnUIThread(() -> {{
                            nextPressed = false;
                            needHideProgress(false);
                            if (getParentActivity() == null || currentViewNum != VIEW_PHONE_INPUT) return;
                            if (error != null && "MAX_CAPTCHA_CANCELLED".equals(error.text)) return;
                            if (error == null && response instanceof TLRPC.TL_auth_sentCode) {{
                                fillNextCodeParams(params, (TLRPC.auth_SentCode) response);
                            }} else {{
                                needShowAlert(getString(R.string.ErrorOccurred), error == null
                                        ? getString(R.string.ErrorOccurred) : maxAuthError(error.text));
                            }}
                        }}));
                needShowProgress(token);
                return;
            }}
""" + src[pos:]
    phone.write_text(src)
    data = ui / "DataSettingsActivity.java"
    for row in ("proxySectionRow", "proxyRow", "proxySection2Row"):
        change(data, row + " = rowCount++;", row + " = -1;")
    change(ui / "DialogsActivity.java", "final boolean proxyVisible = !SharedConfig.proxyList.isEmpty();",
           "final boolean proxyVisible = false;")
    profile = ui / "ProfileActivity.java"
    source = profile.read_text()
    lines = [line for line in source.splitlines(keepends=True) if 'new SearchResult(220,' in line and 'ProxyListActivity' in line]
    if len(lines) != 1: raise RuntimeError("Missing proxy search item")
    change(profile, lines[0], "")
    add_after_opening(java / "messenger/AndroidUtilities.java", r"public static void showProxyAlert\(Activity activity, final ProxySettings settings\)",
                      f"        if ({runtime}.enabled()) return;\n")
    add_after_opening(ui / "ProxyListActivity.java", r"public boolean onFragmentCreate\(\)",
                      f"        if ({runtime}.enabled()) return false;\n")
    change(ui / "ProxySettingsActivity.java", "public class ProxySettingsActivity extends BaseFragment {",
           "public class ProxySettingsActivity extends BaseFragment {\n    @Override public boolean onFragmentCreate() { return false; }")
    for row in ("nekoRow", "nekoSectionRow", "filtersRow"):
        change(profile, row + " = rowCount++;", row + " = -1;")
    change(profile, "boolean hidePremium = NaConfig.INSTANCE.getHidePremiumSection().Bool();", "boolean hidePremium = true;")
    change(profile, "boolean hideHelp = NaConfig.INSTANCE.getHideHelpSection().Bool();", "boolean hideHelp = true;")
    anchor = "            ArrayList<SettingsSearchResult> nagramSettings = SettingsHelper.onCreateSearchArray("
    change(profile, anchor, f"""            if ({runtime}.enabled()) {{
                return Arrays.stream(arr).filter(item -> item != null && (item.guid == 108 || item.guid == 110 ||
                        item.guid == 502 || item.guid == 400 || item.guid == 300 || item.guid == 301 ||
                        item.guid == 900 || (item.guid >= 202 && item.guid <= 205)))
                        .toArray(SearchResult[]::new);
            }}
{anchor}""")
    add_after_opening(profile, r"public void loadFaqWebPage\(\)", f"        if ({runtime}.enabled()) return;\n")
    drawer = ui / "Adapters/DrawerLayoutAdapter.java"
    change(drawer, "(id == nkbtnQrLogin || id == 15 || id == nkbtnFeed)",
           "(id != 16 && id != 6 && id != 8 && id != nkbtnSessions && id != nkbtnRestartApp && id != nkbtnMainTabsCustomize && id != nkbtnBrowser && id != nkbtnArchivedChats)")


def configure_phone_login(target):
    ui = target / "TMessagesProj/src/main/java/org/telegram/ui"
    login = ui / "LoginActivity.java"
    rows = json.loads((ROOT / "integrations/nagram/login-countries.json").read_text())
    if len(rows) != 44 or len({item["iso"] for item in rows}) != 44:
        raise RuntimeError("Expected the 44 supported login countries")
    asset = target / "TMessagesProj/src/main/assets/countries.txt"
    formats = {}
    for line in asset.read_text().splitlines():
        fields = line.split(";")
        if len(fields) > 3: formats[fields[1]] = fields[3]
    asset.write_text("\n".join(";".join([item["code"], item["iso"], item["name"],
                     "XXX XXX XXXX" if item["code"] == "1" else formats.get(item["iso"], "")]) for item in rows) + "\n")
    change(login, "countriesArray.add(0, countryWithCode);", "countriesArray.add(countryWithCode);")
    change(login, "Collections.sort(countriesArray, Comparator.comparing(o -> o.name));", "")
    change(login, 'if (!TextUtils.equals(countryWithCode.code, "FT")) {',
           'if (!LocaleController.getLocaleStringIso639().startsWith("ru")) {')
    source = login.read_text()
    first = source.index("            String country = null;", source.index('open("countries.txt")'))
    last = source.index("            if (codeField.length() == 0) {\n                setCountryButtonText(null);", first)
    source = source[:first] + source[last:]
    source = source.replace("country = list.get(list.size() - 1);", "country = list.get(0);")
    phone_start = source.index("public class PhoneView")
    phone_end = source.index("public class LoginActivitySmsView", phone_start)
    phone = source[phone_start:phone_end]
    phone = phone.replace("            setGravity(Gravity.CENTER);",
                          "            setGravity(Gravity.CENTER);\n            setPadding(0, dp(32), 0, dp(80));", 1)
    phone = phone.replace("                syncContactsBox = new CheckBoxCell(context, 2);",
                          "                syncContactsBox = new CheckBoxCell(context, 2);\n                syncContacts = false;\n                syncContactsBox.setVisibility(View.GONE);", 1)
    source = source[:phone_start] + phone + source[phone_end:]
    login.write_text(source)
    runtime = JAVA_PREFIX + "TgMaxRuntime"
    add_after_opening(login, r"private void loadCountries\(\)", f"        if ({runtime}.enabled()) return;\n")
    selector = ui / "CountrySelectActivity.java"
    add_after_opening(selector, r"public CountryAdapter\(Context context, ArrayList<Country> exisitingCountries, boolean disableAnonymousNumbers\)", f"""            if ({runtime}.enabled()) {{
                mContext = context;
                ArrayList<Country> entries = new ArrayList<>();
                if (exisitingCountries != null) entries.addAll(exisitingCountries);
                else {{
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            ApplicationLoader.applicationContext.getAssets().open("countries.txt"), java.nio.charset.StandardCharsets.UTF_8))) {{
                        String line;
                        while ((line = reader.readLine()) != null) {{
                            String[] columns = line.split(";");
                            Country country = new Country();
                            country.code = columns[0]; country.shortname = columns[1]; country.name = columns[2];
                            if (!LocaleController.getLocaleStringIso639().startsWith("ru"))
                                country.name = LocaleController.getCountryName(country.shortname);
                            entries.add(country);
                        }}
                    }} catch (Exception error) {{ throw new IllegalStateException("Missing MAX countries", error); }}
                }}
                countries.put("", entries);
                sortedCountries.add("");
                return;
            }}
""")
    hooks = (ROOT / "integrations/nagram/login-hooks.java.txt").read_text()
    add_after_opening(login, r"public class LoginActivity extends BaseFragment[^{{]*", hooks)
    add_after_opening(login, r"public View createView\(Context context\)", f"""        {runtime}.get(currentAccount).setCaptchaHandler(maxCaptchaHandler);
""")
    change(login, '        moreButtonView = new ActionBarMenuItem(', '        setupMaxLoginControls(context);\n        moreButtonView = new ActionBarMenuItem(')
    change(login, 'floatingButton.setContentDescription(getString(R.string.Done));',
           'floatingButton.setContentDescription(getString(R.string.Continue));')
    change(login, '                super.onMeasure(widthMeasureSpec, heightMeasureSpec);\n            }\n        };\n        sizeNotifierFrameLayout.setDelegate',
           '                layoutMaxLoginControls(keyboardOffset, statusBarHeight);\n                super.onMeasure(widthMeasureSpec, heightMeasureSpec);\n            }\n        };\n        sizeNotifierFrameLayout.setDelegate')
    add_after_opening(login, r"private void fillNextCodeParams\(Bundle params, TLRPC.auth_SentCode res, boolean animate\)",
        f"""        if (res instanceof {runtime}.CallSentCode) {{
            showDoneButton(false, true);
            AndroidUtilities.hideKeyboard(fragmentView);
            presentFragment(new {JAVA_PREFIX}TgMaxCallActivity(currentAccount, params.getString("ephone", ""),
                    (({runtime}.CallSentCode) res).call, (result, failure) -> handleMaxVerification(params, result, failure)));
            return;
        }}
""")
    add_after_opening(login, r"public void onFragmentDestroy\(\)", f"""        {runtime}.get(currentAccount).clearCaptchaHandler(maxCaptchaHandler);
        if (maxCaptchaDialog != null) maxCaptchaDialog.dismiss();
""")


def remove_native_transport(target):
    jni = target / "TMessagesProj/jni"
    cmake = jni / "CMakeLists.txt"
    source = cmake.read_text()
    start = source.index("add_library(tgnet STATIC")
    end = source.index("target_compile_options(tgnet", start)
    source = source[:start] + """add_library(tgmax_buffers STATIC
        openssl/aes_ige.c
        tgnet/BuffersStorage.cpp
        tgnet/ByteArray.cpp
        tgnet/FileLog.cpp
        tgnet/NativeByteBuffer.cpp)
""" + source[end:]
    source = re.sub(r"\btgnet(?= PUBLIC| PROPERTIES|\n)", "tgmax_buffers", source)
    cmake.write_text(source.replace("libtgnet.a", "libtgmax_buffers.a"))
    change(cmake, "        tests.cpp\n", "")

    bridge = jni / "TgNetWrapper.cpp"
    original = bridge.read_text()
    buffers = original[original.index("jlong getFreeBuffer("):original.index("jlong getCurrentTimeMillis(")]
    bridge.write_text("""#include <jni.h>
#include "tgnet/BuffersStorage.h"
#include "tgnet/NativeByteBuffer.h"

JavaVM *javaVm = nullptr;
jclass jclass_ByteBuffer = nullptr;
jmethodID jclass_ByteBuffer_allocateDirect = nullptr;

""" + buffers + """
extern "C" int registerNativeBuffers(JavaVM *vm, JNIEnv *env) {
    javaVm = vm;
    jclass clazz = env->FindClass(NativeByteBufferClassPathName);
    if (clazz == nullptr) return JNI_FALSE;
    jint result = env->RegisterNatives(clazz, NativeByteBufferMethods,
            sizeof(NativeByteBufferMethods) / sizeof(NativeByteBufferMethods[0]));
    env->DeleteLocalRef(clazz);
    return result == JNI_OK ? JNI_TRUE : JNI_FALSE;
}
""")
    change(jni / "jni.c", "registerNativeTgNetFunctions", "registerNativeBuffers", 2)
    manager = target / "TMessagesProj/src/main/java/org/telegram/tgnet/ConnectionsManager.java"
    change(manager, "public static native void native_setJava(boolean useJavaByteBuffers);",
           "public static void native_setJava(boolean useJavaByteBuffers) {}")
    change(manager, "public static native boolean native_isGoodPrime(byte[] prime, int g);",
           "public static boolean native_isGoodPrime(byte[] prime, int g) { return false; }")
    manager.write_text(re.sub(r"(?m)^\s*public static native [^;\n]+;", "", manager.read_text()))


def verify_guards(target, native_names):
    root = target / "TMessagesProj/src"
    for path in root.rglob("*"):
        if path.suffix not in (".java", ".kt"):
            continue
        text = path.read_text()
        for name in native_names:
            if path.name == "ConnectionsManager.java":
                text_without_declarations = re.sub(r"public static native [^;]+;", "", text)
                if re.search(r"\b" + name + r"\s*\(", text_without_declarations):
                    raise RuntimeError(f"Unprotected native call: {name}")
            elif re.search(r"\b" + name + r"\s*\(", text):
                raise RuntimeError(f"Unprotected native call in {path}")


def install_max_assets(target):
    import base64
    import gzip
    resources = target / "TMessagesProj/src/main/res"
    source = ROOT / "integrations/nagram/resources"
    shutil.copytree(source, resources, dirs_exist_ok=True)
    for path in resources.rglob("*.b64"):
        path.with_suffix("").write_bytes(base64.b64decode(path.read_text()))
        path.unlink()
    for path in resources.rglob("*.json.gz"):
        path.with_suffix("").write_bytes(gzip.decompress(path.read_bytes()))
        path.unlink()
    manifest = target / "TMessagesProj/src/main/AndroidManifest.xml"
    text = re.sub(r'android:(icon|roundIcon)="@mipmap/[^"]+"',
                  lambda m: 'android:' + m[1] + '="@mipmap/maxgram_launcher"', manifest.read_text())
    manifest.write_text(text)
    for style in resources.rglob("styles.xml"):
        text = style.read_text()
        text = re.sub(r'(<item name="android:windowSplashScreenAnimatedIcon">)[^<]+(</item>)',
                      r'\g<1>@mipmap/maxgram_launcher\2', text)
        style.write_text(text)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=ROOT / "upstream/nagramxf")
    parser.add_argument("--output", type=Path, default=ROOT / "build/nagram-client")
    args = parser.parse_args()
    source, output = args.source.resolve(), args.output.resolve()
    lock = json.loads((ROOT / "upstream.lock.json").read_text())["sources"]["nagramxf"]
    if not (source / ".git").exists():
        raise SystemExit("Initialize upstream/nagramxf first: git submodule update --init --depth 1 upstream/nagramxf")
    if run("git", "rev-parse", "HEAD", cwd=source) != lock["commit"]:
        raise SystemExit("NagramXF checkout does not match upstream.lock.json")
    if output.exists():
        raise SystemExit("Output already exists. Choose a new --output; existing edits were preserved.")
    output.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["git", "clone", "--no-hardlinks", "--no-checkout", str(source), str(output)], check=True)
    subprocess.run(["git", "checkout", "--detach", lock["commit"]], cwd=output, check=True)
    native_names = guard_mtproto(output)
    remove_native_transport(output)
    removed = restrict_languages(output)
    configure_build(output)
    patch_password_login(output)
    isolate_client_services(output)
    restrict_max_ui(output)
    configure_phone_login(output)
    shutil.copytree(ROOT / "integrations/nagram/src/main/java", output / "TMessagesProj/src/main/java", dirs_exist_ok=True)
    shutil.copytree(ROOT / "integrations/nagram/src/main/assets", output / "TMessagesProj/src/main/assets", dirs_exist_ok=True)
    for helper in (ROOT / "integrations/nagram/ui").glob("*.java"):
        shutil.copy2(helper, output / "TMessagesProj/src/main/java/io/github/lubara7/tgmax/nagram" / helper.name)
    install_max_assets(output)
    verify_guards(output, native_names)
    report = {"nagram_commit": lock["commit"], "network_entrypoints_guarded": len(native_names),
              "removed_locale_directories": removed, "native_apk_verified": False}
    (output / "TGMAX_CHANGES.json").write_text(json.dumps(report, indent=2) + "\n")
    (output / "TGMAX_CHANGES.md").write_text("MAXGRAM CLIENT modifies this NagramXF checkout to use the Komet-derived MAX backend.\nSee the parent project's SOURCE_PROVENANCE.md and docs/nagram-migration.md.\nUpstream copyright and GPL notices are retained.\n")
    print(json.dumps({"client_directory": str(output), **report}, indent=2))


if __name__ == "__main__":
    main()
