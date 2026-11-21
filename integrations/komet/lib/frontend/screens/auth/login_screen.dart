import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../backend/api.dart';
import '../../../core/config/countries.dart';
import '../../../core/protocol/packet.dart';
import '../../../main.dart';
import '../../../maxgram/login_countries.dart';
import '../../../maxgram/maxgram_navigation.dart';
import '../../widgets/adaptive_shell.dart';
import '../../widgets/custom_notification.dart';
import 'code_confirmation_screen.dart';
import 'phone_input_formatter.dart';
import 'select_country_screen.dart';
import 'token_login_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key, this.returnToAccountId});

  final int? returnToAccountId;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _phone = TextEditingController();
  StreamSubscription<SessionState>? _connection;
  CountryName? _country;
  String? _error;
  bool _submitting = false;
  bool _accepted = false;
  Future<bool>? _disclaimer;

  bool get _ru => MaxgramLabels.ru(context);
  String _text(String ru, String en) => _ru ? ru : en;
  List<CountryName> get _countries =>
      maxgramLoginCountries(api.registrationCountries);
  bool get _valid =>
      _country != null &&
      _phone.text.replaceAll(RegExp(r'\D'), '').length == _country!.phoneDigits;

  @override
  void initState() {
    super.initState();
    final countries = _countries;
    _country = countries.isEmpty ? null : countries.first;
    _connection = api.stateStream.listen((_) {
      if (!mounted) return;
      setState(() {
        final countries = _countries;
        if (!countries.any((c) => c.code == _country?.code)) {
          _country = countries.isEmpty ? null : countries.first;
          _phone.clear();
        }
      });
    });
    if (api.state == SessionState.disconnected) {
      unawaited(
        api.connect().catchError((Object error) {
          if (mounted)
            setState(
              () => _error = _text(
                'Не удалось подключиться. Попробуйте ещё раз.',
                'Could not connect. Please try again.',
              ),
            );
        }),
      );
    }
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) unawaited(_ensureDisclaimer());
    });
  }

  @override
  void dispose() {
    _connection?.cancel();
    _phone.dispose();
    super.dispose();
  }

  Future<bool> _ensureDisclaimer() {
    if (_accepted) return Future.value(true);
    return _disclaimer ??= _readDisclaimer().whenComplete(
      () => _disclaimer = null,
    );
  }

  Future<bool> _readDisclaimer() async {
    final preferences = await SharedPreferences.getInstance();
    if (!mounted) return false;
    if (preferences.getBool('maxgram_disclaimer_accepted_v1') ?? false) {
      _accepted = true;
      return true;
    }
    final accepted =
        await showDialog<bool>(
          context: context,
          barrierDismissible: false,
          builder: (dialog) => AlertDialog(
            title: const Text('MAXGRAM CLIENT'),
            content: Text(
              _text(
                'MaxGram — неофициальный клиент. MAX может блокировать аккаунты за сторонние клиенты. Используйте на свой риск. Разработчик не несёт ответственности за блокировку.',
                'MaxGram is an unofficial client. MAX may block accounts using third-party clients. Use at your own risk. The developer is not responsible for account restrictions.',
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(dialog, false),
                child: Text(_text('Отмена', 'Cancel')),
              ),
              TextButton(
                onPressed: () => Navigator.pop(dialog, true),
                child: Text(_text('Продолжить', 'Continue')),
              ),
            ],
          ),
        ) ??
        false;
    if (accepted) {
      await preferences.setBool('maxgram_disclaimer_accepted_v1', true);
      _accepted = true;
    }
    return accepted;
  }

  Future<void> _openPage(String address) async {
    try {
      final opened = await launchUrl(
        Uri.parse(address),
        mode: LaunchMode.inAppBrowserView,
      );
      if (!opened && mounted)
        showCustomNotification(
          context,
          _text('Не удалось открыть страницу', 'Could not open the page'),
        );
    } catch (_) {
      if (mounted)
        showCustomNotification(
          context,
          _text('Не удалось открыть страницу', 'Could not open the page'),
        );
    }
  }

  void _chooseLanguage() {
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (sheet) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(
                _text('На каком языке продолжить?', 'Choose your language'),
                style: Theme.of(context).textTheme.titleLarge,
              ),
            ),
            for (final (code, name, translation) in [
              ('ru', 'Русский', 'Russian'),
              ('en', 'English', 'Английский'),
            ])
              ListTile(
                title: Text(name),
                subtitle: Text(code == 'ru' && _ru ? 'Русский' : translation),
                selected: Localizations.localeOf(context).languageCode == code,
                trailing: Icon(
                  Localizations.localeOf(context).languageCode == code
                      ? Icons.radio_button_checked
                      : Icons.radio_button_unchecked,
                ),
                onTap: () {
                  Navigator.pop(sheet);
                  KometApp.stateOf(context)?.applyLocale(Locale(code));
                },
              ),
          ],
        ),
      ),
    );
  }

  Future<void> _chooseCountry() async {
    final current = _country;
    if (current == null) return;
    final selected = await Navigator.push<CountryName>(
      context,
      MaterialPageRoute(
        builder: (_) => SelectCountryScreen(
          selectedCountry: current,
          countries: _countries,
          preserveOrder: true,
        ),
      ),
    );
    if (!mounted || selected == null) return;
    setState(() {
      _country = selected;
      _phone.clear();
      _error = null;
    });
  }

  Future<void> _back() async {
    final previous = widget.returnToAccountId;
    if (previous == null) {
      Navigator.of(context).maybePop();
      return;
    }
    try {
      await resetDigitalIdSession();
      await accountModule.switchAccount(previous);
      if (!mounted) return;
      await Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => const AdaptiveShell()),
        (_) => false,
      );
    } catch (_) {
      if (mounted)
        showCustomNotification(
          context,
          _text('Не удалось переключить аккаунт', 'Could not switch accounts'),
        );
    }
  }

  Future<void> _submit() async {
    if (_submitting || !_valid) return;
    final country = _country!;
    final digits = _phone.text.replaceAll(RegExp(r'\D'), '');
    final phone = '${country.phoneCode}$digits';
    final display = '${country.phoneCode} ${_phone.text}';
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      if (!await _ensureDisclaimer() || !mounted) return;
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (dialog) => AlertDialog(
          title: Text(
            _text('Проверьте номер телефона', 'Check your phone number'),
          ),
          content: Text(display),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialog, false),
              child: Text(_text('Изменить', 'Edit')),
            ),
            TextButton(
              onPressed: () => Navigator.pop(dialog, true),
              child: Text(_text('Продолжить', 'Continue')),
            ),
          ],
        ),
      );
      if (!mounted || confirmed != true) return;
      if (api.state != SessionState.online) {
        if (api.state == SessionState.disconnected) await api.connect();
        if (!mounted) return;
        if (api.state != SessionState.online) {
          await api.stateStream
              .firstWhere((state) => state == SessionState.online)
              .timeout(const Duration(seconds: 15));
        }
      }
      if (!mounted) return;
      final result = await accountModule.requestCode(
        phone,
        language: Localizations.localeOf(context).languageCode,
      );
      if (!mounted) return;
      await Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => CodeConfirmationScreen(
            phoneNumber: display,
            rawPhone: phone,
            token: result.token,
          ),
        ),
      );
    } catch (error) {
      if (!mounted) return;
      setState(
        () => _error = error is PacketError
            ? error.message
            : _text(
                'Не удалось запросить код. Проверьте соединение и попробуйте ещё раз.',
                'Could not request a code. Check your connection and try again.',
              ),
      );
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cs = theme.colorScheme;
    final country = _country;
    return Scaffold(
      appBar: AppBar(
        backgroundColor: cs.surface,
        foregroundColor: cs.onSurfaceVariant,
        automaticallyImplyLeading: false,
        leading: Navigator.canPop(context) || widget.returnToAccountId != null
            ? IconButton(
                onPressed: _submitting ? null : _back,
                icon: const Icon(Icons.arrow_back),
                tooltip: MaterialLocalizations.of(context).backButtonTooltip,
              )
            : null,
        actions: [
          IconButton(
            onPressed: _chooseLanguage,
            icon: const Icon(Icons.language, size: 24),
            tooltip: _text('Язык', 'Language'),
          ),
          IconButton(
            onPressed: () => _openPage('https://max.ru/support'),
            icon: const Icon(Icons.help_outline, size: 24),
            tooltip: _text('Помощь', 'Help'),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(24, 36, 24, 24),
        children: [
          Center(
            child: Text(
              _text('Ваш номер телефона', 'Your phone number'),
              textAlign: TextAlign.center,
              style: theme.textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          const SizedBox(height: 16),
          Text(
            _text(
              'Проверьте код страны и введите свой номер телефона из аккаунта MAX.',
              'Check the country code and enter the phone number of your MAX account.',
            ),
            textAlign: TextAlign.center,
            style: TextStyle(
              color: cs.onSurfaceVariant,
              fontSize: 16,
              height: 1.4,
            ),
          ),
          const SizedBox(height: 40),
          OutlinedButton(
            style: OutlinedButton.styleFrom(
              alignment: Alignment.centerLeft,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
              foregroundColor: cs.onSurface,
            ),
            onPressed: _submitting || country == null ? null : _chooseCountry,
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    country?.displayName(
                          Localizations.localeOf(context).languageCode,
                        ) ??
                        _text('Нет доступных стран', 'No countries available'),
                    style: const TextStyle(fontSize: 16),
                  ),
                ),
                const Icon(Icons.chevron_right, size: 24),
              ],
            ),
          ),
          const SizedBox(height: 24),
          TextField(
            controller: _phone,
            enabled: !_submitting && country != null,
            keyboardType: TextInputType.phone,
            autofillHints: const [AutofillHints.telephoneNumberNational],
            textInputAction: TextInputAction.done,
            inputFormatters: [
              FilteringTextInputFormatter.digitsOnly,
              if (country != null) PhoneInputFormatter(country),
            ],
            decoration: InputDecoration(
              labelText: _text('Номер телефона', 'Phone number'),
              prefixText: '${country?.phoneCode ?? ''}  ',
              hintText: country?.phoneMask.replaceAll('#', '0'),
            ),
            onChanged: (_) => setState(() => _error = null),
            onSubmitted: (_) => _submit(),
          ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Text(_error!, style: TextStyle(color: cs.error)),
            ),
          const SizedBox(height: 16),
          TextButton(
            onPressed: _submitting
                ? null
                : () async {
                    if (!await _ensureDisclaimer() || !mounted) return;
                    await Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => TokenLoginScreen(
                          returnToAccountId: widget.returnToAccountId,
                        ),
                      ),
                    );
                  },
            child: Text(
              _text(
                'Войти по своему токену MAX',
                'Sign in with your MAX token',
              ),
            ),
          ),
        ],
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Align(
                alignment: Alignment.centerRight,
                child: FloatingActionButton(
                  onPressed: !_valid || _submitting ? null : _submit,
                  tooltip: _text('Продолжить', 'Continue'),
                  child: _submitting
                      ? SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color:
                                theme.floatingActionButtonTheme.foregroundColor,
                          ),
                        )
                      : const Icon(Icons.arrow_forward),
                ),
              ),
              const SizedBox(height: 16),
              Text(
                _text(
                  'Продолжая, вы принимаете условия MAX:',
                  'By continuing, you accept the MAX terms:',
                ),
                textAlign: TextAlign.center,
                style: TextStyle(color: cs.onSurfaceVariant, fontSize: 12),
              ),
              Wrap(
                alignment: WrapAlignment.center,
                children: [
                  for (final (label, url) in [
                    (
                      _text('Конфиденциальность', 'Privacy'),
                      'https://legal.max.ru/pp',
                    ),
                    (_text('Соглашение', 'Terms'), 'https://legal.max.ru/ps'),
                    (
                      _text('Рекомендации', 'Recommendations'),
                      'https://legal.max.ru/recsysrules',
                    ),
                  ])
                    TextButton(
                      onPressed: () => _openPage(url),
                      child: Text(label, style: const TextStyle(fontSize: 12)),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
