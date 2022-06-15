package dev.bitflow.kfox.localization

import dev.kord.common.Locale
import mu.KotlinLogging
import java.text.MessageFormat
import java.util.*
import kotlin.NullPointerException

class ResourceBundleTranslationProvider(
    override val defaultModule: String,
    override val defaultLocale: Locale,
    private val modules: Map<String, Map<Locale, ResourceBundle>>
) : TranslationProvider {
    private val logger = KotlinLogging.logger { }

    override fun supportsLocale(locale: Locale, module: String): Boolean =
        modules[module]?.get(locale) != null

    override fun getAllLocales(module: String): Set<Locale> =
        modules[module]?.map { it.key }?.toSet()
            ?: throw NullPointerException("Translation module not found \"$module\"")

    override fun getString(key: String, vararg params: List<Any>, locale: Locale, module: String): String {
        val bundle =
            modules[module]?.get(locale) ?: throw NullPointerException("Translation module not found \"$module\"")
        logger.trace { "Fetching key \"$key\" locale \"$locale\" and module \"$module\"" }
        return MessageFormat.format(
            try {
                bundle.getString(key)
            } catch (_: MissingResourceException) {
                logger.warn { "Missing key \"$key\" locale \"${locale}\" and module \"$module\"" }
                key
            },
            params
        )
    }

    override fun getAllStrings(key: String, vararg params: List<Any>, module: String): Map<Locale, String> =
        modules[module]?.mapValues {
            getString(key, locale = it.key, module = module)
        } ?: throw NullPointerException("Translation module not found \"$module\"")
}
