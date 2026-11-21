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


def bounds(node):
    values = [int(value) for value in re.findall(r'\d+', node.get('bounds', ''))]
    if len(values) != 4:
        raise RuntimeError('Missing UI bounds')
    return values


def check_keyboard(tree, output):
    field = next(node for node in tree.iter('node') if node.get('class') == 'android.widget.EditText')
    left, top, right, bottom = bounds(field)
    adb('shell', 'settings', 'put', 'secure', 'show_ime_with_hard_keyboard', '1')
    adb('shell', 'input', 'tap', str((left + right) // 2), str((top + bottom) // 2))
    adb('shell', 'input', 'text', '0000000000')
    try:
        for _ in range(8):
            time.sleep(1)
            try:
                keyboard_tree = capture(output)
            except (subprocess.SubprocessError, ET.ParseError):
                continue
            nodes = list(keyboard_tree.iter('node'))
            next_button = next((node for node in nodes if label(node).strip() in {'Continue', 'Продолжить'}), None)
            windows = adb('shell', 'dumpsys', 'window', 'windows')
            (output / 'window-keyboard.txt').write_text(windows)
            (output / 'input-method.txt').write_text(adb('shell', 'dumpsys', 'input_method'))
            ime_top = None
            for line in windows.splitlines():
                if not ('ITYPE_IME' in line or 'type=ime' in line) or 'visible=true' not in line.lower():
                    continue
                frame = re.search(r'(?:mFrame|\bframe)=\[(\d+),(\d+)\]\[(\d+),(\d+)\]', line)
                if frame and int(frame[2]) > 0:
                    ime_top = int(frame[2])
                    break
            for block in windows.split('Window #')[1:]:
                if ime_top is not None:
                    break
                header = block.splitlines()[0]
                if 'InputMethod' not in header and 'inputmethod' not in header:
                    continue
                frame = re.search(r'(?:mFrame|\bframe)=\[(\d+),(\d+)\]\[(\d+),(\d+)\]', block)
                if frame and int(frame[2]) > 0:
                    ime_top = int(frame[2])
                    break
            if ime_top is None:
                tops = [bounds(node)[1] for node in nodes if 'inputmethod' in node.get('package', '')]
                ime_top = min(tops) if tops else None
            if next_button is not None and ime_top is not None and ime_top > 0:
                button_bottom = bounds(next_button)[3]
                if button_bottom > ime_top:
                    raise RuntimeError(f'Continue is covered by the software keyboard: buttonBottom={button_bottom}, keyboardTop={ime_top}')
                return {'keyboardTop': ime_top, 'continueBottom': button_bottom, 'continueAboveKeyboard': True}
        raise RuntimeError('Could not verify the continue button with the keyboard open')
    finally:
        (output / 'phone-keyboard.png').write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
        adb('shell', 'input', 'keyevent', *(['67'] * 20))
        adb('shell', 'input', 'keyevent', '4')


def main():
    parser = argparse.ArgumentParser()
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument('--apk', type=Path)
    source.add_argument('--artifacts', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.artifacts is not None:
        provenance = json.loads((args.artifacts / 'build-provenance.json').read_text())
        artifact = next(item for item in provenance['artifacts'] if item['abi'] == 'x86_64')
        args.apk = args.artifacts / artifact['name']
        if hashlib.sha256(args.apk.read_bytes()).hexdigest() != artifact['sha256']:
            raise RuntimeError('Emulator APK hash does not match build provenance')
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
        labels = [label(node).strip() for node in nodes]
        country = any('🇷🇺' in text or 'Россия' in text or 'Russian Federation' in text for text in labels)
        help_button = any(text in {'Help', 'Помощь'} for text in labels)
        language_button = any(text in {'Language', 'Язык'} for text in labels)
        if not country or not help_button or not language_button:
            raise RuntimeError('The default country or login toolbar controls were not observed')
        keyboard = check_keyboard(tree, output)
        process = adb('shell', 'pidof', PACKAGE).strip()
        if not process:
            raise RuntimeError('Preview process is not running')
        result = {
            'applicationId': PACKAGE,
            'apkSha256': hashlib.sha256(args.apk.read_bytes()).hexdigest(),
            'disclaimerObserved': disclaimer,
            'phoneLoginObserved': login,
            'defaultRussiaObserved': country,
            'helpControlObserved': help_button,
            'languageControlObserved': language_button,
            'keyboard': keyboard,
            'realAccountLoginAttempted': False,
        }
        (output / 'startup.json').write_text(json.dumps(result, indent=2) + '\n')
        print(json.dumps(result, indent=2))
    finally:
        (output / 'login.png').write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
        (output / 'logcat.txt').write_text(adb('logcat', '-d'))


if __name__ == '__main__':
    main()
