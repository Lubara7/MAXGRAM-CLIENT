import 'package:flutter/foundation.dart';

class MaxgramOtpChallenge extends ChangeNotifier {
  MaxgramOtpChallenge({
    required String token,
    required this.requestNewCode,
    this.resendDelay = const Duration(seconds: 30),
    DateTime Function()? now,
  }) : _token = token,
       _now = now ?? DateTime.now {
    _retryAt = _now().add(resendDelay);
  }

  final Future<String> Function() requestNewCode;
  final Duration resendDelay;
  final DateTime Function() _now;
  late DateTime _retryAt;
  String _token;
  Future<bool>? _pending;
  int _generation = 0;
  bool _disposed = false;

  String get token => _token;
  bool get isResending => _pending != null;
  int get remainingSeconds {
    final remaining = _retryAt.difference(_now()).inMilliseconds;
    return remaining <= 0 ? 0 : (remaining / 1000).ceil();
  }

  void replaceToken(String token) {
    if (_disposed) return;
    if (token.isEmpty) throw const FormatException('Empty verification token');
    _generation++;
    _token = token;
    _retryAt = _now().add(resendDelay);
    notifyListeners();
  }

  Future<bool> resend() {
    if (_disposed) return Future.value(false);
    final pending = _pending;
    if (pending != null) return pending;
    if (remainingSeconds > 0) return Future.value(false);
    final generation = _generation;
    late final Future<bool> operation;
    operation = Future.sync(requestNewCode)
        .then((token) {
          if (_disposed || generation != _generation) return false;
          if (token.isEmpty) {
            throw const FormatException('Empty verification token');
          }
          _token = token;
          _retryAt = _now().add(resendDelay);
          return true;
        })
        .whenComplete(() {
          if (identical(_pending, operation)) _pending = null;
          if (!_disposed) notifyListeners();
        });
    _pending = operation;
    notifyListeners();
    return operation;
  }

  @override
  void dispose() {
    _disposed = true;
    _generation++;
    super.dispose();
  }
}
