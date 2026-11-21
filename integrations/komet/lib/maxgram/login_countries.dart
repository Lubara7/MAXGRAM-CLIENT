import '../core/config/countries.dart';

const maxgramCountryOrder = [
  'RU',
  'BY',
  'AZ',
  'AM',
  'AF',
  'BO',
  'BR',
  'VE',
  'VN',
  'GM',
  'GD',
  'GE',
  'EG',
  'IN',
  'ID',
  'IQ',
  'KZ',
  'KH',
  'QA',
  'CN',
  'CO',
  'CG',
  'CD',
  'CU',
  'KW',
  'KG',
  'LA',
  'LB',
  'MY',
  'MD',
  'MM',
  'NI',
  'AE',
  'PK',
  'PW',
  'SA',
  'KN',
  'TJ',
  'TH',
  'TZ',
  'TM',
  'TR',
  'UZ',
  'ZA',
];

List<CountryName> maxgramLoginCountries(Iterable<CountryName> supported) {
  final available = {for (final country in supported) country.code: country};
  return [
    for (final code in maxgramCountryOrder)
      if (available[code] case final CountryName country)
        _normalizeDialCode(country),
  ];
}

CountryName _normalizeDialCode(CountryName country) {
  if (country.code != 'GD' && country.code != 'KN') return country;
  return CountryName(
    code: country.code,
    en: country.en,
    ru: country.ru,
    phoneCode: '+1',
    phoneDigits: 10,
    phoneMask: '### ### ####',
    phoneGroupSizes: const [3, 3, 4],
    phoneGroupSeparators: const ['', ' ', ' ', ''],
  );
}
