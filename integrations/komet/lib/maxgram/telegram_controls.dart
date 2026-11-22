import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'maxgram_theme.dart';

class TelegramSwitch extends StatelessWidget {
  const TelegramSwitch({
    super.key,
    required this.value,
    required this.onChanged,
  });

  final bool value;
  final ValueChanged<bool>? onChanged;

  @override
  Widget build(BuildContext context) {
    final colors = TelegramColors.of(context);
    return Semantics(
      toggled: value,
      enabled: onChanged != null,
      onTap: onChanged == null ? null : () => onChanged!(!value),
      child: ExcludeSemantics(
        child: FocusableActionDetector(
          enabled: onChanged != null,
          mouseCursor: onChanged == null
              ? SystemMouseCursors.basic
              : SystemMouseCursors.click,
          actions: {
            ActivateIntent: CallbackAction<ActivateIntent>(
              onInvoke: (_) {
                onChanged?.call(!value);
                return null;
              },
            ),
          },
          child: GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: onChanged == null ? null : () => onChanged!(!value),
            onHorizontalDragEnd: onChanged == null
                ? null
                : (details) {
                    final velocity = details.primaryVelocity ?? 0;
                    if (velocity.abs() < 40) return;
                    final rtl = Directionality.of(context) == TextDirection.rtl;
                    onChanged!((velocity > 0) != rtl);
                  },
            child: SizedBox(
              width: 48,
              height: 48,
              child: Center(
                child: TweenAnimationBuilder<double>(
                  tween: Tween(begin: value ? 1 : 0, end: value ? 1 : 0),
                  duration: MediaQuery.disableAnimationsOf(context)
                      ? Duration.zero
                      : const Duration(milliseconds: 200),
                  curve: Curves.easeOut,
                  builder: (context, progress, _) => CustomPaint(
                    size: const Size(37, 20),
                    painter: _SwitchPainter(
                      progress: progress,
                      checked: colors.switchChecked,
                      unchecked: colors.switchTrack,
                      thumb: Theme.of(context).colorScheme.surface,
                      enabled: onChanged != null,
                      rtl: Directionality.of(context) == TextDirection.rtl,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _SwitchPainter extends CustomPainter {
  const _SwitchPainter({
    required this.progress,
    required this.checked,
    required this.unchecked,
    required this.thumb,
    required this.enabled,
    required this.rtl,
  });

  final double progress;
  final Color checked;
  final Color unchecked;
  final Color thumb;
  final bool enabled;
  final bool rtl;

  @override
  void paint(Canvas canvas, Size size) {
    final t = rtl ? 1 - progress : progress;
    final opacity = enabled ? 1.0 : 0.4;
    final paint = Paint()
      ..color = Color.lerp(
        unchecked,
        checked,
        progress,
      )!.withValues(alpha: opacity);
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        const Rect.fromLTWH(3, 3, 31, 14),
        const Radius.circular(7),
      ),
      paint,
    );
    final center = Offset(10 + 17 * t, 10);
    canvas.drawCircle(center, 10, paint);
    canvas.drawCircle(
      center,
      8,
      Paint()..color = thumb.withValues(alpha: opacity),
    );
  }

  @override
  bool shouldRepaint(_SwitchPainter oldDelegate) =>
      progress != oldDelegate.progress ||
      checked != oldDelegate.checked ||
      unchecked != oldDelegate.unchecked ||
      thumb != oldDelegate.thumb ||
      enabled != oldDelegate.enabled ||
      rtl != oldDelegate.rtl;
}

class TelegramRadio extends StatelessWidget {
  const TelegramRadio({super.key, required this.selected});

  final bool selected;

  @override
  Widget build(BuildContext context) => TweenAnimationBuilder<double>(
    tween: Tween(begin: selected ? 1 : 0, end: selected ? 1 : 0),
    duration: MediaQuery.disableAnimationsOf(context)
        ? Duration.zero
        : const Duration(milliseconds: 200),
    builder: (context, progress, _) => CustomPaint(
      size: const Size(22, 22),
      painter: _RadioPainter(
        progress,
        Theme.of(context).colorScheme.primary,
        TelegramColors.of(context).switchTrack,
      ),
    ),
  );
}

class _RadioPainter extends CustomPainter {
  const _RadioPainter(this.progress, this.active, this.inactive);

  final double progress;
  final Color active;
  final Color inactive;

  @override
  void paint(Canvas canvas, Size size) {
    final center = size.center(Offset.zero);
    canvas.drawCircle(
      center,
      9,
      Paint()
        ..color = Color.lerp(inactive, active, progress)!
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2,
    );
    canvas.drawCircle(center, 5 * progress, Paint()..color = active);
  }

  @override
  bool shouldRepaint(_RadioPainter oldDelegate) =>
      progress != oldDelegate.progress ||
      active != oldDelegate.active ||
      inactive != oldDelegate.inactive;
}

class TelegramSeekBar extends StatelessWidget {
  const TelegramSeekBar({
    super.key,
    required this.value,
    required this.onChanged,
    this.min = 0,
    this.max = 1,
    this.divisions,
    this.label,
    this.onChangeStart,
    this.onChangeEnd,
  }) : assert(max > min);

  final double value;
  final ValueChanged<double>? onChanged;
  final double min;
  final double max;
  final int? divisions;
  final String? label;
  final ValueChanged<double>? onChangeStart;
  final ValueChanged<double>? onChangeEnd;

  double _clamp(double next) {
    var normalized = ((next - min) / (max - min)).clamp(0.0, 1.0);
    if (divisions != null) {
      normalized = (normalized * divisions!).round() / divisions!;
    }
    return min + normalized * (max - min);
  }

  @override
  Widget build(BuildContext context) {
    final rtl = Directionality.of(context) == TextDirection.rtl;
    final step = (max - min) / (divisions ?? 20);
    void adjust(double delta) => onChanged?.call(_clamp(value + delta));
    return Semantics(
      slider: true,
      value: label ?? value.toStringAsFixed(0),
      increasedValue: _clamp(value + step).toStringAsFixed(0),
      decreasedValue: _clamp(value - step).toStringAsFixed(0),
      enabled: onChanged != null,
      onIncrease: onChanged == null ? null : () => adjust(step),
      onDecrease: onChanged == null ? null : () => adjust(-step),
      child: CallbackShortcuts(
        bindings: {
          const SingleActivator(LogicalKeyboardKey.arrowRight): () =>
              adjust(rtl ? -step : step),
          const SingleActivator(LogicalKeyboardKey.arrowLeft): () =>
              adjust(rtl ? step : -step),
        },
        child: Focus(
          child: LayoutBuilder(
            builder: (context, constraints) {
              double position(Offset point) {
                final fraction =
                    ((point.dx - 16) / math.max(1, constraints.maxWidth - 32))
                        .clamp(0.0, 1.0);
                return _clamp(
                  min + (rtl ? 1 - fraction : fraction) * (max - min),
                );
              }

              return GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTapDown: onChanged == null
                    ? null
                    : (details) {
                        onChangeStart?.call(value);
                        onChanged!(position(details.localPosition));
                      },
                onTapUp: onChanged == null
                    ? null
                    : (details) =>
                          onChangeEnd?.call(position(details.localPosition)),
                onHorizontalDragStart: onChanged == null
                    ? null
                    : (_) => onChangeStart?.call(value),
                onHorizontalDragUpdate: onChanged == null
                    ? null
                    : (details) => onChanged!(position(details.localPosition)),
                onHorizontalDragEnd: onChanged == null
                    ? null
                    : (_) => onChangeEnd?.call(value),
                child: CustomPaint(
                  size: Size(constraints.maxWidth, 38),
                  painter: _SeekBarPainter(
                    progress: ((value - min) / (max - min)).clamp(0.0, 1.0),
                    color: Theme.of(context).colorScheme.primary,
                    rtl: rtl,
                    enabled: onChanged != null,
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}

class _SeekBarPainter extends CustomPainter {
  const _SeekBarPainter({
    required this.progress,
    required this.color,
    required this.rtl,
    required this.enabled,
  });

  final double progress;
  final Color color;
  final bool rtl;
  final bool enabled;

  @override
  void paint(Canvas canvas, Size size) {
    final left = 16.0;
    final right = size.width - 16;
    final y = size.height / 2;
    final x = left + (right - left) * (rtl ? 1 - progress : progress);
    final paint = Paint()
      ..strokeWidth = 3
      ..strokeCap = StrokeCap.round;
    canvas.drawLine(
      Offset(left, y),
      Offset(right, y),
      paint..color = color.withValues(alpha: 0.22),
    );
    canvas.drawLine(
      Offset(rtl ? right : left, y),
      Offset(x, y),
      paint..color = color.withValues(alpha: enabled ? 1 : 0.4),
    );
    canvas.drawCircle(Offset(x, y), 6, paint);
  }

  @override
  bool shouldRepaint(_SeekBarPainter oldDelegate) =>
      progress != oldDelegate.progress ||
      color != oldDelegate.color ||
      rtl != oldDelegate.rtl ||
      enabled != oldDelegate.enabled;
}
