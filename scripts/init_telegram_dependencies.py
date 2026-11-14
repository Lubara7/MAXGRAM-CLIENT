#!/usr/bin/env python3
import argparse
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULES = [
    'TMessagesProj/lib/jlatexmath',
    'TMessagesProj/jni/twidget/twidget',
    'TMessagesProj/jni/twidget/widget-sandbox',
    'TMessagesProj/jni/wallet-engine',
    'TMessagesProj_Modules/media',
    'TMessagesProj/jni/third_party/libyuv',
    'TMessagesProj/jni/third_party/absl',
    'TMessagesProj/jni/third_party/boringssl',
    'TMessagesProj/jni/third_party/xiph/ogg',
    'TMessagesProj/jni/third_party/xiph/opus',
    'TMessagesProj/jni/third_party/xiph/opusfile',
    'TMessagesProj/jni/third_party/boost-regex',
    'TMessagesProj/jni/third_party/libprisma',
    'TMessagesProj/jni/tlottie',
    'TMessagesProj/jni/tgcalls',
    'TMessagesProj/jni/third_party/wamr',
    'TMessagesProj/jni/third_party/openh264',
    'TMessagesProj/jni/td',
    'TMessagesProj/jni/third_party/ffmpeg',
    'TMessagesProj/jni/third_party/libvpx',
    'TMessagesProj/jni/third_party/dav1d',
]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('client', type=Path, nargs='?', default=ROOT / 'build/telegram-client')
    args = parser.parse_args()
    client = args.client.resolve()
    actual = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=client, text=True).strip()
    expected = json.loads((ROOT / 'upstream.lock.json').read_text())['sources']['telegram']['commit']
    if actual != expected:
        raise SystemExit('Prepared client revision differs from upstream.lock.json')
    subprocess.run(['git', 'submodule', 'update', '--init', '--depth', '1', '--jobs', '4', *MODULES], cwd=client, check=True)
    sandbox = client / 'TMessagesProj/jni/twidget/widget-sandbox'
    quickjs_module = 'packages/ios/Sources/CQuickJS/quickjs'
    # QuickJS's test262 submodule is deliberately disabled upstream; it is not an APK input.
    subprocess.run(['git', 'submodule', 'update', '--init', '--depth', '1', quickjs_module], cwd=sandbox, check=True)
    nested = subprocess.check_output(['git', 'submodule', 'status', quickjs_module], cwd=sandbox, text=True)
    if not nested.startswith(' '):
        raise SystemExit('Pinned QuickJS revision mismatch')
    quickjs = client / 'TMessagesProj/jni/twidget/widget-sandbox/packages/ios/Sources/CQuickJS/quickjs/quickjs.c'
    if not quickjs.is_file():
        raise SystemExit('Pinned widget sandbox is missing its nested QuickJS source')
    for module in MODULES:
        status = subprocess.check_output(['git', 'submodule', 'status', module], cwd=client, text=True)
        if not status.startswith(' '):
            raise SystemExit('Native dependency revision mismatch: ' + module)
    if not (client / 'TMessagesProj/jni/third_party/wamr/core/iwasm/include/wasm_export.h').is_file():
        raise SystemExit('Pinned WAMR checkout is missing its public Android header')
    print('Pinned official Telegram dependencies initialized:', len(MODULES), 'plus QuickJS')


if __name__ == '__main__':
    main()
