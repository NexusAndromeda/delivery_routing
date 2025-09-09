package com.daniel.deliveryrouting.presentation.company

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daniel.deliveryrouting.data.api.BackendApi
import com.daniel.deliveryrouting.data.api.models.Company
import com.daniel.deliveryrouting.data.api.models.CompanyListResponse
import com.daniel.deliveryrouting.data.api.models.SelectedCompany
import com.daniel.deliveryrouting.data.cache.CompanyCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la selección de empresas
 */
class CompanySelectionViewModel(
    private val backendApi: BackendApi,
    private val companyCache: CompanyCache
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CompanySelectionUiState())
    val uiState: StateFlow<CompanySelectionUiState> = _uiState.asStateFlow()
    
    init {
        loadCompanies()
    }
    
    /**
     * Cargar empresas (primero del cache, luego de la API si es necesario)
     */
    private fun loadCompanies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Intentar cargar del cache primero
                val cachedCompanies = companyCache.getCompanies()
                
                if (cachedCompanies != null && companyCache.hasValidCache()) {
                    // Usar datos del cache
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        companies = cachedCompanies.companies,
                        isFromCache = true
                    )
                } else {
                    // Cargar de la API
                    loadFromApi()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error cargando empresas: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Cargar empresas desde la API
     */
    private suspend fun loadFromApi() {
        try {
            val response = backendApi.getCompanies()
            
            if (response.isSuccessful) {
                val companyListResponse = response.body()!!
                
                // Guardar en cache
                companyCache.saveCompanies(companyListResponse)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    companies = companyListResponse.companies,
                    isFromCache = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error del servidor: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Error de conexión: ${e.message}"
            )
        }
    }
    
    /**
     * Seleccionar una empresa
     */
    fun selectCompany(company: SelectedCompany) {
        companyCache.saveSelectedCompany(company)
        _uiState.value = _uiState.value.copy(selectedCompany = company)
    }
    
    /**
     * Obtener empresa seleccionada
     */
    fun getSelectedCompany(): SelectedCompany? {
        return companyCache.getSelectedCompany()
    }
    
    /**
     * Recargar empresas
     */
    fun refreshCompanies() {
        companyCache.clearCache()
        loadCompanies()
    }
    
    /**
     * Limpiar error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * Estado de la UI para la selección de empresas
 */
data class CompanySelectionUiState(
    val isLoading: Boolean = false,
    val companies: List<Company> = emptyList(),
    val selectedCompany: SelectedCompany? = null,
    val isFromCache: Boolean = false,
    val error: String? = null
)
