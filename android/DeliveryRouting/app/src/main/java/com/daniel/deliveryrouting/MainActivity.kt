package com.daniel.deliveryrouting

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
// import com.mapbox.maps.Mapbox
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.daniel.deliveryrouting.data.repository.BackendRepository
import com.daniel.deliveryrouting.data.api.models.AuthData
import com.daniel.deliveryrouting.data.api.models.LoginResponse
import com.daniel.deliveryrouting.data.api.models.LoginSuccessData
import com.daniel.deliveryrouting.data.api.models.PackageData
import com.daniel.deliveryrouting.data.api.models.SelectedCompany
import com.daniel.deliveryrouting.data.cache.CompanyCache
import com.daniel.deliveryrouting.data.cache.LoginCache
import com.daniel.deliveryrouting.presentation.packages.PackagesScreen
import com.daniel.deliveryrouting.presentation.map.PackageMapScreen
import com.daniel.deliveryrouting.presentation.company.CompanySelectionScreen
import com.daniel.deliveryrouting.presentation.company.CompanySelectionViewModel
import com.daniel.deliveryrouting.presentation.components.ViewToggle
import com.daniel.deliveryrouting.ui.theme.DeliveryRoutingTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val TAG_MAIN = "MainActivity"

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "🚀 MainActivity onCreate iniciado")
        
        // 🗺️ CONFIGURAR MAPBOX (temporalmente comentado)
        Log.d(TAG, "🗺️ Configurando Mapbox...")
        // Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        Log.d(TAG, "✅ Mapbox configurado exitosamente")
        
        Log.d(TAG, "🎨 Configurando UI con Compose...")
        setContent {
            DeliveryRoutingTheme(
                darkTheme = true // 🌙 FORZAR MODO OSCURO
            ) {
                Log.d(TAG, "🌙 Modo oscuro activado - Background: ${MaterialTheme.colorScheme.background}")
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginApp()
                }
            }
        }
    }
}


@Composable
fun LoginApp() {
    val context = LocalContext.current
    val loginCache = remember { LoginCache(context) }
    
    // 🔍 VERIFICAR SI EL LOGIN ESTÁ EXPIRADO
    val isLoginExpired = loginCache.isLoginExpired(24) // 24 horas
    val cachedLoginData = if (isLoginExpired) null else loginCache.getLoginData()
    
    // 🚨 TEMPORAL: Forzar pantalla de login para debug
    var isLoggedIn by remember { mutableStateOf(false) } // Siempre false para debug
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var societe by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var loginData by remember { mutableStateOf<LoginSuccessData?>(cachedLoginData) }
    
    // Estados para la pantalla de paquetes
    var packages by remember { mutableStateOf<List<PackageData>>(emptyList()) }
    var isLoadingPackages by remember { mutableStateOf(false) }
    var packagesError by remember { mutableStateOf("") }
    var packagesMessage by remember { mutableStateOf<String?>(null) }
    
    // Estado para navegación al mapa
    var showMap by remember { mutableStateOf(false) }
    
    // Estados para selección de empresa
    var showCompanySelection by remember { mutableStateOf(false) }
    var selectedCompany by remember { mutableStateOf<SelectedCompany?>(null) }
    var companies by remember { mutableStateOf<List<com.daniel.deliveryrouting.data.api.models.Company>>(emptyList()) }

    val repository = remember { BackendRepository(context) }
    val companyCache = remember { CompanyCache(context) }
    val scope = rememberCoroutineScope()
    
    // Cargar empresas y verificar empresa seleccionada al iniciar
    LaunchedEffect(Unit) {
        Log.d(TAG_MAIN, "🏢 Iniciando carga de empresas...")
        
        // Verificar si ya hay una empresa seleccionada
        val cachedCompany = companyCache.getSelectedCompany()
        if (cachedCompany != null) {
            selectedCompany = cachedCompany
            societe = cachedCompany.code
            Log.d(TAG_MAIN, "✅ Empresa ya seleccionada: ${cachedCompany.name} (${cachedCompany.code})")
        }
        
        // Cargar empresas desde la API si no hay cache válido
        if (!companyCache.hasValidCache()) {
            Log.d(TAG_MAIN, "🔄 Cache expirado, cargando empresas desde API...")
            try {
                val result = repository.getCompanies()
                result.fold(
                    onSuccess = { companyListResponse ->
                        Log.d(TAG_MAIN, "🎉 Empresas cargadas exitosamente: ${companyListResponse.companies.size} empresas")
                        companyCache.saveCompanies(companyListResponse)
                        companies = companyListResponse.companies
                    },
                    onFailure = { error ->
                        Log.e(TAG_MAIN, "❌ Error cargando empresas: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG_MAIN, "❌ Excepción cargando empresas: ${e.message}")
            }
        } else {
            Log.d(TAG_MAIN, "✅ Cache válido encontrado, usando empresas en cache")
            val cachedCompanies = companyCache.getCompanies()
            if (cachedCompanies != null) {
                companies = cachedCompanies.companies
            }
        }
    }

    // Función para cargar paquetes
    val loadPackages: () -> Unit = {
        scope.launch {
            isLoadingPackages = true
            packagesError = ""
            
            try {
                val result = repository.getPackages(
                    matricule = loginData?.matricule ?: "",
                    societe = selectedCompany?.code ?: ""
                )
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            packages = response.packages ?: emptyList()
                            packagesMessage = response.message // Capturar mensaje del backend
                            Log.d(TAG_MAIN, "📦 Paquetes cargados: ${packages.size}, mensaje: ${packagesMessage}")
                        } else {
                            packagesError = response.error?.message ?: "Error obteniendo paquetes"
                            packagesMessage = null
                        }
                    },
                    onFailure = { error ->
                        packagesError = "Error de conexión: ${error.message}"
                        packagesMessage = null
                    }
                )
            } catch (e: Exception) {
                packagesError = "Error inesperado: ${e.message}"
            } finally {
                isLoadingPackages = false
            }
        }
    }

    if (isLoggedIn && loginData != null) {
        // 🎛️ TOGGLE DE VISTA en la esquina superior izquierda
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Toggle en esquina superior izquierda
            ViewToggle(
                isMapView = !showMap, // showMap = false significa mapa (vista principal)
                onToggle = { isMap ->
                    showMap = !isMap // Invertir lógica: isMap = true significa mostrar mapa
                    Log.d(TAG_MAIN, "🔄 Cambiando vista: ${if (isMap) "Mapa" else "Lista"}")
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
            
            // Contenido principal
            if (showMap) {
                // 📦 PANTALLA DE PAQUETES (vista secundaria)
                Log.d(TAG_MAIN, "📦 Mostrando pantalla de paquetes con ${packages.size} paquetes")
                PackagesScreen(
                    packages = packages,
                    isLoading = isLoadingPackages,
                    onRefresh = loadPackages,
                    onLogout = {
                        isLoggedIn = false
                        loginData = null
                        packages = emptyList()
                        errorMessage = ""
                        packagesError = ""
                        packagesMessage = null
                        showMap = false
                        
                        // 🗑️ LIMPIAR CACHE AL HACER LOGOUT
                        loginCache.clearLoginData()
                        Log.d(TAG_MAIN, "🗑️ Cache de login limpiado")
                    },
                    onMapClick = { 
                        Log.d(TAG_MAIN, "🗺️ Navegando a pantalla de mapa")
                        showMap = false // Cambiar a false para ir al mapa
                    },
                    message = packagesMessage
                )
            } else {
                // 🗺️ PANTALLA DE MAPA (vista principal)
                Log.d(TAG_MAIN, "🗺️ Mostrando pantalla de mapa con ${packages.size} paquetes")
                PackageMapScreen(
                    onNavigateBack = { 
                        Log.d(TAG_MAIN, "⬅️ Regresando a pantalla de paquetes")
                        showMap = true // Cambiar a true para ir a paquetes
                    }
                )
            }
        }
        
        // Cargar paquetes automáticamente al entrar
        LaunchedEffect(loginData) {
            loadPackages()
        }
    } else if (showCompanySelection) {
        // 🏢 PANTALLA DE SELECCIÓN DE EMPRESA
        CompanySelectionScreen(
            companies = companies,
            onCompanySelected = { company ->
                selectedCompany = company
                societe = company.code
                showCompanySelection = false
            },
            onBack = {
                showCompanySelection = false
            }
        )
    } else {
        // ✅ PANTALLA DE LOGIN
        LoginScreen(
            username = username,
            password = password,
            societe = societe,
            selectedCompany = selectedCompany,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onUsernameChange = { username = it },
            onPasswordChange = { password = it },
            onSelectCompanyClick = {
                showCompanySelection = true
            },
            onLoginClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = ""

                    try {
                        // ✅ LLAMADA REAL AL BACKEND
                        val result = repository.login(username, password, societe)

                        result.fold(
                            onSuccess = { loginResponse ->
                                if (loginResponse.success) {
                                    val fullUsername = "${societe}_${username}"
                                    val matricule = loginResponse.authentication?.matricule ?: fullUsername
                                    val token = loginResponse.authentication?.token ?: ""
                                    loginData = LoginSuccessData(fullUsername, matricule, token)
                                    isLoggedIn = true
                                    
                                    // 💾 GUARDAR EN CACHE PARA PERSISTENCIA
                                    loginCache.saveLoginData(loginData!!, selectedCompany)
                                    Log.d(TAG_MAIN, "💾 Datos de login guardados en cache")
                                } else {
                                    errorMessage = loginResponse.error?.message ?: "Error en el login"
                                }
                            },
                            onFailure = { error ->
                                errorMessage = "Error de conexión: ${error.message}"
                            }
                        )
                    } catch (e: Exception) {
                        errorMessage = "Error inesperado: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            }
        )
    }
}

@Composable
fun LoginScreen(
    username: String,
    password: String,
    societe: String,
    selectedCompany: SelectedCompany?,
    isLoading: Boolean,
    errorMessage: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSelectCompanyClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Delivery Routing Login",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de empresa seleccionada
        OutlinedTextField(
            value = selectedCompany?.name ?: "Seleccionar empresa...",
            onValueChange = { /* No editable */ },
            label = { Text("Empresa") },
            singleLine = true,
            readOnly = true,
            trailingIcon = {
                TextButton(onClick = onSelectCompanyClick) {
                    Text("Cambiar")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        if (selectedCompany != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Código: ${selectedCompany.code}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = onLoginClick,
                enabled = username.isNotBlank() && password.isNotBlank() && selectedCompany != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar Sesión")
            }
        }

        if (errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}