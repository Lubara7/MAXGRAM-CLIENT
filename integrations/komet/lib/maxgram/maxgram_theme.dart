import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

abstract final class MaxgramTheme {
  static const accent = Color(0xFF3390EC);
  static const lightBar = Color(0xFF527DA3);
  static const darkBar = Color(0xFF212D3B);

  static ThemeData apply(
    ThemeData base, {
    Color? accentColor,
    String? fontFamily,
  }) {
    final dark = base.brightness == Brightness.dark;
    final amoled = dark && base.colorScheme.surface == Colors.black;
    final primary = accentColor ?? (dark ? const Color(0xFF6AB2F2) : accent);
    final onPrimary =
        ThemeData.estimateBrightnessForColor(primary) == Brightness.dark
        ? Colors.white
        : const Color(0xFF222222);
    final foreground = dark ? const Color(0xFFF5F5F5) : const Color(0xFF222222);
    final secondary = dark ? const Color(0xFF8D9AA8) : const Color(0xFF8A8A8A);
    final surface = amoled
        ? Colors.black
        : (dark ? const Color(0xFF17212B) : Colors.white);
    final section = amoled
        ? Colors.black
        : (dark ? const Color(0xFF0E1621) : const Color(0xFFF0F0F0));
    final incoming = dark ? const Color(0xFF182533) : Colors.white;
    final outgoing = dark ? const Color(0xFF2B5278) : const Color(0xFFEFFFD9);
    final bar = dark ? darkBar : (accentColor ?? lightBar);
    final barForeground =
        ThemeData.estimateBrightnessForColor(bar) == Brightness.dark
        ? Colors.white
        : const Color(0xFF222222);
    final scheme = base.colorScheme.copyWith(
      primary: primary,
      onPrimary: onPrimary,
      primaryContainer: outgoing,
      onPrimaryContainer: foreground,
      secondary: primary,
      onSecondary: onPrimary,
      surface: surface,
      onSurface: foreground,
      onSurfaceVariant: secondary,
      surfaceContainerLowest: section,
      surfaceContainerLow: section,
      surfaceContainer: surface,
      surfaceContainerHigh: incoming,
      surfaceContainerHighest: incoming,
      outline: secondary,
      outlineVariant: dark ? const Color(0xFF263440) : const Color(0xFFE6E6E6),
    );
    return base.copyWith(
      colorScheme: scheme,
      scaffoldBackgroundColor: surface,
      canvasColor: surface,
      dividerColor: scheme.outlineVariant,
      textTheme: base.textTheme.apply(fontFamily: fontFamily ?? 'Roboto'),
      primaryTextTheme: base.primaryTextTheme.apply(
        fontFamily: fontFamily ?? 'Roboto',
      ),
      splashFactory: InkRipple.splashFactory,
      appBarTheme: AppBarTheme(
        backgroundColor: bar,
        foregroundColor: barForeground,
        surfaceTintColor: Colors.transparent,
        centerTitle: false,
        toolbarHeight: 56,
        elevation: 0,
        scrolledUnderElevation: 1,
        titleSpacing: 0,
        iconTheme: IconThemeData(color: barForeground, size: 24),
        titleTextStyle: TextStyle(
          color: barForeground,
          fontFamily: fontFamily ?? 'Roboto',
          fontSize: 20,
          fontWeight: FontWeight.w500,
        ),
        systemOverlayStyle: dark || barForeground == Colors.white
            ? SystemUiOverlayStyle.light
            : SystemUiOverlayStyle.dark,
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: primary,
        foregroundColor: onPrimary,
        shape: const CircleBorder(),
        elevation: 3,
      ),
      drawerTheme: DrawerThemeData(
        backgroundColor: surface,
        surfaceTintColor: Colors.transparent,
        shape: const RoundedRectangleBorder(),
      ),
      listTileTheme: ListTileThemeData(
        minLeadingWidth: 24,
        iconColor: secondary,
        textColor: foreground,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16),
      ),
      popupMenuTheme: PopupMenuThemeData(
        color: surface,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(6)),
      ),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: surface,
        surfaceTintColor: Colors.transparent,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: surface,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      inputDecorationTheme: InputDecorationTheme(
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16,
          vertical: 16,
        ),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
      ),
      dividerTheme: DividerThemeData(
        color: scheme.outlineVariant,
        thickness: 0.5,
      ),
    );
  }
}
