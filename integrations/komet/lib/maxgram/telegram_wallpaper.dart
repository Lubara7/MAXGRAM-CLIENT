import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import 'maxgram_theme.dart';

class TelegramWallpaper extends StatefulWidget {
  const TelegramWallpaper({super.key, this.dark, this.color});

  final bool? dark;
  final Color? color;

  static const classicColors = [
    Color(0xFFDBDDBB),
    Color(0xFF6BA587),
    Color(0xFFD5D88D),
    Color(0xFF88B884),
  ];

  static Future<void> preload() async {
    final results = await Future.wait<Object>([
      _TelegramWallpaperState._program,
      _TelegramWallpaperState._picture,
    ]);
    _TelegramWallpaperState._loadedProgram = results[0] as ui.FragmentProgram;
    _TelegramWallpaperState._loadedPicture = results[1] as PictureInfo;
  }

  @override
  State<TelegramWallpaper> createState() => _TelegramWallpaperState();
}

class _TelegramWallpaperState extends State<TelegramWallpaper> {
  static final _program = ui.FragmentProgram.fromAsset(
    'shaders/telegram_gradient.frag',
  );
  static final _picture = vg.loadPicture(
    const SvgAssetLoader('assets/telegram/default_pattern.svg'),
    null,
  );
  static ui.FragmentProgram? _loadedProgram;
  static PictureInfo? _loadedPicture;
  ui.FragmentShader? _shader;
  PictureInfo? _pattern;

  @override
  void initState() {
    super.initState();
    if (_loadedProgram != null && _loadedPicture != null) {
      _shader = _loadedProgram!.fragmentShader();
      _pattern = _loadedPicture;
    } else {
      _resolve();
    }
  }

  Future<void> _resolve() async {
    await TelegramWallpaper.preload();
    if (!mounted) return;
    setState(() {
      _shader = _loadedProgram!.fragmentShader();
      _pattern = _loadedPicture;
    });
  }

  @override
  void dispose() {
    _shader?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final dark = widget.dark ?? Theme.of(context).brightness == Brightness.dark;
    final wallpaper = widget.color ?? TelegramColors.of(context).wallpaper;
    return RepaintBoundary(
      child: CustomPaint(
        painter: _WallpaperPainter(
          shader: _shader,
          pattern: _pattern,
          dark: dark,
          color: wallpaper,
        ),
        child: const SizedBox.expand(),
      ),
    );
  }
}

class _WallpaperPainter extends CustomPainter {
  const _WallpaperPainter({
    required this.shader,
    required this.pattern,
    required this.dark,
    required this.color,
  });

  final ui.FragmentShader? shader;
  final PictureInfo? pattern;
  final bool dark;
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    if (size.isEmpty) return;
    final bounds = Offset.zero & size;
    canvas.clipRect(bounds);
    final gradient = shader;
    if (!dark && color != Colors.white && gradient != null) {
      gradient.setFloat(0, size.width);
      gradient.setFloat(1, size.height);
      for (var i = 0; i < 4; i++) {
        final value = TelegramWallpaper.classicColors[i];
        gradient.setFloat(2 + i * 4, value.r);
        gradient.setFloat(3 + i * 4, value.g);
        gradient.setFloat(4 + i * 4, value.b);
        gradient.setFloat(5 + i * 4, 1);
      }
      canvas.drawRect(bounds, Paint()..shader = gradient);
    } else {
      canvas.drawRect(
        bounds,
        Paint()
          ..color = dark || color == Colors.white
              ? color
              : TelegramWallpaper.classicColors.first,
      );
    }
    if (!dark && color == Colors.white) return;
    final drawing = pattern;
    if (drawing == null) return;
    final paint = Paint()
      ..blendMode = BlendMode.softLight
      ..colorFilter = ColorFilter.mode(
        (dark ? Colors.white : Colors.black).withValues(
          alpha: dark ? 0.15 : 0.34,
        ),
        BlendMode.srcIn,
      );
    canvas.saveLayer(bounds, paint);
    canvas.scale(size.width / drawing.size.width);
    canvas.drawPicture(drawing.picture);
    canvas.restore();
  }

  @override
  bool shouldRepaint(_WallpaperPainter oldDelegate) =>
      shader != oldDelegate.shader ||
      pattern != oldDelegate.pattern ||
      dark != oldDelegate.dark ||
      color != oldDelegate.color;
}
