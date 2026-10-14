package com.gestor.medicamentos.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestor.medicamentos.ui.components.PhotoPicker
import com.gestor.medicamentos.ui.theme.*

/** Primera pantalla: el nombre es obligatorio, la foto es opcional. No hay forma de saltarla. */
@Composable
fun OnboardingScreen(
    isDark: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onDone: (name: String, photo: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(Modifier.fillMaxSize()) {
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Text(if (isDark) "☀️" else "🌙", fontSize = 22.sp)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("💊", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Gestor de Medicamentos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Personaliza tu perfil para comenzar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))
                PhotoPicker(photo, { photo = it }, Modifier.size(150.dp).border(4.dp, Turquoise, CircleShape))
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    name, { name = it },
                    label = { Text("Tu nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onDone(name.trim().ifBlank { "Usuario" }, photo) },
                    enabled = true,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Continuar") }
            }
        }
    }
}
