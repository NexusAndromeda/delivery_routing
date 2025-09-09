package com.daniel.deliveryrouting.presentation.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class PackageMapUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

class PackageMapViewModel : ViewModel() {
    var uiState by mutableStateOf(PackageMapUiState())
        private set
}
