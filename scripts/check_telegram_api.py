#!/usr/bin/env python3
import argparse
import json
import os
import re
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def mask_java(text):
    pattern = r'//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\''
    return re.sub(pattern, lambda m: "".join("\n" if c == "\n" else " " for c in m[0]), text)


def end_brace(text, opening):
    depth = 0
    for i in range(opening, len(text)):
        depth += (text[i] == "{") - (text[i] == "}")
        if depth == 0:
            return i
    raise RuntimeError("Unbalanced upstream Java source")


def classes(text):
    text = mask_java(text)
    result = {}
    for match in re.finditer(r"public\s+static\s+(?:(?:abstract|final)\s+)*class\s+(\w+)(?:\s+extends\s+([\w.]+))?[^\{]*\{", text):
        opening = match.end() - 1
        body = text[opening + 1:end_brace(text, opening)]
        fields = {}
        depth, start = 0, 0
        for i, char in enumerate(body):
            if char == "{": depth += 1
            elif char == "}":
                depth -= 1
                if depth == 0: start = i + 1
            elif char == ";" and depth == 0:
                statement = body[start:i].strip()
                start = i + 1
                statement = re.sub(r"@\w+(?:\([^)]*\))?", "", statement).strip()
                statement = statement.split("=", 1)[0].strip()
                field = re.fullmatch(r"public\s+(?:(?:static|final|volatile|transient)\s+)*([\w.<>?,\[\] ]+?)\s+(\w+)", statement)
                if field: fields[field[2]] = field[1]
        result[match[1]] = {"base": match[2] or "TLObject", "fields": fields}
    return result


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=ROOT / "upstream/telegram")
    parser.add_argument("--android-jar", type=Path)
    parser.add_argument("--output", type=Path, default=ROOT / "build/telegram-api-check")
    args = parser.parse_args()
    source = args.source.resolve()
    lock = json.loads((ROOT / "upstream.lock.json").read_text())["sources"]["telegram"]
    actual = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=source, text=True).strip()
    if actual != lock["commit"]: raise SystemExit("Telegram pin mismatch")
    java = source / "TMessagesProj/src/main/java"
    files = list((ROOT / "integrations/telegram/src/main/java").rglob("*.java"))
    adapter = "\n".join(p.read_text() for p in files)
    needed_fields = set(re.findall(r"\.(\w+)", adapter))
    namespaces = [(name, "org/telegram/tgnet/" + path + name + ".java") for name, path in
                  [("TLRPC", ""), ("TL_update", "tl/"), ("TL_account", "tl/")]]
    api = {}
    for namespace, relative in namespaces:
        for name, shape in classes((java / relative).read_text()).items():
            api[namespace + "." + name] = {**shape, "namespace": namespace}
    needed = set(m[0] for m in re.finditer(r"(?:TLRPC|TL_update|TL_account)\.\w+", adapter))
    selected = {}
    queue = list(needed)
    while queue:
        qualified = queue.pop()
        if qualified in selected: continue
        if qualified not in api: raise SystemExit(f"Telegram has no {qualified}")
        shape = api[qualified]
        fields = {k: v for k, v in shape["fields"].items() if k in needed_fields}
        selected[qualified] = {**shape, "fields": fields}
        for match in re.finditer(r"(?:(TLRPC|TL_update|TL_account)\.)?(\w+)", " ".join([shape["base"], *fields.values()])):
            candidate = (match[1] or shape["namespace"]) + "." + match[2]
            if candidate in api and candidate not in selected: queue.append(candidate)
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    stubs = output / "api-shapes"
    for namespace, relative in namespaces:
        declarations = []
        for qualified, shape in selected.items():
            if shape["namespace"] != namespace: continue
            name = qualified.split(".")[1]
            fields = "\n".join(f"public {t} {n};" for n, t in shape["fields"].items())
            declarations.append(f"public static class {name} extends {shape['base']} {{\n{fields}\n}}")
        package = "org.telegram.tgnet" if namespace == "TLRPC" else "org.telegram.tgnet.tl"
        write(stubs / relative, f"package {package}; import java.util.*; import org.telegram.tgnet.*; public class {namespace} {{\n" + "\n".join(declarations) + "\n}\n")
    vector = (java / "org/telegram/tgnet/Vector.java").read_text()
    if "public Vector(TLDeserializer<T> itemDeserializer)" not in vector or "ArrayList<T> objects" not in vector:
        raise SystemExit("Vector API changed")
    write(stubs / "org/telegram/tgnet/Vector.java", "package org.telegram.tgnet; import java.util.*; public class Vector<T extends TLObject> extends TLObject { public ArrayList<T> objects; public Vector(Object deserializer) {} }\n")
    write(stubs / "org/telegram/tgnet/TLObject.java", "package org.telegram.tgnet; public class TLObject {}\n")
    method = (java / "org/telegram/tgnet/TLMethod.java").read_text()
    if "extends TLObject" not in method: raise SystemExit("TLMethod hierarchy changed")
    write(stubs / "org/telegram/tgnet/TLMethod.java", "package org.telegram.tgnet; public class TLMethod<T extends TLObject> extends TLObject {}\n")
    for name in ["RequestDelegate", "RequestDelegateTimestamp", "WriteToSocketDelegate", "QuickAckDelegate"]:
        write(stubs / f"org/telegram/tgnet/{name}.java", (java / f"org/telegram/tgnet/{name}.java").read_text())
    connection = (java / "org/telegram/tgnet/ConnectionsManager.java").read_text()
    if "void onConnectionStateChanged(final int state, final int currentAccount)" not in connection:
        raise SystemExit("ConnectionsManager state callback changed")
    write(stubs / "org/telegram/tgnet/ConnectionsManager.java",
          "package org.telegram.tgnet; public class ConnectionsManager { public static void onConnectionStateChanged(int state,int account) {} }\n")
    controller = (java / "org/telegram/messenger/MessagesController.java").read_text()
    if not re.search(r"void processUpdates\([^)]*TLRPC.Updates[^)]*boolean", controller):
        raise SystemExit("MessagesController update callback changed")
    write(stubs / "org/telegram/messenger/MessagesController.java", """package org.telegram.messenger;
public class MessagesController {
 public static MessagesController getInstance(int account) {return null;}
 public void processUpdates(org.telegram.tgnet.TLRPC.Updates updates,boolean fromQueue) {}
}
""")
    write(stubs / "org/telegram/messenger/ApplicationLoader.java",
          "package org.telegram.messenger; public class ApplicationLoader { public static android.content.Context applicationContext; }\n")
    write(stubs / "org/telegram/messenger/Utilities.java", """package org.telegram.messenger;
public class Utilities { public static DispatchQueue stageQueue;
 public static class DispatchQueue { public boolean postRunnable(Runnable runnable) {return false;} }
}
""")
    locale_source = (java / "org/telegram/messenger/LocaleController.java").read_text()
    locale = classes(locale_source)["LocaleInfo"]["fields"]
    locale_names = ["name", "nameEnglish", "shortName", "pluralLangCode", "builtIn"]
    for name in locale_names:
        if name not in locale: raise SystemExit(f"LocaleInfo.{name} missing")
    locale_fields = "\n".join(f"public {locale[name]} {name};" for name in locale_names)
    write(stubs / "org/telegram/messenger/LocaleController.java", """package org.telegram.messenger;
import java.util.*;
public class LocaleController {
 public static String getString(int id) {return "";}
 public static String getLocaleStringIso639() {return "en";}
 public static LocaleController getInstance() {return null;}
 public Locale getCurrentLocale() {return Locale.ENGLISH;}
 public ArrayList<LocaleInfo> languages,remoteLanguages,unofficialLanguages;
 public HashMap<String,LocaleInfo> languagesDict,remoteLanguagesDict;
 public static class LocaleInfo {
""" + locale_fields + "\n}\n}\n")
    write(stubs / "org/telegram/messenger/R.java", "package org.telegram.messenger; public class R { public static class string { public static final int TgMaxUnknownUser=1; } }\n")
    android = args.android_jar
    if android is None:
        sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
        if not sdk: raise SystemExit("Set ANDROID_HOME or pass --android-jar")
        android = Path(sdk) / "platforms/android-36/android.jar"
    classpath = (ROOT / "max-backend/build/adapter-classpath.txt").read_text().strip()
    compiler = str(Path(os.environ["JAVA_HOME"]) / "bin/javac") if os.environ.get("JAVA_HOME") else "javac"
    command = [compiler, "-encoding", "UTF-8", "-source", "17", "-target", "17", "-cp", str(android) + os.pathsep + classpath,
               "-d", str(output / "classes"), *map(str, stubs.rglob("*.java")), *map(str, files)]
    subprocess.run(command, check=True)
    print(json.dumps({"pinned_api_classes_checked": len(selected), "integration_sources_compiled": len(files),
                      "full_telegram_apk_built": False}, indent=2))


if __name__ == "__main__":
    main()
