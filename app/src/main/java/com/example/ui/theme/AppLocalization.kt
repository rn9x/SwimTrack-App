package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import com.example.domain.model.AppLanguage

val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }

@Composable
@ReadOnlyComposable
fun tr(en: String, ar: String): String {
    return if (LocalAppLanguage.current == AppLanguage.ARABIC) ar else en
}

fun tr(language: AppLanguage, en: String, ar: String): String {
    return if (language == AppLanguage.ARABIC) ar else en
}
