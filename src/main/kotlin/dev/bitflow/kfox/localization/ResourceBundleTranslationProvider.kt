package dev.bitflow.kfox.localization

import dev.kord.common.Locale
import mu.KotlinLogging
import java.io.File
import java.net.URLClassLoader
import java.util.*
import kotlin.NullPointerException

class ResourceBundleTranslationProvider : TranslationProvider {
    private val logger = KotlinLogging.logger { }
    private val modules: Map<String, Map<Locale, ResourceBundle>>

    override val defaultModule: String
    override val defaultLocale: Locale

    constructor(defaultModule: String, defaultLocale: Locale, path: String = "/modules") {
        this.defaultModule = defaultModule
        this.defaultLocale = defaultLocale
        this.modules = File(ResourceBundleTranslationProvider::class.java.getResource(path).file)
            .listFiles()
            .groupBy { it.nameWithoutExtension.substringBefore("_") }
            .map { module ->
                module.key to module.value.associate {
                    val l = it.nameWithoutExtension.substringAfter("_")
                    val locale = Locale(
                        l.substringBefore("_"),
                        l.substringAfter("_", "").ifBlank { null }
                    )

                    locale to ResourceBundle.getBundle(
                        "${path.substringAfter("/")}.${module.key}",
                        locale.asJavaLocale()
                    )
                }
            }.toMap()
    }

    constructor(defaultModule: String, defaultLocale: Locale, modules: Map<String, Map<Locale, ResourceBundle>>) {
        this.defaultModule = defaultModule
        this.defaultLocale = defaultLocale
        this.modules = modules
    }

    override fun supportsLocale(locale: Locale, module: String): Boolean =
        modules[module]?.get(locale) != null

    override fun getAllLocales(module: String): Set<Locale> =
        modules[module]?.map { it.key }?.toSet()
            ?: throw NullPointerException("Translation module not found \"$module\"")

    override fun getString(key: String, vararg params: Any, locale: Locale, module: String): String {
        logger.trace { "Fetching key \"$key\" locale \"$locale\" and module \"$module\"" }
        val m = modules[module]
        if (m == null) {
            logger.warn { "Translation module not found \"$module\"" }
            return key
        }
        val bundle = m[locale] ?: m[defaultLocale]
        ?: throw IllegalStateException("Default locale \"$locale\" for module \"$module\" is missing")

        return format(
            try {
                bundle.getString(key)
            } catch (e: MissingResourceException) {
                key
            },
            *params
        )
    }

    override fun getAllStrings(key: String, module: String): Map<Locale, String> =
        modules[module]?.mapValues {
            getString(key, locale = it.key, module = module)
        } ?: throw NullPointerException("Translation module not found \"$module\" with key \"$key\"")
}
