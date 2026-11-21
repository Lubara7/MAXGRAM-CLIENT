import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:komet/maxgram/maxgram_navigation.dart';
import 'package:komet/maxgram/maxgram_theme.dart';

Widget host(Widget child, {double scale = 1, bool dark = false}) {
  return MaterialApp(
    locale: const Locale('ru'),
    supportedLocales: const [Locale('ru'), Locale('en')],
    localizationsDelegates: GlobalMaterialLocalizations.delegates,
    theme: MaxgramTheme.apply(
      ThemeData(brightness: dark ? Brightness.dark : Brightness.light),
    ),
    builder: (context, app) => MediaQuery(
      data: MediaQuery.of(
        context,
      ).copyWith(textScaler: TextScaler.linear(scale)),
      child: app!,
    ),
    home: Scaffold(body: child),
  );
}

void main() {
  for (final dark in [false, true]) {
    testWidgets(
      'toolbar remains usable with large text in ${dark ? "dark" : "light"} theme',
      (tester) async {
        tester.view.reset();
        tester.view.physicalSize = const Size(360, 800);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        var opened = false;
        var searched = false;
        await tester.pumpWidget(
          host(
            Align(
              alignment: Alignment.topCenter,
              child: MaxgramToolbar(
                title: 'Чаты',
                subtitle: 'Подключение',
                onMenu: () => opened = true,
                onSearch: () => searched = true,
              ),
            ),
            scale: 2,
            dark: dark,
          ),
        );
        await tester.tap(find.byTooltip('Меню'));
        await tester.tap(find.byTooltip('Поиск'));
        expect(opened, isTrue);
        expect(searched, isTrue);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'drawer exposes real destinations and supports scrolling at large text',
    (tester) async {
      tester.view.physicalSize = const Size(360, 640);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      final opened = <MaxgramDestination>[];
      await tester.pumpWidget(
        host(
          MaxgramDrawer(
            name: 'Тестовый профиль',
            phone: '',
            avatar: const CircleAvatar(child: Text('Т')),
            onSelected: opened.add,
          ),
          scale: 2,
        ),
      );
      await tester.tap(find.text('Контакты'));
      await Scrollable.ensureVisible(
        tester.element(find.text('Настройки')),
        alignment: 0.5,
      );
      await tester.pumpAndSettle();
      await tester.tap(find.text('Настройки'));
      expect(opened, [
        MaxgramDestination.contacts,
        MaxgramDestination.settings,
      ]);
      expect(tester.takeException(), isNull);
    },
  );
}
