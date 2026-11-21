import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:komet/backend/api.dart';
import 'package:komet/backend/modules/account.dart';
import 'package:komet/core/config/countries.dart';
import 'package:komet/frontend/screens/auth/login_screen.dart';
import 'package:komet/l10n/app_localizations.dart';
import 'package:komet/maxgram/maxgram_theme.dart';
import 'package:shared_preferences/shared_preferences.dart';

class LoginApi extends Api {
  @override
  SessionState get state => SessionState.online;

  @override
  List<CountryName> get registrationCountries => [countriesByCode['AZ']!];
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
      await Scrollable.ensureVisible(
        tester.element(find.byType(TextField)),
        alignment: 0.5,
      );
      await tester.pumpAndSettle();
      await tester.tap(find.byType(TextField));
      await tester.pump();
      expect(tester.testTextInput.hasAnyClients, isTrue);
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
    'registration hints cannot replace the existing-account country list',
    (tester) async {
      await showLogin(tester);
      expect(find.text('🇷🇺 Россия'), findsOneWidget);
      final prefix = tester
          .widget<TextField>(find.byType(TextField))
          .decoration
          ?.prefixText;
      expect(prefix?.trim(), '+7');
    },
  );

  testWidgets(
    'help and language icons retain visible contrast on the white app bar',
    (tester) async {
      await showLogin(tester);
      for (final glyph in [Icons.help_outline, Icons.language]) {
        final finder = find.byIcon(glyph);
        final icon = tester.widget<Icon>(finder);
        final color = icon.color ?? IconTheme.of(tester.element(finder)).color!;
        final background = tester
            .widget<AppBar>(find.byType(AppBar))
            .backgroundColor!;
        final light = background.computeLuminance();
        final dark = color.computeLuminance();
        final contrast = (light > dark
            ? (light + 0.05) / (dark + 0.05)
            : (dark + 0.05) / (light + 0.05));
        expect(contrast, greaterThanOrEqualTo(3));
      }
    },
  );

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

  testWidgets('the enabled continue button stays above the software keyboard', (
    tester,
  ) async {
    await showLogin(tester);
    await tester.enterText(find.byType(TextField), '0000000000');
    tester.view.viewInsets = const FakeViewPadding(bottom: 300);
    await tester.pumpAndSettle();
    final button = find.byType(FloatingActionButton);
    expect(tester.widget<FloatingActionButton>(button).onPressed, isNotNull);
    expect(tester.getBottomRight(button).dy, lessThanOrEqualTo(340));
    expect(tester.takeException(), isNull);
  });
}
