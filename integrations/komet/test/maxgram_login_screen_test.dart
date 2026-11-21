import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:komet/backend/api.dart';
import 'package:komet/backend/modules/account.dart';
import 'package:komet/frontend/screens/auth/login_screen.dart';
import 'package:komet/l10n/app_localizations.dart';
import 'package:komet/maxgram/maxgram_theme.dart';
import 'package:shared_preferences/shared_preferences.dart';

class LoginApi extends Api {
  @override
  SessionState get state => SessionState.online;
}

void main() {
  late LoginApi api;

  setUp(() {
    SharedPreferences.setMockInitialValues({
      'maxgram_disclaimer_accepted_v1': true,
    });
    api = LoginApi();
  });

  tearDown(() => api.dispose());

  Future<void> showLogin(WidgetTester tester, {double scale = 1}) async {
    tester.view.physicalSize = const Size(360, 640);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      MaterialApp(
        locale: const Locale('ru'),
        supportedLocales: const [Locale('ru'), Locale('en')],
        localizationsDelegates: const [
          AppLocalizations.delegate,
          ...GlobalMaterialLocalizations.delegates,
        ],
        theme: MaxgramTheme.apply(ThemeData(useMaterial3: false)),
        builder: (context, app) => MediaQuery(
          data: MediaQuery.of(
            context,
          ).copyWith(textScaler: TextScaler.linear(scale)),
          child: app!,
        ),
        home: LoginScreen(connection: api, account: AccountModule(api)),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets(
    'phone login remains scrollable at large text and contains no dead login options',
    (tester) async {
      await showLogin(tester, scale: 2);
      expect(find.byTooltip('Помощь'), findsOneWidget);
      expect(find.byTooltip('Язык'), findsOneWidget);
      expect(find.byIcon(Icons.qr_code_2), findsNothing);
      expect(find.byIcon(Icons.vpn_lock), findsNothing);
      await tester.scrollUntilVisible(
        find.byType(TextField),
        120,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.ensureVisible(find.byType(TextField));
      await tester.tap(find.byType(TextField));
      await tester.enterText(find.byType(TextField), '0000000000');
      await tester.pump();
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('two taps on continue create one confirmation dialog', (
    tester,
  ) async {
    await showLogin(tester);
    await tester.enterText(find.byType(TextField), '0000000000');
    await tester.pump();
    final submit = tester
        .widget<FloatingActionButton>(find.byType(FloatingActionButton))
        .onPressed!;
    submit();
    submit();
    await tester.pump(const Duration(milliseconds: 400));
    expect(find.byType(AlertDialog), findsOneWidget);
    await tester.tap(find.text('Изменить'));
    await tester.pumpAndSettle();
    expect(find.byType(AlertDialog), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'first use displays the disclaimer before a code can be requested',
    (tester) async {
      SharedPreferences.setMockInitialValues({});
      await showLogin(tester);
      expect(find.textContaining('неофициальный клиент'), findsOneWidget);
      await tester.tap(find.text('Продолжить').last);
      await tester.pumpAndSettle();
      final preferences = await SharedPreferences.getInstance();
      expect(preferences.getBool('maxgram_disclaimer_accepted_v1'), isTrue);
      expect(find.byType(AlertDialog), findsNothing);
    },
  );
}
