package com.daniel.deliveryrouting.presentation.packages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.daniel.deliveryrouting.data.api.models.PackageData
import com.daniel.deliveryrouting.presentation.components.StatusIcon
import com.daniel.deliveryrouting.presentation.components.StatusText
import android.util.Log

private const val TAG_PACKAGES = "PackagesScreen"

@Composable
fun PackagesScreen(
    packages: List<PackageData>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onMapClick: () -> Unit = {},
    message: String? = null
) {
    Log.d(TAG_PACKAGES, "📦 PackagesScreen iniciado con ${packages.size} paquetes, isLoading: $isLoading")
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header compacto
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = "Paquetes",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = packages.size.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
            
            Row {
                // Botón de sincronizar
                IconButton(
                    onClick = {
                        Log.d(TAG_PACKAGES, "🔄 Botón de sincronizar presionado")
                        onRefresh()
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sincronizar"
                        )
                    }
                }
                
                // Botón de logout
                TextButton(onClick = {
                    Log.d(TAG_PACKAGES, "🚪 Botón de logout presionado")
                    onLogout()
                }) {
                    Text("Salir")
                }
            }
        }
        
        // Lista de paquetes - usa todo el espacio disponible
        if (packages.isEmpty() && !isLoading) {
            // Estado vacío - mostrar mensaje del backend si existe
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Icono de estado
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "Estado",
                        modifier = Modifier.size(64.dp),
                        tint = if (message?.contains("completada") == true) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Mensaje principal
                    Text(
                        text = message ?: "No hay paquetes disponibles",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (message?.contains("completada") == true) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Mensaje secundario
                    Text(
                        text = if (message?.contains("completada") == true) 
                            "La jornada de trabajo ha terminado" 
                        else 
                            "Usa el botón de sincronizar para actualizar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(packages) { packageItem ->
                    PackageCard(packageItem = packageItem)
                }
            }
        }
    }
}

@Composable
fun PackageCard(packageItem: PackageData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header con tracking number y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = packageItem.trackingNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusIcon(status = packageItem.status)
                    StatusText(status = packageItem.status)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Información del destinatario
            Text(
                text = packageItem.recipientName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Dirección (más compacta)
            Text(
                text = packageItem.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Instrucciones si existen
            if (packageItem.instructions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = packageItem.instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
