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
            text = re.sub(r"(ConnectionsManager\.)" + name + r"\s*\(", r"\1tgmax_" + name + "(", text)
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
    anchor = "            NativeByteBuffer buffer = new NativeByteBuffer(finalRequestObject.getObjectSize());"
    change(connection, anchor, f"""            if ({runtime}.enabled()) {{
                {runtime}.get(currentAccount).send(requestToken, finalRequestObject, finalOnComplete,
                        onCompleteTimestamp, onQuickAck, onWriteToSocket);
                return;
            }}
{anchor}""")
    return names


def restrict_languages(target):
    base = target / "TMessagesProj/src/main"
    controller = base / "java/org/telegram/messenger/LocaleController.java"
    helper = JAVA_PREFIX + "TgMaxLocales"
    change(controller, "        systemDefaultLocale = Locale.getDefault();", f"        {helper}.install(this);\n        systemDefaultLocale = Locale.getDefault();")
    change(controller, "currentInfo = getLanguageFromDict(systemDefaultLocale.getLanguage());",
           f"currentInfo = getLanguageFromDict({helper}.code(systemDefaultLocale.toLanguageTag()));")
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
    login = base / "java/org/telegram/ui/LoginActivity.java"
    add_after_opening(login, r"public void saveSelfArgs\(Bundle outState\)",
                      f"        if ({JAVA_PREFIX}TgMaxRuntime.enabled()) {{ clearCurrentState(); return; }}\n")
    install_auth_translations(base / "res")
    return removed


def install_auth_translations(resources):
    mapping = {
        "language_title": ["Language"], "phone_title": ["YourPhone"],
        "phone_description": ["StartText"], "phone_label": ["PhoneNumber"],
        "action_continue": ["Next", "Continue"], "action_sign_in": ["StartMessaging"],
        "action_cancel": ["Cancel"], "action_back": ["Back"],
        "code_title": ["SentSmsCodeTitle"], "code_label": ["Code"],
        "code_description": ["SentSmsCode"], "action_resend": ["ResendCode"],
        "action_edit_phone": ["WrongNumber"], "error_invalid_phone": ["InvalidPhoneNumber"],
        "error_invalid_code": ["InvalidCode"], "error_expired_code": ["CodeExpired"],
        "error_unknown": ["ErrorOccurred"], "action_sign_out": ["LogOut"],
    }
    names = {"values": "English", "values-ru": "Русский", "values-es": "Español",
             "values-uz": "O‘zbekcha", "values-pt": "Português (Portugal)",
             "values-pt-rBR": "Português (Brasil)", "values-fr": "Français", "values-be": "Беларуская"}
    for qualifier, native_name in names.items():
        source = ROOT / "core/localization/src/main/res" / qualifier / "strings.xml"
        root = ET.parse(source).getroot()
        strings = {item.attrib["name"]: "".join(item.itertext()) for item in root.findall("string")}
        translations = {key: strings[name] for name, keys in mapping.items() for key in keys}
        translations.update(AppName="TG-MAX Ultimate", NoChats="TG-MAX Ultimate", LanguageName=native_name)
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
    change(app, "dependencies {", "dependencies {\n    implementation project(':tgmax-backend')")
    change(app, "    defaultConfig.applicationId = APP_PACKAGE", "    defaultConfig.applicationId = APP_PACKAGE\n    defaultConfig.resourceConfigurations += ['en', 'ru', 'es', 'uz', 'pt', 'pt-rBR', 'fr', 'be']")
    text = app.read_text()
    start = text.index("        // obtain your own keys")
    stop = text.index("        buildConfigField 'String', 'BUILD_VERSION_STRING'", start)
    text = text[:start] + "        def appId = '0'\n        def appHash = ''\n\n" + text[stop:]
    debug = text.index("        debug {", text.index("    buildTypes"))
    signing = text.index("signingConfig = signingConfigs.release", debug)
    text = text[:signing] + text[signing:].replace("signingConfig = signingConfigs.release", "signingConfig = signingConfigs.debug", 1)
    text = re.sub(r"def verCode = \d+", "def verCode = 2", text, count=1)
    app.write_text(text)
    settings = target / "settings.gradle"
    settings.write_text(settings.read_text() + "\ninclude ':tgmax-backend'\n")
    shutil.copytree(ROOT / "max-backend", target / "tgmax-backend", ignore=shutil.ignore_patterns("build", ".gradle"))
    properties = target / "gradle.properties"
    text = properties.read_text()
    text = re.sub(r"(?m)^APP_PACKAGE=.*$", "APP_PACKAGE=io.github.lubara7.tgmax", text)
    version = json.loads((ROOT / "release.json").read_text())["version"]
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
    text = re.sub(r'(<string name="AppName"[^>]*>).*?(</string>)', r'\g<1>TG-MAX Ultimate\2', text)
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
            elif re.search(r"ConnectionsManager\." + name + r"\s*\(", text):
                raise RuntimeError(f"Unprotected native call in {path}")


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
    removed = restrict_languages(output)
    configure_build(output)
    patch_password_login(output)
    isolate_client_services(output)
    shutil.copytree(ROOT / "integrations/nagram/src/main/java", output / "TMessagesProj/src/main/java", dirs_exist_ok=True)
    verify_guards(output, native_names)
    report = {"nagram_commit": lock["commit"], "network_entrypoints_guarded": len(native_names),
              "removed_locale_directories": removed, "native_apk_verified": False}
    (output / "TGMAX_CHANGES.json").write_text(json.dumps(report, indent=2) + "\n")
    (output / "TGMAX_CHANGES.md").write_text("TG-MAX Ultimate modifies this NagramXF checkout to use the Komet-derived MAX backend.\nSee the parent project's SOURCE_PROVENANCE.md and docs/nagram-migration.md.\nUpstream copyright and GPL notices are retained.\n")
    print(json.dumps({"client_directory": str(output), **report}, indent=2))


if __name__ == "__main__":
    main()
