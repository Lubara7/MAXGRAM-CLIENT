import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess


ROOT = Path(__file__).resolve().parents[1]
REVISION = '21f7d109da6ab3506c9e9577f98c6a38771ab335'
REPOSITORY = 'https://github.com/TelegramMessenger/Telegram-Android'
KEYS = (
    'windowBackgroundWhite', 'windowBackgroundGray',
    'windowBackgroundWhiteBlackText', 'windowBackgroundWhiteGrayText2',
    'windowBackgroundWhiteGrayIcon', 'windowBackgroundWhiteBlueHeader',
    'windowBackgroundWhiteValueText', 'windowBackgroundWhiteBlueText',
    'actionBarDefault', 'actionBarDefaultIcon', 'actionBarDefaultTitle',
    'actionBarDefaultSubtitle', 'switchTrack', 'switchTrackChecked',
    'divider', 'chat_wallpaper', 'chat_inBubble', 'chat_outBubble',
    'chat_messageTextIn', 'chat_messageTextOut', 'chat_messagePanelBackground',
    'chat_messagePanelIcons',
)
EXTRA_ICONS = (
    'ic_ab_back', 'ic_ab_other', 'msg_search', 'msg_saved', 'msg_contacts',
    'msg_edit', 'msg_settings', 'msg_background', 'msg_theme', 'msg_block',
    'msg_calls', 'msg_help', 'msg_download', 'msg_groups_create', 'msg_send',
    'msg_camera', 'msg_add', 'msg_delete', 'msg_log_out', 'msg_mute',
    'msg_check_s', 'msg_photos', 'msg_voice_unmuted', 'input_attach', 'input_smile', 'input_mic',
    'msg_unmute', 'msg_attach', 'msg_smile', 'msg_voice',
)


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--font-source', type=Path, required=True)
    args = parser.parse_args()
    revision = subprocess.check_output(
        ['git', 'rev-parse', 'HEAD'], cwd=args.source, text=True,
    ).strip()
    if revision != REVISION:
        raise RuntimeError('The official Telegram revision does not match the UI reference')
    source = args.source / 'TMessagesProj/src/main'
    target = ROOT / 'integrations/komet/assets/telegram'
    target.mkdir(parents=True, exist_ok=True)
    entries = []

    def copy(original, relative, origin=REPOSITORY):
        destination = target / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(original, destination)
        entries.append({
            'asset': 'assets/telegram/' + relative,
            'source': str(original.relative_to(args.source)) if origin == REPOSITORY else original.name,
            'repository': origin,
            'sha256': sha256(original),
        })

    drawable = source / 'res/drawable-xxhdpi'
    names = {path.stem for path in drawable.glob('msg2_*.webp')}
    names.update(EXTRA_ICONS)
    for name in sorted(names):
        original = next((drawable / (name + suffix) for suffix in ('.webp', '.png') if (drawable / (name + suffix)).is_file()), None)
        if original is not None:
            copy(original, 'icons/' + original.name)
    copy(source / 'res/raw/default_pattern.svg', 'default_pattern.svg')
    copy(source / 'assets/fonts/rmedium.ttf', 'fonts/Roboto-Medium.ttf')
    for name in ('Roboto-Regular.ttf', 'Roboto-Bold.ttf', 'Roboto-Italic.ttf', 'Roboto_LICENSE.txt'):
        copy(args.font_source / name, 'fonts/' + name, 'https://github.com/flutter/flutter/tree/3.44.3')
    copy(args.source / 'LICENSE', 'TELEGRAM_LICENSE')

    text = (source / 'java/org/telegram/ui/ActionBar/ThemeColors.java').read_text()
    constants = {
        name: int(value, 16) for name, value in
        re.findall(r'public static final int (\w+) = (0x[0-9a-fA-F]+)', text)
    }

    def color(value):
        value = value.split(';')[0].strip()
        return constants[value] if value in constants else int(value, 0) & 0xffffffff

    defaults = {
        key: color(value) for key, value in
        re.findall(r'defaultColors\[key_(\w+)\]\s*=\s*([^;]+);', text)
        if key in KEYS
    }
    defaults.setdefault('chat_wallpaper', 0xffdbddbb)
    palettes = {'classic': defaults}
    for name, filename in (('day', 'day'), ('night', 'night'), ('tinted', 'darkblue')):
        values = dict(defaults)
        for line in (source / f'assets/{filename}.attheme').read_text(errors='ignore').splitlines():
            key, sep, value = line.partition('=')
            if sep and key in KEYS:
                values[key] = color(value)
        palettes[name] = values
        copy(source / f'assets/{filename}.attheme', f'themes/{filename}.attheme')
    generated = ROOT / 'integrations/komet/lib/maxgram/telegram_palettes.dart'
    generated.parent.mkdir(parents=True, exist_ok=True)
    generated.write_text(
        "import 'package:flutter/material.dart';\n\n"
        'const telegramPalettes = <String, Map<String, Color>>{\n' +
        ''.join("  '" + name + "': {\n" + ''.join(
            "    '" + key + "': Color(0x" + format(value, '08X') + "),\n"
            for key, value in values.items()
        ) + '  },\n' for name, values in palettes.items()) + '};\n'
    )
    manifest = {'repository': REPOSITORY, 'commit': REVISION, 'density': 'xxhdpi', 'assets': entries, 'palettes': palettes}
    (ROOT / 'integrations/komet/telegram-design.lock.json').write_text(json.dumps(manifest, indent=2) + '\n')
    (target / 'NOTICE.txt').write_text(
        'Telegram for Android UI resources and control geometry\n'
        'Copyright Telegram and the respective source-file authors.\n'
        'The Switch component credits Nikolai Kudashov, 2013-2018.\n'
        'Licensed under GNU GPL version 2 or later; see TELEGRAM_LICENSE.\n'
        'Official source: ' + REPOSITORY + '/tree/' + REVISION + '\n'
        'Original resource bytes are retained. Their source paths and SHA-256 hashes\n'
        'are recorded in integrations/komet/telegram-design.lock.json.\n'
        'Ported control geometry: Switch.java, TextCell.java, TextSettingsCell.java,\n'
        'ActionBar/MessageDrawable.java, Theme.java and jni/image.cpp.\n'
        'The static gradient is phase 0 of Telegram MotionBackgroundDrawable.\n'
        'Roboto regular/bold/italic are distributed with their Apache 2.0 notice.\n'
    )
    print(json.dumps({'referenceCommit': revision, 'importedAssets': len(entries), 'palettes': list(palettes)}))


if __name__ == '__main__':
    main()
