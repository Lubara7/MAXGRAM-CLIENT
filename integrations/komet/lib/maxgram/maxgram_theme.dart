import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'telegram_icon.dart';
import 'telegram_palettes.dart';
import 'telegram_theme_config.dart';

@immutable
class TelegramColors extends ThemeExtension<TelegramColors> {
  const TelegramColors(this.palette);

  final Map<String, Color> palette;

  Color get icon => palette['windowBackgroundWhiteGrayIcon']!;
  Color get secondary => palette['windowBackgroundWhiteGrayText2']!;
  Color get section => palette['windowBackgroundGray']!;
  Color get sectionTitle => palette['windowBackgroundWhiteBlueHeader']!;
  Color get value => palette['windowBackgroundWhiteValueText']!;
  Color get switchTrack => palette['switchTrack']!;
  Color get switchChecked => palette['switchTrackChecked']!;
  Color get incoming => palette['chat_inBubble']!;
  Color get outgoing => palette['chat_outBubble']!;
  Color get incomingText => palette['chat_messageTextIn']!;
  Color get outgoingText => palette['chat_messageTextOut']!;
  Color get wallpaper => palette['chat_wallpaper']!;

  static TelegramColors of(BuildContext context) {
    final theme = Theme.of(context);
    return theme.extension<TelegramColors>() ??
        TelegramColors(
          telegramPalettes[TelegramThemeConfig.resolve(theme.brightness).name]!,
        );
  }

  @override
  TelegramColors copyWith({Map<String, Color>? palette}) =>
      TelegramColors(palette ?? this.palette);

  @override
  TelegramColors lerp(covariant TelegramColors? other, double t) =>
      other == null
      ? this
      : TelegramColors({
          for (final entry in palette.entries)
            entry.key: Color.lerp(entry.value, other.palette[entry.key], t)!,
        });
}

abstract final class MaxgramTheme {
  static const accent = Color(0xFF229AF0);
  static const lightBar = Color(0xFFFFFFFF);
  static const darkBar = Color(0xFF243442);

  static ThemeData apply(
    ThemeData base, {
    Color? accentColor,
    String? fontFamily,
  }) {
    final dark = base.brightness == Brightness.dark;
    final amoled = dark && base.colorScheme.surface == Colors.black;
    final selected = TelegramThemeConfig.resolve(base.brightness);
    final palette = <String, Color>{...telegramPalettes[selected.name]!};
    final primary = accentColor ?? palette['switchTrackChecked']!;
    if (accentColor != null) {
      palette['switchTrackChecked'] = accentColor;
      palette['windowBackgroundWhiteBlueHeader'] = accentColor;
      palette['windowBackgroundWhiteValueText'] = accentColor;
    }
    if (amoled) {
      palette['windowBackgroundWhite'] = Colors.black;
      palette['windowBackgroundGray'] = Colors.black;
      palette['actionBarDefault'] = Colors.black;
    }
    final surface = palette['windowBackgroundWhite']!;
    final foreground = palette['windowBackgroundWhiteBlackText']!;
    final secondary = palette['windowBackgroundWhiteGrayText2']!;
    final bar = palette['actionBarDefault']!;
    final barForeground = palette['actionBarDefaultTitle']!;
    final onPrimary =
        ThemeData.estimateBrightnessForColor(primary) == Brightness.dark
        ? Colors.white
        : const Color(0xFF1A1D21);
    final scheme = base.colorScheme.copyWith(
      primary: primary,
      onPrimary: onPrimary,
      primaryContainer: palette['chat_outBubble'],
      onPrimaryContainer: palette['chat_messageTextOut'],
      secondary: primary,
      onSecondary: onPrimary,
      surface: surface,
      onSurface: foreground,
      onSurfaceVariant: secondary,
      surfaceContainerLowest: palette['windowBackgroundGray'],
      surfaceContainerLow: palette['windowBackgroundGray'],
      surfaceContainer: surface,
      surfaceContainerHigh: surface,
      surfaceContainerHighest: palette['chat_inBubble'],
      outline: secondary,
      outlineVariant: palette['divider'],
    );
    final family = fontFamily ?? 'Roboto';
    final buttonShape = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(8),
    );
    return base.copyWith(
      useMaterial3: false,
      colorScheme: scheme,
      scaffoldBackgroundColor: surface,
      canvasColor: surface,
      dividerColor: palette['divider'],
      textTheme: base.textTheme.apply(
        fontFamily: family,
        bodyColor: foreground,
        displayColor: foreground,
      ),
      primaryTextTheme: base.primaryTextTheme.apply(fontFamily: family),
      extensions: [...base.extensions.values, TelegramColors(palette)],
      visualDensity: VisualDensity.standard,
      splashFactory: InkRipple.splashFactory,
      splashColor: foreground.withValues(alpha: 0.06),
      highlightColor: foreground.withValues(alpha: 0.04),
      appBarTheme: AppBarTheme(
        backgroundColor: bar,
        foregroundColor: barForeground,
        surfaceTintColor: Colors.transparent,
        centerTitle: false,
        toolbarHeight: 56,
        elevation: 0,
        scrolledUnderElevation: 0,
        titleSpacing: 16,
        iconTheme: IconThemeData(
          color: palette['actionBarDefaultIcon'],
          size: 24,
        ),
        actionsIconTheme: IconThemeData(
          color: palette['actionBarDefaultIcon'],
          size: 24,
        ),
        titleTextStyle: TextStyle(
          color: barForeground,
          fontFamily: family,
          fontSize: 20,
          fontWeight: FontWeight.w500,
        ),
        systemOverlayStyle:
            (dark ? SystemUiOverlayStyle.light : SystemUiOverlayStyle.dark)
                .copyWith(
                  statusBarColor: Colors.transparent,
                  systemNavigationBarColor: surface,
                ),
      ),
      actionIconTheme: ActionIconThemeData(
        backButtonIconBuilder: (_) => const TelegramIcon(Icons.arrow_back),
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: primary,
        foregroundColor: onPrimary,
        shape: const CircleBorder(),
        elevation: 2,
      ),
      drawerTheme: DrawerThemeData(
        backgroundColor: surface,
        surfaceTintColor: Colors.transparent,
        shape: const RoundedRectangleBorder(),
      ),
      listTileTheme: ListTileThemeData(
        minTileHeight: 50,
        minLeadingWidth: 24,
        horizontalTitleGap: 23,
        iconColor: palette['windowBackgroundWhiteGrayIcon'],
        textColor: foreground,
        titleTextStyle: TextStyle(
          color: foreground,
          fontFamily: family,
          fontSize: 16,
          fontWeight: FontWeight.w400,
        ),
        contentPadding: const EdgeInsets.symmetric(horizontal: 21),
      ),
      sliderTheme: SliderThemeData(
        trackHeight: 3,
        activeTrackColor: primary,
        inactiveTrackColor: primary.withValues(alpha: 0.22),
        thumbColor: primary,
        thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 6),
        overlayShape: const RoundSliderOverlayShape(overlayRadius: 16),
        overlayColor: primary.withValues(alpha: 0.08),
        showValueIndicator: ShowValueIndicator.never,
      ),
      popupMenuTheme: PopupMenuThemeData(
        color: surface,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      ),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: surface,
        surfaceTintColor: Colors.transparent,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(14)),
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: surface,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: primary,
          foregroundColor: onPrimary,
          minimumSize: const Size(48, 48),
          padding: const EdgeInsets.symmetric(horizontal: 21, vertical: 12),
          shape: buttonShape,
          textStyle: TextStyle(
            fontFamily: family,
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primary,
          foregroundColor: onPrimary,
          elevation: 0,
          minimumSize: const Size(48, 48),
          shape: buttonShape,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: palette['windowBackgroundWhiteBlueHeader'],
          minimumSize: const Size(48, 48),
          shape: buttonShape,
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: palette['windowBackgroundWhiteBlueHeader'],
          minimumSize: const Size(48, 48),
          textStyle: TextStyle(
            fontFamily: family,
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        contentPadding: const EdgeInsets.symmetric(vertical: 12),
        border: UnderlineInputBorder(
          borderSide: BorderSide(color: scheme.outlineVariant),
        ),
        enabledBorder: UnderlineInputBorder(
          borderSide: BorderSide(color: scheme.outlineVariant),
        ),
        focusedBorder: UnderlineInputBorder(
          borderSide: BorderSide(color: primary, width: 2),
        ),
      ),
      dividerTheme: DividerThemeData(
        color: palette['divider'],
        thickness: 0.5,
        space: 0,
      ),
    );
  }
}
