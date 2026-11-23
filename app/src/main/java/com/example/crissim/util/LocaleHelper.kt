package com.example.crissim.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import com.example.crissim.R
import java.util.Locale

/**
 * Helper para gestionar el cambio de idioma de la aplicación.
 * Persiste la preferencia en SharedPreferences y aplica el locale globalmente.
 */
object LocaleHelper {

    private const val PREFS_NAME = "crisissim_prefs"
    private const val KEY_LANGUAGE = "app_language"
    private const val DEFAULT_LANGUAGE = "es"

    /**
     * Obtiene el idioma guardado o el por defecto.
     */
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    /**
     * Guarda el idioma y actualiza el locale de la app.
     */
    fun setLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
        updateLocale(context, language)
    }

    /**
     * Aplica el locale al contexto dado (para Activities, Application, etc).
     */
    @SuppressLint("ApplySharedPref")
    fun updateLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val config: Configuration = resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            config.setLocale(locale)
        }

        resources.updateConfiguration(config, resources.displayMetrics)
    }

    /**
     * Crea un nuevo Context con el locale aplicado (para usar en Application.attachBaseContext).
     */
    fun attachBaseContext(base: Context): Context {
        val language = getLanguage(base)
        return updateBaseContextLocale(base, language)
    }

    private fun updateBaseContextLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val config: Configuration = resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            config.setLocale(locale)
        }

        return context.createConfigurationContext(config)
    }

    /**
     * Verifica si el idioma actual es español.
     */
    fun isSpanish(context: Context): Boolean = getLanguage(context) == "es"

    /**
     * Verifica si el idioma actual es inglés.
     */
    fun isEnglish(context: Context): Boolean = getLanguage(context) == "en"

    /**
     * Obtiene el nombre legible del idioma actual.
     */
    fun getLanguageDisplayName(context: Context): String {
        return when (getLanguage(context)) {
            "es" -> context.getString(R.string.welcome_lang_es)
            "en" -> context.getString(R.string.welcome_lang_en)
            else -> context.getString(R.string.welcome_lang_es)
        }
    }
}