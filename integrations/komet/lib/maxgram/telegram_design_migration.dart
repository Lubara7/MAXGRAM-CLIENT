import 'package:shared_preferences/shared_preferences.dart';

import '../core/config/app_chat_chrome.dart';
import '../core/config/app_composer_background.dart';
import '../core/config/app_composer_style.dart';
import '../core/config/app_pill_gradient.dart';
import '../core/config/app_spectrum_background.dart';
import '../core/config/app_visual_style.dart';

abstract final class TelegramDesignMigration {
  static const revisionKey = 'maxgram_telegram_design_revision';
  static const revision = 1;

  static Future<void> apply() async {
    final prefs = await SharedPreferences.getInstance();
    if ((prefs.getInt(revisionKey) ?? 0) >= revision) return;
    await prefs.setString(AppVisualStyle.prefKey, VisualStyle.materialYou.name);
    await prefs.setString(
      AppComposerStyle.prefKey,
      ComposerStyle.materialYou.name,
    );
    await prefs.setString(AppChatChrome.prefKey, ChatChromeStyle.color.name);
    await prefs.setString(
      AppComposerBackground.prefKey,
      ComposerBackground.standard.name,
    );
    await prefs.setBool(AppPillGradient.prefKey, false);
    await prefs.setBool(AppSpectrumBackground.prefKey, false);
    await prefs.setInt(revisionKey, revision);
  }
}
