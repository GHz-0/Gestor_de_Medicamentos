package com.gestor.medicamentos.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestor.medicamentos.data.Medication
import com.gestor.medicamentos.data.Profile
import com.gestor.medicamentos.ui.screens.*
import com.gestor.medicamentos.ui.theme.GestorTheme

sealed interface Screen {
    data object Home : Screen
    data class Form(val med: Medication?) : Screen
    data object History : Screen
    data object ProfileEdit : Screen
}

@Composable
fun AppRoot(vm: MedViewModel = viewModel()) {
    val ctx = LocalContext.current
    val prefs = remember(ctx) { ctx.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE) }

    // Read saved preference: -1 = system default (null), 0 = light (false), 1 = dark (true)
    var darkThemeOverride by remember {
        val saved = prefs.getInt("dark_theme", -1)
        mutableStateOf(if (saved == -1) null else saved == 1)
    }

    val systemDark = isSystemInDarkTheme()
    val isDark = darkThemeOverride ?: systemDark

    val toggleTheme = {
        val next = !isDark
        darkThemeOverride = next
        prefs.edit().putInt("dark_theme", if (next) 1 else 0).apply()
    }

    GestorTheme(darkTheme = isDark) {
        val profiles by vm.profiles.collectAsState()
        var screen by remember { mutableStateOf<Screen>(Screen.Home) }
        BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

        val profile = profiles?.firstOrNull()
        when {
            profiles == null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            // Sin nombre configurado no se puede avanzar: siempre vuelve aquí al abrir la app
            profile == null -> OnboardingScreen(isDark = isDark, onToggleTheme = toggleTheme) { name, photo -> vm.saveProfile(Profile(name = name, photoPath = photo)) }
            else -> when (val s = screen) {
                Screen.Home -> HomeScreen(
                    vm, profile,
                    isDark = isDark, onToggleTheme = toggleTheme,
                    onOpenProfile = { screen = Screen.ProfileEdit },
                    onAdd = { screen = Screen.Form(null) },
                    onEdit = { screen = Screen.Form(it) },
                    onHistory = { screen = Screen.History }
                )
                is Screen.Form -> MedFormScreen(
                    s.med, isDark = isDark, onToggleTheme = toggleTheme,
                    onSave = { vm.saveMed(it); screen = Screen.Home }, onBack = { screen = Screen.Home }
                )
                Screen.History -> HistoryScreen(
                    vm, isDark = isDark, onToggleTheme = toggleTheme,
                    onBack = { screen = Screen.Home }
                )
                Screen.ProfileEdit -> ProfileScreen(
                    profile, isDark = isDark, onToggleTheme = toggleTheme,
                    onSave = { vm.saveProfile(it) }, onBack = { screen = Screen.Home }
                )
            }
        }
    }
}
