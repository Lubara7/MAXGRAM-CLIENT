import 'dart:async';
import 'package:flutter_test/flutter_test.dart';
import 'package:komet/maxgram/otp_challenge.dart';

void main() {
  late DateTime now;

  setUp(() => now = DateTime.utc(2026, 1, 1));

  test(
    'a duplicate tap sends one request and replaces the challenge only after success',
    () async {
      var requests = 0;
      final result = Completer<String>();
      final challenge = MaxgramOtpChallenge(
        token: 'synthetic-initial',
        now: () => now,
        requestNewCode: () {
          requests++;
          return result.future;
        },
      );
      addTearDown(challenge.dispose);
      expect(await challenge.resend(), isFalse);
      expect(requests, 0);
      now = now.add(const Duration(seconds: 30));
      final first = challenge.resend();
      final duplicate = challenge.resend();
      expect(identical(first, duplicate), isTrue);
      expect(requests, 1);
      expect(challenge.token, 'synthetic-initial');
      expect(challenge.isResending, isTrue);
      result.complete('synthetic-renewed');
      expect(await first, isTrue);
      expect(challenge.token, 'synthetic-renewed');
      expect(challenge.remainingSeconds, 30);
      expect(challenge.isResending, isFalse);
    },
  );

  test('failed resend preserves the working token and allows retry', () async {
    var attempts = 0;
    final challenge = MaxgramOtpChallenge(
      token: 'synthetic-initial',
      now: () => now,
      requestNewCode: () async {
        if (attempts++ == 0) throw TimeoutException('synthetic timeout');
        return 'synthetic-retry';
      },
    );
    addTearDown(challenge.dispose);
    now = now.add(const Duration(seconds: 30));
    await expectLater(challenge.resend(), throwsA(isA<TimeoutException>()));
    expect(challenge.token, 'synthetic-initial');
    expect(challenge.remainingSeconds, 0);
    expect(await challenge.resend(), isTrue);
    expect(challenge.token, 'synthetic-retry');
  });

  test(
    'a response from an old connection cannot overwrite the current challenge',
    () async {
      final result = Completer<String>();
      final challenge = MaxgramOtpChallenge(
        token: 'synthetic-initial',
        now: () => now,
        requestNewCode: () => result.future,
      );
      addTearDown(challenge.dispose);
      now = now.add(const Duration(seconds: 30));
      final pending = challenge.resend();
      challenge.replaceToken('synthetic-new-session');
      result.complete('synthetic-old-session');
      expect(await pending, isFalse);
      expect(challenge.token, 'synthetic-new-session');
    },
  );

  test('empty response is rejected without replacing the token', () async {
    final challenge = MaxgramOtpChallenge(
      token: 'synthetic-initial',
      now: () => now,
      requestNewCode: () async => '',
    );
    addTearDown(challenge.dispose);
    now = now.add(const Duration(seconds: 30));
    await expectLater(challenge.resend(), throwsFormatException);
    expect(challenge.token, 'synthetic-initial');
    expect(challenge.isResending, isFalse);
  });

  test(
    'a late response after closing the screen produces no notifications',
    () async {
      final result = Completer<String>();
      final challenge = MaxgramOtpChallenge(
        token: 'synthetic-initial',
        now: () => now,
        requestNewCode: () => result.future,
      );
      now = now.add(const Duration(seconds: 30));
      final pending = challenge.resend();
      challenge.dispose();
      result.complete('synthetic-late');
      expect(await pending, isFalse);
      expect(challenge.token, 'synthetic-initial');
    },
  );
}
