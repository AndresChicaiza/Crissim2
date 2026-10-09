package com.example.crissim.domain.model

/**
 * Texto localizado bilingüe.
 * El español (es) es obligatorio; el inglés (en) es opcional.
 */
data class LocalizedText(
    val es: String,
    val en: String? = null
) {
    /**
     * Devuelve el texto en el idioma solicitado.
     * Si no existe la traducción, devuelve español como fallback.
     */
    fun get(language: String = "es"): String =
        when (language) {
            "en" -> en ?: es
            else -> es
        }

    override fun toString(): String = es
}
