package dev.bitflow.kfox.localization

import dev.kord.common.Locale
import java.text.MessageFormat
import java.util.ResourceBundle
import kotlin.NullPointerException

class ResourceBundleTranslationProvider(
    override val defaultModule: String,
    override val defaultLocale: Locale,
    private val modules: Map<String, Map<Locale, ResourceBundle>>
) : TranslationProvider {
    override fun supportsLocale(locale: Locale, module: String): Boolean =
        modules[module]?.get(locale) != null

    override fun getAllLocales(module: String): Set<Locale> =
        modules[module]?.map { it.key }?.toSet()
            ?: throw NullPointerException("Translation module not found \"$module\"")

    override fun getString(key: String, vararg params: List<Any>, locale: Locale, module: String): String {
        val bundle =
            modules[module]?.get(locale) ?: throw NullPointerException("Translation module not found \"$module\"")
        val string = try {
            bundle.getString(key)
        } catch (_: NullPointerException) {
            key
        }
        return MessageFormat.format(string, params)
    }

    override fun getAllStrings(key: String, vararg params: List<Any>, module: String): Map<Locale, String> =
        modules[module]?.mapValues {
            try {
                it.value.getString(key)
            } catch (_: NullPointerException) {
                key
            }
        } ?: throw NullPointerException("Translation module not found \"$module\"")
}
