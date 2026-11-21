import 'package:flutter/material.dart';

enum MaxgramDestination {
  newGroup,
  contacts,
  calls,
  savedMessages,
  settings,
  downloads,
}

abstract final class MaxgramLabels {
  static bool ru(BuildContext context) =>
      Localizations.localeOf(context).languageCode == 'ru';
  static String chats(BuildContext context) => ru(context) ? 'Чаты' : 'Chats';
  static String settings(BuildContext context) =>
      ru(context) ? 'Настройки' : 'Settings';
  static String contacts(BuildContext context) =>
      ru(context) ? 'Контакты' : 'Contacts';
  static String calls(BuildContext context) => ru(context) ? 'Звонки' : 'Calls';
  static String search(BuildContext context) =>
      ru(context) ? 'Поиск' : 'Search';
  static String menu(BuildContext context) => ru(context) ? 'Меню' : 'Menu';
  static String compose(BuildContext context) =>
      ru(context) ? 'Новое сообщение' : 'New message';
}

class MaxgramToolbar extends StatelessWidget {
  const MaxgramToolbar({
    super.key,
    required this.title,
    required this.onMenu,
    this.onSearch,
    this.leading = Icons.menu,
    this.subtitle,
    this.actions = const [],
  });

  final String title;
  final VoidCallback onMenu;
  final VoidCallback? onSearch;
  final IconData leading;
  final String? subtitle;
  final List<Widget> actions;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context).appBarTheme;
    return Material(
      color: theme.backgroundColor,
      child: IconTheme(
        data: IconThemeData(color: theme.foregroundColor, size: 24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(minHeight: 56),
          child: Row(
            children: [
              IconButton(
                tooltip: leading == Icons.menu
                    ? MaxgramLabels.menu(context)
                    : MaterialLocalizations.of(context).backButtonTooltip,
                onPressed: onMenu,
                icon: Icon(leading),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: theme.titleTextStyle,
                      ),
                      if (subtitle?.isNotEmpty ?? false)
                        Text(
                          subtitle!,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            color: theme.foregroundColor?.withValues(
                              alpha: 0.75,
                            ),
                            fontSize: 13,
                          ),
                        ),
                    ],
                  ),
                ),
              ),
              ...actions,
              if (onSearch != null)
                IconButton(
                  tooltip: MaxgramLabels.search(context),
                  onPressed: onSearch,
                  icon: const Icon(Icons.search),
                ),
              const SizedBox(width: 8),
            ],
          ),
        ),
      ),
    );
  }
}

class MaxgramDrawer extends StatelessWidget {
  const MaxgramDrawer({
    super.key,
    required this.name,
    required this.phone,
    required this.avatar,
    required this.onSelected,
  });

  final String name;
  final String phone;
  final Widget avatar;
  final ValueChanged<MaxgramDestination> onSelected;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final russian = MaxgramLabels.ru(context);
    return Drawer(
      width: (MediaQuery.sizeOf(context).width - 56).clamp(240, 320).toDouble(),
      child: SafeArea(
        top: false,
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            Material(
              color: theme.appBarTheme.backgroundColor,
              child: InkWell(
                onTap: () => onSelected(MaxgramDestination.settings),
                child: Padding(
                  padding: EdgeInsets.fromLTRB(
                    16,
                    MediaQuery.paddingOf(context).top + 20,
                    16,
                    18,
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      SizedBox(width: 64, height: 64, child: avatar),
                      const SizedBox(height: 18),
                      Text(
                        name.isEmpty ? 'MAXGRAM' : name,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: theme.appBarTheme.foregroundColor,
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 5),
                      Text(
                        phone,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: theme.appBarTheme.foregroundColor?.withValues(
                            alpha: 0.75,
                          ),
                          fontSize: 13,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 8),
            _entry(
              context,
              MaxgramDestination.newGroup,
              Icons.group_outlined,
              russian ? 'Новая группа' : 'New group',
            ),
            _entry(
              context,
              MaxgramDestination.contacts,
              Icons.person_outline,
              MaxgramLabels.contacts(context),
            ),
            _entry(
              context,
              MaxgramDestination.calls,
              Icons.call_outlined,
              MaxgramLabels.calls(context),
            ),
            _entry(
              context,
              MaxgramDestination.savedMessages,
              Icons.bookmark_border,
              russian ? 'Избранное' : 'Saved messages',
            ),
            _entry(
              context,
              MaxgramDestination.downloads,
              Icons.download_outlined,
              russian ? 'Загрузки' : 'Downloads',
            ),
            _entry(
              context,
              MaxgramDestination.settings,
              Icons.settings_outlined,
              MaxgramLabels.settings(context),
            ),
            const Divider(height: 24),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Text(
                'MAXGRAM CLIENT',
                style: TextStyle(
                  color: theme.colorScheme.onSurfaceVariant,
                  fontSize: 13,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _entry(
    BuildContext context,
    MaxgramDestination value,
    IconData icon,
    String text,
  ) {
    return ListTile(
      minTileHeight: 48,
      leading: Icon(icon, size: 24),
      title: Text(text, style: const TextStyle(fontSize: 16)),
      onTap: () => onSelected(value),
    );
  }
}
