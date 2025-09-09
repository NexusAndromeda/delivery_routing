package com.daniel.deliveryrouting.presentation.company

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daniel.deliveryrouting.data.api.models.Company
import com.daniel.deliveryrouting.data.api.models.SelectedCompany

/**
 * Pantalla de selección de empresa con barra de búsqueda
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanySelectionScreen(
    companies: List<Company>,
    onCompanySelected: (SelectedCompany) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filteredCompanies by remember { mutableStateOf(companies) }
    
    // Filtrar empresas en tiempo real
    LaunchedEffect(searchQuery, companies) {
        filteredCompanies = if (searchQuery.isBlank()) {
            companies
        } else {
            companies.filter { company ->
                company.name.contains(searchQuery, ignoreCase = true) ||
                company.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Atrás")
            }
            Text(
                text = "Seleccionar Empresa",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(80.dp)) // Para centrar el título
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Barra de búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar empresa...") },
            placeholder = { Text("Escribe el nombre o código de la empresa") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Contador de resultados
        Text(
            text = "${filteredCompanies.size} empresas encontradas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Lista de empresas
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredCompanies) { company ->
                CompanyItem(
                    company = company,
                    onClick = {
                        onCompanySelected(
                            SelectedCompany(
                                name = company.name,
                                code = company.code
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CompanyItem(
    company: Company,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = company.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Código: ${company.code}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            company.description?.let { description ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
