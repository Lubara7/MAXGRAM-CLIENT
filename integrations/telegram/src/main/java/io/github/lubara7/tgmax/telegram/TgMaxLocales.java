package io.github.lubara7.tgmax.telegram;

import java.util.Locale;
import org.telegram.messenger.LocaleController;

public final class TgMaxLocales {
    private static final String[][] LANGUAGES = {
            {"ru", "Русский", "Russian"}, {"en", "English", "English"},
            {"es", "Español", "Spanish"}, {"pt_br", "Português (Brasil)", "Portuguese (Brazil)"},
            {"uz", "O‘zbek", "Uzbek"}, {"fr", "Français", "French"}, {"be", "Беларуская", "Belarusian"}
    };

    private TgMaxLocales() { }

    public static String code(String tag) {
        Locale locale = Locale.forLanguageTag(tag.replace('_', '-'));
        if (locale.getLanguage().equals("pt")) return "pt_br";
        for (String[] language : LANGUAGES) if (language[0].equals(locale.getLanguage())) return language[0];
        return "en";
    }

    public static String networkCode(String tag) {
        String language = code(tag);
        return language.equals("pt_br") ? "pt-BR" : language;
    }

    public static String localizedName(LocaleController.LocaleInfo language) {
        String label = Locale.forLanguageTag(language.shortName.replace('_', '-'))
                .getDisplayName(LocaleController.getInstance().getCurrentLocale());
        if (label.isEmpty()) return language.name;
        return label.substring(0, 1).toUpperCase(LocaleController.getInstance().getCurrentLocale()) + label.substring(1);
    }

    public static void install(LocaleController controller) {
        controller.languages.clear(); controller.languagesDict.clear();
        controller.remoteLanguages.clear(); controller.remoteLanguagesDict.clear(); controller.unofficialLanguages.clear();
        for (String[] language : LANGUAGES) {
            LocaleController.LocaleInfo info = new LocaleController.LocaleInfo();
            info.name = language[1]; info.nameEnglish = language[2];
            info.shortName = language[0]; info.pluralLangCode = language[0].equals("pt_br") ? "pt_BR" : language[0];
            info.builtIn = true;
            controller.languages.add(info); controller.languagesDict.put(info.shortName, info);
        }
    }
}
