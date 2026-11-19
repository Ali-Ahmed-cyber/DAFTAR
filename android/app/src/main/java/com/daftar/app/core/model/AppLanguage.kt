package com.daftar.app.core.model

enum class AppLanguage(val code: String, val isRtl: Boolean) {
    ARABIC("ar", true),
    ENGLISH("en", false);

    companion object {
        fun fromCode(code: String): AppLanguage =
            if (code.equals("en", ignoreCase = true)) ENGLISH else ARABIC
    }
}

enum class AppThemeMode(val id: String) {
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromId(id: String): AppThemeMode =
            if (id.equals("dark", ignoreCase = true)) DARK else LIGHT
    }
}

data class BilingualText(
    val ar: String,
    val en: String,
) {
    fun get(lang: AppLanguage): String = if (lang == AppLanguage.ARABIC) ar else en
}
