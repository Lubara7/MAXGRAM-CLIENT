#!/usr/bin/env python3
import argparse
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULES = [
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
    'TMessagesProj/jni/third_party/ffmpeg',
    'TMessagesProj/jni/third_party/libvpx',
    'TMessagesProj/jni/third_party/dav1d',
]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('client', type=Path, nargs='?', default=ROOT / 'build/nagram-client')
    args = parser.parse_args()
    client = args.client.resolve()
    actual = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=client, text=True).strip()
    expected = json.loads((ROOT / 'upstream.lock.json').read_text())['sources']['nagramxf']['commit']
    if actual != expected:
        raise SystemExit('Prepared client revision differs from upstream.lock.json')
    subprocess.run(['git', 'submodule', 'update', '--init', '--depth', '1', '--jobs', '4', *MODULES], cwd=client, check=True)
    for module in MODULES:
        status = subprocess.check_output(['git', 'submodule', 'status', module], cwd=client, text=True)
        if not status.startswith(' '):
            raise SystemExit('Native dependency revision mismatch: ' + module)
    print('Pinned normal-flavor dependencies initialized:', len(MODULES))


if __name__ == '__main__':
    main()
