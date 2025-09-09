package com.daniel.deliveryrouting.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StatusIcon(
    status: String,
    modifier: Modifier = Modifier
) {
    val isDelivered = status.uppercase().contains("LIVRER")
    val backgroundColor = if (isDelivered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val contentColor = if (isDelivered) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
    val icon = if (isDelivered) Icons.Filled.Home else Icons.Filled.Person

    Box(
        modifier = modifier
            .size(32.dp)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = status,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun StatusText(
    status: String,
    modifier: Modifier = Modifier
) {
    val displayText = when {
        status.uppercase().contains("LIVRER") -> "Entregado"
        else -> "Pendiente"
    }

    Text(
        text = displayText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}