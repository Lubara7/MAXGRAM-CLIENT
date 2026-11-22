import 'package:flutter/material.dart';

import '../core/config/persisted_setting.dart';

enum TelegramPalette { classic, day, night, tinted }

abstract final class TelegramThemeConfig {
  static final _setting = PersistedEnum<TelegramPalette>(
    prefKey: 'maxgram_telegram_palette',
    defaultValue: TelegramPalette.classic,
    encode: (value) => value.name,
    decode: (value) =>
        enumFromName(TelegramPalette.values, value, TelegramPalette.classic),
  );

  static ValueNotifier<TelegramPalette> get current => _setting.current;

  static Future<TelegramPalette> load() => _setting.load();

  static Future<void> save(TelegramPalette value) => _setting.save(value);

  static TelegramPalette resolve(Brightness brightness) {
    final selected = current.value;
    if (brightness == Brightness.dark) {
      return selected == TelegramPalette.night
          ? TelegramPalette.night
          : TelegramPalette.tinted;
    }
    return selected == TelegramPalette.day
        ? TelegramPalette.day
        : TelegramPalette.classic;
  }
}

abstract final class TelegramChatStyle {
  static final _textSize = PersistedSetting<double>(
    prefKey: 'maxgram_message_text_size',
    defaultValue: 16,
    read: (prefs, key) => prefs.getDouble(key),
    write: (prefs, key, value) async {
      await prefs.setDouble(key, value);
    },
  );
  static final _corners = PersistedSetting<double>(
    prefKey: 'maxgram_message_corners',
    defaultValue: 17,
    read: (prefs, key) => prefs.getDouble(key),
    write: (prefs, key, value) async {
      await prefs.setDouble(key, value);
    },
  );

  static ValueNotifier<double> get textSize => _textSize.current;
  static ValueNotifier<double> get corners => _corners.current;

  static Future<void> load() async {
    await _textSize.load();
    await _corners.load();
  }

  static Future<void> saveTextSize(double value) =>
      _textSize.save(value.clamp(12, 30).toDouble());
  static Future<void> saveCorners(double value) =>
      _corners.save(value.clamp(0, 17).toDouble());
}
