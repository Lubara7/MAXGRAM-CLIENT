import 'package:flutter_test/flutter_test.dart';
import 'package:komet/core/config/countries.dart';
import 'package:komet/maxgram/login_countries.dart';

void main() {
  test(
    'shared phone codes retain separate countries in the requested order',
    () {
      final countries = maxgramLoginCountries(allCountries.reversed);
      expect(countries.take(4).map((c) => c.code), ['RU', 'BY', 'AZ', 'AM']);
      expect(countries.where((c) => c.phoneCode == '+7').map((c) => c.code), [
        'RU',
        'KZ',
      ]);
      expect(countries.last.code, 'ZA');
      expect(countries.where((c) => c.phoneCode == '+1').map((c) => c.code), [
        'GD',
        'KN',
      ]);
      expect(
        countries
            .where((c) => c.phoneCode == '+1')
            .every((c) => c.phoneDigits == 10),
        isTrue,
      );
      expect(countries.any((c) => c.code == 'DE'), isFalse);
    },
  );

  test(
    'server restrictions are respected without fallback to an unavailable country',
    () {
      final supported = allCountries.where(
        (c) => {'KZ', 'AZ', 'DE'}.contains(c.code),
      );
      expect(maxgramLoginCountries(supported).map((c) => c.code), ['AZ', 'KZ']);
      expect(maxgramLoginCountries([]), isEmpty);
    },
  );
}
