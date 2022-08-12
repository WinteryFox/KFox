package dev.bitflow.kfox.localization

import dev.kord.common.Locale
import mu.KotlinLogging
import java.util.*
import kotlin.NullPointerException

class ResourceBundleTranslationProvider(
    override val defaultLocale: Locale = Locale.ENGLISH_UNITED_STATES,
    override val supportedLocales: Set<Locale> = Locale.ALL.toSet(),
    override val defaultModule: String = "default"
) : TranslationProvider {
    private val logger = KotlinLogging.logger { }

    private fun getModule(module: String, locale: Locale): ResourceBundle? =
        try {
            ResourceBundle.getBundle("modules/$module", locale.asJavaLocale())
        } catch (exception: MissingResourceException) {
            logger.warn { "Couldn't get resource bundle \"$module\" for locale \"$locale\"" }
            null
        }

    override fun supportsLocale(locale: Locale, module: String): Boolean =
        getModule(module, locale) != null

    override fun getString(key: String, vararg params: Any, locale: Locale, module: String): String {
        logger.trace { "Fetching key \"$key\" locale \"$locale\" and module \"$module\"" }
        val m = getModule(module, locale)
        if (m == null) {
            logger.warn { "Translation module not found \"$module\"" }
            return key
        }

        return format(
            try {
                m.getString(key)
            } catch (e: MissingResourceException) {
                key
            },
            *params
        )
    }

    override fun getAllStrings(key: String, module: String): Map<Locale, String> =
        supportedLocales.associateWith { getString(key, locale = it, module = module) }
}
