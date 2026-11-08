package io.github.lubara7.tgmax.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageController {
    fun currentTag(): String = AppLanguage.normalize(
        AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag().orEmpty(),
    )

    /** Empty tag means follow the system; AppCompat owns persistence and system synchronization. */
    fun select(tag: String) {
        require(tag.isEmpty() || AppLanguage.entries.any { it.tag == tag })
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}
