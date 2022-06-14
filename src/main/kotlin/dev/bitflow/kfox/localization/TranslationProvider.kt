package dev.bitflow.kfox.localization

import dev.kord.common.Locale

interface TranslationProvider {
    val defaultModule: String

    val defaultLocale: Locale

    fun supportsLocale(locale: Locale, module: String = defaultModule): Boolean

    fun getAllLocales(module: String = defaultModule): Set<Locale>

    fun getString(key: String, vararg params: List<Any>, locale: Locale = defaultLocale, module: String = defaultModule): String

    fun getAllStrings(key: String, vararg params: List<Any>, module: String = defaultModule): Map<Locale, String>
}
