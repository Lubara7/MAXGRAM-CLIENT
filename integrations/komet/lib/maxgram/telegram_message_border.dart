import 'dart:math' as math;

import 'package:flutter/material.dart';

class TelegramMessageBorder extends ShapeBorder {
  const TelegramMessageBorder({
    required this.radius,
    required this.outgoing,
    this.tail = true,
  });

  final BorderRadius radius;
  final bool outgoing;
  final bool tail;

  @override
  EdgeInsetsGeometry get dimensions => tail
      ? EdgeInsets.only(left: outgoing ? 0 : 6, right: outgoing ? 6 : 0)
      : EdgeInsets.zero;

  @override
  ShapeBorder scale(double t) =>
      TelegramMessageBorder(radius: radius * t, outgoing: outgoing, tail: tail);

  @override
  Path getInnerPath(Rect rect, {TextDirection? textDirection}) =>
      getOuterPath(rect, textDirection: textDirection);

  @override
  Path getOuterPath(Rect rect, {TextDirection? textDirection}) {
    if (!tail || rect.width < 24 || rect.height < 24) {
      return Path()..addRRect(radius.toRRect(rect));
    }
    final left = rect.left;
    final right = rect.right;
    final top = rect.top;
    final bottom = rect.bottom;
    const padding = 2.0;
    final r = math.min(
      math.max(radius.topLeft.x, radius.bottomLeft.x),
      (rect.height - padding) / 2,
    );
    final near = math.min(outgoing ? radius.topRight.x : radius.topLeft.x, r);
    const small = 6.0;
    final path = Path()
      ..moveTo(right - 2.6, bottom - padding)
      ..lineTo(left + padding + r, bottom - padding)
      ..arcTo(
        Rect.fromLTRB(
          left + padding,
          bottom - padding - r * 2,
          left + padding + r * 2,
          bottom - padding,
        ),
        math.pi / 2,
        math.pi / 2,
        false,
      )
      ..lineTo(left + padding, top + padding + r)
      ..arcTo(
        Rect.fromLTRB(
          left + padding,
          top + padding,
          left + padding + r * 2,
          top + padding + r * 2,
        ),
        math.pi,
        math.pi / 2,
        false,
      )
      ..lineTo(right - 8 - near, top + padding)
      ..arcTo(
        Rect.fromLTRB(
          right - 8 - near * 2,
          top + padding,
          right - 8,
          top + padding + near * 2,
        ),
        math.pi * 1.5,
        math.pi / 2,
        false,
      )
      ..lineTo(right - 8, bottom - padding - small - 3)
      ..arcTo(
        Rect.fromLTRB(
          right - 8,
          bottom - padding - small * 2 - 9,
          right - 7 + small * 2,
          bottom - padding - 1,
        ),
        math.pi,
        -83 * math.pi / 180,
        false,
      )
      ..close();
    if (outgoing) return path;
    final mirror = Matrix4.identity()
      ..translateByDouble(left + right, 0, 0, 1)
      ..scaleByDouble(-1, 1, 1, 1);
    return path.transform(mirror.storage);
  }

  @override
  void paint(Canvas canvas, Rect rect, {TextDirection? textDirection}) {}
}
