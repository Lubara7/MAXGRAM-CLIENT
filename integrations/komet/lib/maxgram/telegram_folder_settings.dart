import 'package:flutter/material.dart';
import 'package:material_symbols_icons/symbols.dart';

import '../backend/models/chat_folder.dart';
import '../backend/modules/folders.dart';
import '../core/storage/token_storage.dart';
import '../frontend/screens/chats/folder_edit_sheet.dart';
import '../frontend/widgets/settings_card.dart';
import '../frontend/widgets/small_spinner.dart';
import 'maxgram_theme.dart';

class TelegramFolderSettings extends StatefulWidget {
  const TelegramFolderSettings({super.key});

  @override
  State<TelegramFolderSettings> createState() => _TelegramFolderSettingsState();
}

class _TelegramFolderSettingsState extends State<TelegramFolderSettings> {
  List<ChatFolder> _folders = const [];
  bool _loading = true;
  bool _failed = false;

  String _text(String ru, String en) =>
      Localizations.localeOf(context).languageCode == 'en' ? en : ru;

  @override
  void initState() {
    super.initState();
    FoldersModule.revision.addListener(_reload);
    _reload();
  }

  @override
  void dispose() {
    FoldersModule.revision.removeListener(_reload);
    super.dispose();
  }

  Future<void> _reload() async {
    try {
      final account = await TokenStorage.getActiveAccountId();
      if (account == null) throw StateError('No active account');
      final folders = await FoldersModule.loadFolders(account);
      if (mounted) {
        setState(() {
          _folders = folders;
          _loading = false;
          _failed = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _loading = false;
          _failed = true;
        });
      }
    }
  }

  Future<void> _edit([ChatFolder? folder]) async {
    await showFolderEditSheet(context, folder: folder);
    if (mounted) await _reload();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: TelegramColors.of(context).section,
    appBar: AppBar(title: Text(_text('Папки с чатами', 'Chat Folders'))),
    body: _loading
        ? const Center(child: SmallSpinner(size: 28))
        : _failed
        ? Center(
            child: TextButton(
              onPressed: _reload,
              child: Text(_text('Повторить загрузку', 'Retry')),
            ),
          )
        : ListView(
            padding: const EdgeInsets.only(top: 8, bottom: 24),
            children: [
              SettingsSectionTitle(_text('Мои папки', 'My Folders')),
              SettingsCard(
                children: [
                  for (final folder in _folders)
                    SettingsNavTile(
                      icon: Symbols.folder,
                      label: folder.title,
                      onTap: () => _edit(folder),
                    ),
                  SettingsNavTile(
                    icon: Symbols.add,
                    label: _text('Создать новую папку', 'Create New Folder'),
                    tintColor: TelegramColors.of(context).sectionTitle,
                    onTap: () => _edit(),
                  ),
                ],
              ),
              SettingsDescription(
                _text(
                  'Разделяйте чаты по папкам и переключайтесь между ними на главном экране.',
                  'Organize chats into folders and switch between them on the main screen.',
                ),
              ),
            ],
          ),
  );
}
