package io.github.lubara7.tgmax.nagram;

import java.util.Locale;
import org.telegram.messenger.LocaleController;

public final class TgMaxLocales {
    private static final String[][] LANGUAGES = {
            {"ru", "Русский", "Russian"}, {"en", "English", "English"},
            {"es", "Español", "Spanish"}, {"uz", "O‘zbekcha", "Uzbek"},
            {"pt", "Português (Portugal)", "Portuguese"}, {"pt_br", "Português (Brasil)", "Portuguese (Brazil)"},
            {"fr", "Français", "French"}, {"be", "Беларуская", "Belarusian"}
    };

    private TgMaxLocales() { }

    public static String code(String tag) {
        Locale locale = Locale.forLanguageTag(tag.replace('_', '-'));
        if (locale.getLanguage().equals("pt") && locale.getCountry().equals("BR")) return "pt-BR";
        for (String[] language : LANGUAGES) if (language[0].equals(locale.getLanguage())) return language[0];
        return "en";
    }

    public static void install(LocaleController controller) {
        controller.languages.clear(); controller.languagesDict.clear();
        controller.remoteLanguages.clear(); controller.remoteLanguagesDict.clear(); controller.unofficialLanguages.clear();
        for (String[] language : LANGUAGES) {
            LocaleController.LocaleInfo info = new LocaleController.LocaleInfo();
            info.name = language[1]; info.nameEnglish = language[2];
            info.shortName = language[0]; info.pluralLangCode = language[0].startsWith("pt") ? "pt" : language[0];
            info.builtIn = true;
            controller.languages.add(info); controller.languagesDict.put(info.shortName, info);
        }
    }
}
