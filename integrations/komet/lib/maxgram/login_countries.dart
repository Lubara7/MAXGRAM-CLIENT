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

List<CountryName> maxgramLoginCountries(Iterable<CountryName> catalog) {
  final available = {for (final country in catalog) country.code: country};
  return [
    for (final code in maxgramCountryOrder)
      if (available[code] case final CountryName country)
        _normalizeCountry(country),
  ];
}

CountryName _normalizeCountry(CountryName country) {
  const names = {
    'CN': 'Китай',
    'CG': 'Конго - Браззавиль',
    'CD': 'Конго - Киншаса',
    'MM': 'Мьянма (Бирма)',
    'ZA': 'Южно-Африканская Республика',
  };
  final sharedCode = country.code == 'GD' || country.code == 'KN';
  final name = names[country.code];
  if (!sharedCode && name == null) return country;
  return CountryName(
    code: country.code,
    en: country.en,
    ru: name ?? country.ru,
    phoneCode: sharedCode ? '+1' : country.phoneCode,
    phoneDigits: sharedCode ? 10 : country.phoneDigits,
    phoneMask: sharedCode ? '### ### ####' : country.phoneMask,
    phoneGroupSizes: sharedCode ? const [3, 3, 4] : country.phoneGroupSizes,
    phoneGroupSeparators: sharedCode
        ? const ['', ' ', ' ', '']
        : country.phoneGroupSeparators,
  );
}
