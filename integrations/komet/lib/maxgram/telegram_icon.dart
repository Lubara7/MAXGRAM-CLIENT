import 'package:flutter/material.dart';
import 'package:material_symbols_icons/symbols.dart';

class TelegramIcon extends StatelessWidget {
  const TelegramIcon(
    this.icon, {
    super.key,
    this.color,
    this.size,
    this.weight,
    this.fill,
    this.opticalSize,
    this.semanticLabel,
    this.textDirection,
    this.shadows,
  });

  final IconData? icon;
  final Color? color;
  final double? size;
  final double? weight;
  final double? fill;
  final double? opticalSize;
  final String? semanticLabel;
  final TextDirection? textDirection;
  final List<Shadow>? shadows;

  static final assets = <IconData, String>{
    Symbols.notifications: 'msg2_notifications',
    Symbols.notifications_active: 'msg2_notifications',
    Symbols.lock: 'msg2_secret',
    Symbols.shield: 'msg2_secret',
    Symbols.shield_lock: 'msg2_secret',
    Symbols.security: 'msg2_secret',
    Symbols.database: 'msg2_data',
    Symbols.storage: 'msg2_data',
    Symbols.data_usage: 'msg2_data',
    Symbols.chat: 'msg2_discussion',
    Symbols.chat_bubble: 'msg2_discussion',
    Symbols.forum: 'msg2_discussion',
    Symbols.folder: 'msg2_folder',
    Symbols.folder_open: 'msg2_folder',
    Symbols.devices: 'msg2_devices',
    Symbols.language: 'msg2_language',
    Symbols.translate: 'msg2_language',
    Symbols.battery_saver: 'msg2_battery',
    Symbols.battery_full: 'msg2_battery',
    Symbols.speed: 'msg2_battery',
    Symbols.help: 'msg2_help',
    Symbols.contact_support: 'msg2_ask_question',
    Symbols.policy: 'msg2_policy',
    Symbols.description: 'msg2_policy',
    Symbols.block: 'msg2_block2',
    Symbols.person: 'msg_contacts',
    Symbols.person_add: 'msg_groups_create',
    Symbols.contacts: 'msg_contacts',
    Symbols.group: 'msg_groups_create',
    Symbols.call: 'msg_calls',
    Symbols.wallpaper: 'msg_background',
    Symbols.palette: 'msg_theme',
    Symbols.styler: 'msg_theme',
    Symbols.settings: 'msg_settings',
    Symbols.search: 'msg_search',
    Symbols.edit: 'msg_edit',
    Symbols.delete: 'msg_delete',
    Symbols.delete_history: 'msg2_autodelete',
    Symbols.bookmark: 'msg_saved',
    Symbols.download: 'msg_download',
    Symbols.camera_alt: 'msg_camera',
    Symbols.add: 'msg_add',
    Symbols.check: 'msg_check_s',
    Icons.check: 'msg_check_s',
    Symbols.image: 'msg_photos',
    Symbols.photo: 'msg_photos',
    Symbols.mic: 'msg_voice_unmuted',
    Symbols.attach_file: 'input_attach',
    Symbols.attachment: 'input_attach',
    Symbols.face: 'input_smile',
    Symbols.emoji_emotions: 'input_smile',
    Symbols.send: 'msg_send',
    Symbols.more_vert: 'ic_ab_other',
    Symbols.arrow_back: 'ic_ab_back',
    Icons.arrow_back: 'ic_ab_back',
    Icons.more_vert: 'ic_ab_other',
    Icons.search: 'msg_search',
    Icons.settings: 'msg_settings',
    Icons.settings_outlined: 'msg_settings',
    Icons.group_outlined: 'msg_groups_create',
    Icons.person_outline: 'msg_contacts',
    Icons.call_outlined: 'msg_calls',
    Icons.bookmark_border: 'msg_saved',
    Icons.download_outlined: 'msg_download',
    Icons.edit: 'msg_edit',
    Icons.notifications_outlined: 'msg2_notifications',
    Icons.lock_outline: 'msg2_secret',
    Icons.devices_outlined: 'msg2_devices',
  };

  static String? assetFor(IconData? icon) {
    final name = assets[icon];
    if (name == null) return null;
    return 'assets/telegram/icons/$name.${name == 'msg_delete' ? 'png' : 'webp'}';
  }

  @override
  Widget build(BuildContext context) {
    final asset = assetFor(icon);
    if (asset == null) {
      return Icon(
        icon,
        color: color,
        size: size,
        weight: weight,
        fill: fill,
        opticalSize: opticalSize,
        semanticLabel: semanticLabel,
        textDirection: textDirection,
        shadows: shadows,
      );
    }
    final theme = IconTheme.of(context);
    return Semantics(
      label: semanticLabel,
      excludeSemantics: true,
      child: Image.asset(
        asset,
        width: size ?? theme.size ?? 24,
        height: size ?? theme.size ?? 24,
        color: color ?? theme.color ?? Theme.of(context).colorScheme.onSurface,
        colorBlendMode: BlendMode.srcIn,
        fit: BoxFit.contain,
        filterQuality: FilterQuality.medium,
        matchTextDirection:
            icon == Icons.arrow_back || icon == Symbols.arrow_back,
      ),
    );
  }
}
