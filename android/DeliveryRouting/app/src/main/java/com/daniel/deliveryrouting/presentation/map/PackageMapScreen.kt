package com.daniel.deliveryrouting.presentation.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daniel.deliveryrouting.presentation.components.ViewToggle

@Composable
fun PackageMapScreen(
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Toggle en esquina superior izquierda
        ViewToggle(
            isMapView = true, // true porque estamos en el mapa
            onToggle = { isMap ->
                if (!isMap) { // Si se presiona lista, volver
                    onNavigateBack()
                }
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
        
        // Contenido del mapa
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Mapa de Paquetes",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
                    Text(
                text = "Funcionalidad en desarrollo",
                        style = MaterialTheme.typography.bodyLarge
                    )
            
            Spacer(modifier = Modifier.height(8.dp))
            
                    Text(
                text = "Aquí se mostrará el mapa con los paquetes",
                        style = MaterialTheme.typography.bodyMedium
                    )
        }
    }
}