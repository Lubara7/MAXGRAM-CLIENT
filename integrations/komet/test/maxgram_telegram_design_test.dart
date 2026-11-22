import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_symbols_icons/symbols.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:komet/core/config/app_theme_mode.dart';
import 'package:komet/frontend/screens/profile/appearance_screen.dart';
import 'package:komet/frontend/widgets/settings_card.dart';
import 'package:komet/frontend/widgets/chat_wallpaper_sheet.dart';
import 'package:komet/maxgram/maxgram_theme.dart';
import 'package:komet/maxgram/telegram_controls.dart';
import 'package:komet/maxgram/telegram_design_migration.dart';
import 'package:komet/maxgram/telegram_wallpaper.dart';
import 'package:komet/maxgram/telegram_icon.dart';
import 'package:komet/maxgram/telegram_settings_view.dart';
import 'package:komet/maxgram/telegram_theme_config.dart';

Widget harness(Widget child, {Brightness brightness = Brightness.light}) =>
    MaterialApp(
      locale: const Locale('ru'),
      supportedLocales: const [Locale('ru'), Locale('en')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      theme: MaxgramTheme.apply(ThemeData(brightness: brightness)),
      home: child,
    );

Widget settings({required VoidCallback privacy}) => TelegramSettingsView(
  name: 'MaxGram Demo',
  phone: '+0 000 000 00 00',
  avatar: const CircleAvatar(
    backgroundColor: Color(0xFF5DA8DC),
    child: Text('M', style: TextStyle(color: Colors.white, fontSize: 28)),
  ),
  version: 'MAXGRAM Preview',
  onEditProfile: () {},
  onLogout: () {},
  groups: [
    TelegramSettingsGroup(
      title: 'Настройки',
      entries: [
        TelegramSettingsEntry(
          icon: Symbols.chat,
          label: 'Настройки чатов',
          onTap: () {},
        ),
        TelegramSettingsEntry(
          icon: Symbols.lock,
          label: 'Конфиденциальность',
          onTap: privacy,
        ),
        TelegramSettingsEntry(
          icon: Symbols.notifications_active,
          label: 'Уведомления и звуки',
          onTap: () {},
        ),
        TelegramSettingsEntry(
          icon: Symbols.data_usage,
          label: 'Данные и память',
          onTap: () {},
        ),
        TelegramSettingsEntry(
          icon: Symbols.folder,
          label: 'Папки с чатами',
          onTap: () {},
        ),
        TelegramSettingsEntry(
          icon: Symbols.devices,
          label: 'Устройства',
          onTap: () {},
        ),
        TelegramSettingsEntry(
          icon: Symbols.language,
          label: 'Язык',
          value: 'Русский',
          onTap: () {},
        ),
      ],
    ),
    TelegramSettingsGroup(
      title: 'Помощь',
      entries: [
        TelegramSettingsEntry(
          icon: Symbols.contact_support,
          label: 'Задать вопрос',
          onTap: () {},
        ),
        TelegramSettingsEntry(
          icon: Symbols.policy,
          label: 'Политика конфиденциальности',
          onTap: () {},
        ),
      ],
    ),
  ],
);

Future<void> portrait(WidgetTester tester) async {
  tester.view.physicalSize = const Size(411, 891);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
}

void main() {
  setUpAll(() async {
    TestWidgetsFlutterBinding.ensureInitialized();
    final fonts = FontLoader('Roboto')
      ..addFont(rootBundle.load('assets/telegram/fonts/Roboto-Regular.ttf'))
      ..addFont(rootBundle.load('assets/telegram/fonts/Roboto-Medium.ttf'))
      ..addFont(rootBundle.load('assets/telegram/fonts/Roboto-Bold.ttf'));
    await fonts.load();
    await TelegramWallpaper.preload();
  });

  setUp(() {
    SharedPreferences.setMockInitialValues({});
    TelegramThemeConfig.current.value = TelegramPalette.classic;
    AppThemeModeConfig.current.value = AppThemeMode.light;
    TelegramChatStyle.textSize.value = 16;
    TelegramChatStyle.corners.value = 17;
  });

  test(
    'design migration preserves session, wallpaper and later user choices',
    () async {
      SharedPreferences.setMockInitialValues({
        'app_composer_background': 'liquidGlass',
        'app_spectrum_background': true,
        'synthetic_session': 'demo-only',
        'synthetic_wallpaper': 'gallery-demo',
        'maxgram_message_text_size': 21.0,
      });
      await TelegramDesignMigration.apply();
      final prefs = await SharedPreferences.getInstance();
      expect(prefs.getString('app_composer_background'), 'standard');
      expect(prefs.getBool('app_spectrum_background'), isFalse);
      expect(prefs.getString('synthetic_session'), 'demo-only');
      expect(prefs.getString('synthetic_wallpaper'), 'gallery-demo');
      expect(prefs.getDouble('maxgram_message_text_size'), 21);
      await prefs.setString('app_composer_background', 'frostBlur');
      await TelegramDesignMigration.apply();
      expect(prefs.getString('app_composer_background'), 'frostBlur');
      expect(prefs.getInt(TelegramDesignMigration.revisionKey), 1);
    },
  );

  test('settings use the exact imported Telegram resource files', () {
    expect(
      TelegramIcon.assetFor(Symbols.notifications_active),
      endsWith('/msg2_notifications.webp'),
    );
    expect(TelegramIcon.assetFor(Symbols.lock), endsWith('/msg2_secret.webp'));
    expect(
      TelegramIcon.assetFor(Symbols.chat),
      endsWith('/msg2_discussion.webp'),
    );
    expect(
      TelegramIcon.assetFor(Symbols.devices),
      endsWith('/msg2_devices.webp'),
    );
    for (final icon in TelegramIcon.assets.keys) {
      expect(File(TelegramIcon.assetFor(icon)!).existsSync(), isTrue);
    }
  });

  testWidgets(
    'settings preserve the original 50 dp row and 71 dp title inset',
    (tester) async {
      await portrait(tester);
      var opened = false;
      await tester.pumpWidget(harness(settings(privacy: () => opened = true)));
      await tester.pumpAndSettle();
      final label = find.text('Конфиденциальность');
      final tile = find.ancestor(
        of: label,
        matching: find.byType(SettingsNavTile),
      );
      expect(tester.getSize(tile).height, 50);
      expect(tester.getTopLeft(label).dx, 71);
      expect(tester.getSize(find.byType(AppBar)).height, 56);
      await tester.tap(label);
      expect(opened, isTrue);
      expect(find.byIcon(Symbols.chevron_right), findsNothing);
    },
  );

  testWidgets('Telegram switch uses a 37 by 20 drawing and a 48 dp hit area', (
    tester,
  ) async {
    var value = false;
    var changes = 0;
    await tester.pumpWidget(
      harness(
        Scaffold(
          body: StatefulBuilder(
            builder: (context, setState) => Center(
              child: TelegramSwitch(
                value: value,
                onChanged: (next) => setState(() {
                  value = next;
                  changes++;
                }),
              ),
            ),
          ),
        ),
      ),
    );
    final toggle = find.byType(TelegramSwitch);
    expect(tester.getSize(toggle), const Size(48, 48));
    final paint = find.descendant(
      of: toggle,
      matching: find.byType(CustomPaint),
    );
    expect(tester.getSize(paint), const Size(37, 20));
    await tester.tap(toggle);
    await tester.pumpAndSettle();
    expect(value, isTrue);
    expect(changes, 1);
    await tester.tap(toggle);
    await tester.pumpAndSettle();
    expect(value, isFalse);
    expect(changes, 2);
  });

  testWidgets('disabled switch cannot change its setting', (tester) async {
    await tester.pumpWidget(
      harness(
        const Scaffold(
          body: Center(child: TelegramSwitch(value: false, onChanged: null)),
        ),
      ),
    );
    await tester.tap(find.byType(TelegramSwitch));
    await tester.pump();
    expect(
      tester.widget<TelegramSwitch>(find.byType(TelegramSwitch)).value,
      isFalse,
    );
  });

  testWidgets('message size is persisted separately from interface scale', (
    tester,
  ) async {
    await portrait(tester);
    await tester.pumpWidget(harness(const AppearanceScreen()));
    await tester.pumpAndSettle();
    final slider = find.byType(TelegramSeekBar).first;
    final rect = tester.getRect(slider);
    await tester.tapAt(Offset(rect.right - 8, rect.center.dy));
    await tester.pumpAndSettle();
    expect(TelegramChatStyle.textSize.value, 30);
    expect(
      (await SharedPreferences.getInstance()).getDouble(
        'maxgram_message_text_size',
      ),
      30,
    );
    expect(
      (await SharedPreferences.getInstance()).getDouble('app_font_scale'),
      isNull,
    );
  });

  testWidgets('theme selection persists its palette and night mode', (
    tester,
  ) async {
    await portrait(tester);
    await tester.pumpWidget(harness(const AppearanceScreen()));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Ночная'));
    await tester.pumpAndSettle();
    final prefs = await SharedPreferences.getInstance();
    expect(TelegramThemeConfig.current.value, TelegramPalette.night);
    expect(AppThemeModeConfig.current.value, AppThemeMode.dark);
    expect(prefs.getString('maxgram_telegram_palette'), 'night');
  });

  testWidgets('large messages and accents fit a narrow Android screen', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 740);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    TelegramChatStyle.textSize.value = 30;
    await tester.pumpWidget(harness(const AppearanceScreen()));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    await tester.drag(find.byType(ListView).first, const Offset(0, -250));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });

  testWidgets('wallpaper selection returns the actual Telegram wallpaper', (
    tester,
  ) async {
    await portrait(tester);
    WallpaperPick? chosen;
    await tester.pumpWidget(
      harness(
        Builder(
          builder: (context) => Scaffold(
            body: TextButton(
              onPressed: () async {
                chosen = await showChatWallpaperSheet(context, current: null);
              },
              child: const Text('Choose'),
            ),
          ),
        ),
      ),
    );
    await tester.tap(find.text('Choose'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Классические'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Применить'));
    await tester.pumpAndSettle();
    expect(chosen?.type, WallpaperPickType.theme);
    expect(chosen?.theme?.id, 'telegram_classic');
  });

  for (final brightness in Brightness.values) {
    testWidgets('Telegram settings ${brightness.name} screenshot', (
      tester,
    ) async {
      await portrait(tester);
      await tester.pumpWidget(
        harness(
          RepaintBoundary(
            key: const ValueKey('capture'),
            child: settings(privacy: () {}),
          ),
          brightness: brightness,
        ),
      );
      await tester.pumpAndSettle();
      await expectLater(
        find.byKey(const ValueKey('capture')),
        matchesGoldenFile('goldens/telegram-settings-${brightness.name}.png'),
      );
    });
    testWidgets('Telegram chat settings ${brightness.name} screenshot', (
      tester,
    ) async {
      await portrait(tester);
      await tester.pumpWidget(
        harness(
          const RepaintBoundary(
            key: ValueKey('capture'),
            child: AppearanceScreen(),
          ),
          brightness: brightness,
        ),
      );
      await tester.pumpAndSettle();
      await expectLater(
        find.byKey(const ValueKey('capture')),
        matchesGoldenFile(
          'goldens/telegram-chat-settings-${brightness.name}.png',
        ),
      );
    });
    testWidgets('Telegram controls ${brightness.name} screenshot', (
      tester,
    ) async {
      await portrait(tester);
      await tester.pumpWidget(
        harness(
          RepaintBoundary(
            key: const ValueKey('controls'),
            child: Scaffold(
              appBar: AppBar(title: const Text('Настройки уведомлений')),
              body: Column(
                children: [
                  SettingsToggleTile(
                    icon: Symbols.notifications,
                    label: 'Уведомления',
                    value: true,
                    onChanged: (_) {},
                  ),
                  SettingsToggleTile(
                    icon: Symbols.chat,
                    label: 'Предпросмотр сообщений',
                    value: false,
                    onChanged: (_) {},
                  ),
                  SettingsToggleTile(
                    icon: Symbols.notifications,
                    label: 'Тихий режим',
                    subtitle: 'Отключены системными настройками',
                    value: false,
                    enabled: false,
                    onChanged: (_) {},
                  ),
                  const SettingsSectionTitle('Размер текста'),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 13),
                    child: TelegramSeekBar(value: 0.35, onChanged: (_) {}),
                  ),
                ],
              ),
            ),
          ),
          brightness: brightness,
        ),
      );
      await tester.pumpAndSettle();
      await expectLater(
        find.byKey(const ValueKey('controls')),
        matchesGoldenFile('goldens/telegram-controls-${brightness.name}.png'),
      );
    });
  }
}
