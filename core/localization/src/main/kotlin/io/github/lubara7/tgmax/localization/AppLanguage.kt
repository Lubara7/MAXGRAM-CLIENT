package io.github.lubara7.tgmax.localization

import java.util.Locale

enum class AppLanguage(val tag: String, val nativeName: String) {
    RUSSIAN("ru", "Русский"), ENGLISH("en", "English"), SPANISH("es", "Español"),
    UZBEK("uz", "O‘zbekcha"), PORTUGUESE("pt", "Português (Portugal)"),
    BRAZILIAN_PORTUGUESE("pt-BR", "Português (Brasil)"), FRENCH("fr", "Français"),
    BELARUSIAN("be", "Беларуская");

    companion object {
        fun normalize(tag: String): String {
            val locale = Locale.forLanguageTag(tag)
            return when {
                locale.language == "pt" && locale.country == "BR" -> "pt-BR"
                locale.language == "pt" -> "pt"
                else -> entries.firstOrNull { it.tag == locale.language }?.tag.orEmpty()
            }
        }
    }
}
