import 'package:flutter/material.dart';
import 'package:material_symbols_icons/symbols.dart';

import '../frontend/widgets/settings_card.dart';
import 'maxgram_theme.dart';
import 'telegram_icon.dart';

@immutable
class TelegramSettingsEntry {
  const TelegramSettingsEntry({
    required this.icon,
    required this.label,
    required this.onTap,
    this.value,
  });

  final IconData icon;
  final String label;
  final String? value;
  final VoidCallback onTap;
}

@immutable
class TelegramSettingsGroup {
  const TelegramSettingsGroup({this.title, required this.entries});

  final String? title;
  final List<TelegramSettingsEntry> entries;
}

class TelegramSettingsView extends StatelessWidget {
  const TelegramSettingsView({
    super.key,
    required this.name,
    required this.phone,
    required this.avatar,
    required this.groups,
    this.version,
    this.onVersionTap,
    required this.onEditProfile,
    required this.onLogout,
    this.standalone = true,
  });

  final String name;
  final String phone;
  final Widget avatar;
  final List<TelegramSettingsGroup> groups;
  final String? version;
  final VoidCallback? onVersionTap;
  final VoidCallback onEditProfile;
  final VoidCallback onLogout;
  final bool standalone;

  @override
  Widget build(BuildContext context) {
    final english = Localizations.localeOf(context).languageCode == 'en';
    final scheme = Theme.of(context).colorScheme;
    final colors = TelegramColors.of(context);
    final entries = groups.expand((group) => group.entries).toList();
    return Scaffold(
      backgroundColor: colors.section,
      appBar: AppBar(
        automaticallyImplyLeading: standalone,
        title: Text(english ? 'Settings' : 'Настройки'),
        actions: [
          IconButton(
            tooltip: english ? 'Search settings' : 'Поиск настроек',
            icon: const TelegramIcon(Symbols.search),
            onPressed: () => showSearch<void>(
              context: context,
              delegate: _SettingsSearch(entries, english),
            ),
          ),
          PopupMenuButton<String>(
            icon: const TelegramIcon(Symbols.more_vert),
            onSelected: (value) =>
                value == 'profile' ? onEditProfile() : onLogout(),
            itemBuilder: (_) => [
              PopupMenuItem(
                value: 'profile',
                child: Text(english ? 'Edit profile' : 'Изменить профиль'),
              ),
              PopupMenuItem(
                value: 'logout',
                child: Text(english ? 'Log out' : 'Выйти'),
              ),
            ],
          ),
        ],
      ),
      body: ListView(
        padding: EdgeInsets.only(
          bottom: MediaQuery.paddingOf(context).bottom + 24,
        ),
        children: [
          Material(
            color: scheme.surface,
            child: InkWell(
              onTap: onEditProfile,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(21, 12, 21, 20),
                child: Row(
                  children: [
                    SizedBox(width: 64, height: 64, child: avatar),
                    const SizedBox(width: 21),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            name,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              color: scheme.onSurface,
                              fontSize: 20,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            phone,
                            style: TextStyle(
                              color: colors.secondary,
                              fontSize: 14,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          for (final group in groups) ...[
            const SizedBox(height: 8),
            if (group.title != null) SettingsSectionTitle(group.title!),
            SettingsCard(
              children: [
                for (final entry in group.entries)
                  SettingsNavTile(
                    icon: entry.icon,
                    label: entry.label,
                    value: entry.value,
                    onTap: entry.onTap,
                  ),
              ],
            ),
          ],
          if (version != null)
            GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: onVersionTap,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(21, 20, 21, 12),
                child: Text(
                  version!,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: colors.secondary,
                    fontSize: 12,
                    height: 1.5,
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _SettingsSearch extends SearchDelegate<void> {
  _SettingsSearch(this.entries, this.english)
    : super(searchFieldLabel: english ? 'Search settings' : 'Поиск настроек');

  final List<TelegramSettingsEntry> entries;
  final bool english;

  @override
  List<Widget> buildActions(BuildContext context) => [
    if (query.isNotEmpty)
      IconButton(icon: const Icon(Icons.close), onPressed: () => query = ''),
  ];

  @override
  Widget buildLeading(BuildContext context) => IconButton(
    icon: const TelegramIcon(Icons.arrow_back),
    onPressed: () => close(context, null),
  );

  @override
  Widget buildResults(BuildContext context) => buildSuggestions(context);

  @override
  Widget buildSuggestions(BuildContext context) {
    final normalized = query.trim().toLowerCase();
    final matches = entries
        .where((entry) => entry.label.toLowerCase().contains(normalized))
        .toList();
    if (matches.isEmpty) {
      return Center(child: Text(english ? 'No results' : 'Ничего не найдено'));
    }
    return ListView(
      children: [
        for (final entry in matches)
          SettingsNavTile(
            icon: entry.icon,
            label: entry.label,
            onTap: () {
              close(context, null);
              entry.onTap();
            },
          ),
      ],
    );
  }
}
