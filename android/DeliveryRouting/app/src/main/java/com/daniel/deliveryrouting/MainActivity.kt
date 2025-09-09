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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import com.daniel.deliveryrouting.data.repository.BackendRepository
import com.daniel.deliveryrouting.data.api.models.AuthData
import com.daniel.deliveryrouting.data.api.models.LoginResponse
import com.daniel.deliveryrouting.data.api.models.PackageData
import com.daniel.deliveryrouting.presentation.packages.PackagesScreen
import com.daniel.deliveryrouting.presentation.map.PackageMapScreen
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
            DeliveryRoutingTheme {
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

data class LoginSuccessData(
    val username: String,
    val matricule: String,
    val token: String
)

@Composable
fun LoginApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var societe by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var loginData by remember { mutableStateOf<LoginSuccessData?>(null) }
    
    // Estados para la pantalla de paquetes
    var packages by remember { mutableStateOf<List<PackageData>>(emptyList()) }
    var isLoadingPackages by remember { mutableStateOf(false) }
    var packagesError by remember { mutableStateOf("") }
    
    // Estado para navegación al mapa
    var showMap by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val repository = remember { BackendRepository(context) }
    val scope = rememberCoroutineScope()

    // Función para cargar paquetes
    val loadPackages: () -> Unit = {
        scope.launch {
            isLoadingPackages = true
            packagesError = ""
            
            try {
                val result = repository.getPackages(loginData?.matricule ?: "")
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            packages = response.packages ?: emptyList()
                        } else {
                            packagesError = response.error?.message ?: "Error obteniendo paquetes"
                        }
                    },
                    onFailure = { error ->
                        packagesError = "Error de conexión: ${error.message}"
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
        if (showMap) {
            // 🗺️ PANTALLA DE MAPA
            Log.d(TAG_MAIN, "🗺️ Mostrando pantalla de mapa con ${packages.size} paquetes")
            PackageMapScreen(
                packages = packages,
                onBackClick = { 
                    Log.d(TAG_MAIN, "⬅️ Regresando a pantalla de paquetes")
                    showMap = false 
                }
            )
        } else {
            // ✅ PANTALLA DE PAQUETES
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
                    showMap = false
                },
                onMapClick = { 
                    Log.d(TAG_MAIN, "🗺️ Navegando a pantalla de mapa")
                    showMap = true 
                }
            )
        }
        
        // Cargar paquetes automáticamente al entrar
        LaunchedEffect(loginData) {
            loadPackages()
        }
    } else {
        // ✅ PANTALLA DE LOGIN
        LoginScreen(
            username = username,
            password = password,
            societe = societe,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onUsernameChange = { username = it },
            onPasswordChange = { password = it },
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
    isLoading: Boolean,
    errorMessage: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
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

        OutlinedTextField(
            value = societe,
            onValueChange = { /* No editable */ },
            label = { Text("Sociedad") },
            singleLine = true,
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = onLoginClick,
                enabled = username.isNotBlank() && password.isNotBlank(),
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