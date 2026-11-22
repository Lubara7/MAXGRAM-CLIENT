import 'package:flutter/material.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:material_symbols_icons/symbols.dart';

import '../frontend/screens/downloads_screen.dart';
import '../frontend/screens/profile/cloud_storage_screen.dart';
import '../frontend/screens/profile/traffic_monitor_screen.dart';
import '../frontend/widgets/custom_notification.dart';
import '../frontend/widgets/settings_card.dart';
import 'maxgram_theme.dart';

class TelegramDataSettings extends StatefulWidget {
  const TelegramDataSettings({super.key});

  @override
  State<TelegramDataSettings> createState() => _TelegramDataSettingsState();
}

class _TelegramDataSettingsState extends State<TelegramDataSettings> {
  bool _clearing = false;

  String _text(String ru, String en) =>
      Localizations.localeOf(context).languageCode == 'en' ? en : ru;

  void _open(Widget screen) =>
      Navigator.push(context, MaterialPageRoute<void>(builder: (_) => screen));

  Future<void> _clearCache() async {
    if (_clearing) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(_text('Очистить кэш изображений?', 'Clear image cache?')),
        content: Text(
          _text(
            'Фотографии будут загружены повторно при просмотре. Сообщения и сохранённые файлы останутся.',
            'Photos will download again when viewed. Messages and saved files will be kept.',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: Text(_text('ОТМЕНА', 'CANCEL')),
          ),
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: Text(_text('ОЧИСТИТЬ', 'CLEAR')),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    setState(() => _clearing = true);
    try {
      await DefaultCacheManager().emptyCache();
      PaintingBinding.instance.imageCache.clear();
      PaintingBinding.instance.imageCache.clearLiveImages();
      if (mounted) {
        showCustomNotification(
          context,
          _text('Кэш изображений очищен', 'Image cache cleared'),
        );
      }
    } catch (_) {
      if (mounted) {
        showCustomNotification(
          context,
          _text('Не удалось очистить кэш', 'Could not clear cache'),
        );
      }
    } finally {
      if (mounted) setState(() => _clearing = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: TelegramColors.of(context).section,
    appBar: AppBar(title: Text(_text('Данные и память', 'Data and Storage'))),
    body: ListView(
      padding: const EdgeInsets.only(top: 8, bottom: 24),
      children: [
        SettingsSectionTitle(_text('Использование данных', 'Data Usage')),
        SettingsCard(
          children: [
            SettingsNavTile(
              icon: Symbols.data_usage,
              label: _text('Использование трафика', 'Network Usage'),
              onTap: () => _open(const TrafficMonitorScreen()),
            ),
            SettingsNavTile(
              icon: Symbols.download,
              label: _text('Загрузки', 'Downloads'),
              onTap: () => _open(const DownloadsScreen()),
            ),
          ],
        ),
        const SizedBox(height: 8),
        SettingsSectionTitle(_text('Память устройства', 'Device Storage')),
        SettingsCard(
          children: [
            SettingsNavTile(
              icon: Symbols.delete,
              label: _clearing
                  ? _text('Очистка…', 'Clearing…')
                  : _text('Очистить кэш изображений', 'Clear Image Cache'),
              onTap: _clearing ? null : _clearCache,
            ),
          ],
        ),
        SettingsDescription(
          _text(
            'Загруженные изображения хранятся локально для быстрого просмотра.',
            'Downloaded images are cached on this device for faster viewing.',
          ),
        ),
        SettingsCard(
          children: [
            SettingsNavTile(
              icon: Symbols.storage,
              label: _text('Облачное хранилище MAX', 'MAX Cloud Storage'),
              onTap: () => _open(const CloudStorageScreen()),
            ),
          ],
        ),
      ],
    ),
  );
}
