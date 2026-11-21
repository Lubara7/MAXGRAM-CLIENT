import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET


PACKAGE = 'io.github.lubara7.maxgram.preview'


def adb(*args, binary=False):
    return subprocess.check_output(['adb', *args], text=not binary, timeout=45)


def capture(output):
    adb('shell', 'uiautomator', 'dump', '/sdcard/maxgram-ui.xml')
    xml = adb('exec-out', 'cat', '/sdcard/maxgram-ui.xml')
    (output / 'screen.xml').write_text(xml)
    return ET.fromstring(xml)


def label(node):
    return node.get('text', '') + ' ' + node.get('content-desc', '')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    output = args.output
    output.mkdir(parents=True, exist_ok=True)
    adb('wait-for-device')
    adb('install', '-r', str(args.apk))
    adb('logcat', '-c')
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/ru.komet.app.MainActivity')
    disclaimer = False
    login = False
    try:
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline:
            time.sleep(1)
            try:
                tree = capture(output)
            except (subprocess.SubprocessError, ET.ParseError):
                continue
            nodes = list(tree.iter('node'))
            if any('неофициальный клиент' in label(node) or 'unofficial client' in label(node) for node in nodes):
                disclaimer = True
                accept = next((node for node in nodes if label(node).strip() in {'Continue', 'Продолжить'}), None)
                if accept is not None:
                    bounds = [int(value) for value in re.findall(r'\d+', accept.get('bounds', ''))]
                    if len(bounds) == 4:
                        adb('shell', 'input', 'tap', str((bounds[0] + bounds[2]) // 2), str((bounds[1] + bounds[3]) // 2))
                        continue
            login = any('Your phone number' in label(node) or 'Ваш номер телефона' in label(node) for node in nodes)
            if login and disclaimer:
                break
        if not disclaimer or not login:
            raise RuntimeError('The disclaimer and phone login were not both observed')
        process = adb('shell', 'pidof', PACKAGE).strip()
        if not process:
            raise RuntimeError('Preview process is not running')
        result = {
            'applicationId': PACKAGE,
            'apkSha256': hashlib.sha256(args.apk.read_bytes()).hexdigest(),
            'disclaimerObserved': disclaimer,
            'phoneLoginObserved': login,
            'realAccountLoginAttempted': False,
        }
        (output / 'startup.json').write_text(json.dumps(result, indent=2) + '\n')
        print(json.dumps(result, indent=2))
    finally:
        (output / 'login.png').write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
        (output / 'logcat.txt').write_text(adb('logcat', '-d'))


if __name__ == '__main__':
    main()
