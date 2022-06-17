package dev.bitflow.kfox.localization

import com.ibm.icu.text.MessageFormat
import dev.kord.common.Locale

interface TranslationProvider {
    val defaultModule: String

    val defaultLocale: Locale

    fun supportsLocale(locale: Locale, module: String = defaultModule): Boolean

    fun getAllLocales(module: String = defaultModule): Set<Locale>

    fun getString(
        key: String,
        vararg params: Any,
        locale: Locale = defaultLocale,
        module: String = defaultModule
    ): String

    fun getAllStrings(key: String, module: String = defaultModule): Map<Locale, String>

    fun format(value: String, vararg params: Any): String = MessageFormat.format(value, *params)
}
