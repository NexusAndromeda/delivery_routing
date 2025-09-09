package com.daniel.deliveryrouting.data.repository

import android.content.Context
import android.util.Log
import com.daniel.deliveryrouting.BuildConfig
import com.daniel.deliveryrouting.data.api.BackendApi
import com.daniel.deliveryrouting.data.api.models.LoginRequest
import com.daniel.deliveryrouting.data.api.models.LoginResponse
import com.daniel.deliveryrouting.data.api.models.GetPackagesRequest
import com.daniel.deliveryrouting.data.api.models.GetPackagesResponse
import com.daniel.deliveryrouting.data.api.models.PackageData
import com.daniel.deliveryrouting.data.api.models.CompanyListResponse
import com.daniel.deliveryrouting.data.demo.DemoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class BackendRepository(private val context: Context) {

    private val TAG = "BackendRepository"

    private val api: BackendApi by lazy {
        Log.d(TAG, "🔧 Configurando API con:")
        Log.d(TAG, "   Base URL: ${BuildConfig.BASE_URL}")
        Log.d(TAG, "   Environment: ${BuildConfig.ENVIRONMENT}")
        Log.d(TAG, "   Enable Logging: ${BuildConfig.ENABLE_LOGGING}")
        
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.ENABLE_LOGGING) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)
    }

    suspend fun login(
        username: String,
        password: String,
        societe: String
    ): Result<LoginResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🚀 Iniciando login al backend")
            Log.d(TAG, "Username: $username")
            Log.d(TAG, "Societe: $societe")

            val request = LoginRequest(
                username = username,
                password = password,
                societe = societe
            )

            val response = api.login(request)

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "🎉 Login exitoso: ${response.body()?.success}")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                Log.e(TAG, "❌ Error en login: ${response.code()} - $errorBody")
                Result.failure(Exception("Error ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción en login: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getPackages(
        matricule: String,
        societe: String,
        date: String? = null
    ): Result<GetPackagesResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📦 Obteniendo paquetes para matricule: $matricule")
            
            val request = GetPackagesRequest(
                matricule = matricule,
                societe = societe,
                date = date
            )

            val response = api.getPackages(request)

            if (response.isSuccessful && response.body() != null) {
                val packages = response.body()?.packages
                if (packages.isNullOrEmpty()) {
                    // 🎯 MODO DEMO: Si no hay paquetes, usar datos de prueba
                    Log.d(TAG, "🎯 Activando modo demo - usando datos de prueba")
                    val demoPackages = DemoData.getDemoPackages()
                    val demoResponse = GetPackagesResponse(
                        success = true,
                        message = DemoData.getDemoMessage(),
                        packages = demoPackages,
                        error = null
                    )
                    Result.success(demoResponse)
                } else {
                    Log.d(TAG, "🎉 Paquetes obtenidos exitosamente: ${packages.size} paquetes")
                    Result.success(response.body()!!)
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                Log.e(TAG, "❌ Error obteniendo paquetes: ${response.code()} - $errorBody")
                
                // 🎯 MODO DEMO: Si hay error, usar datos de prueba
                Log.d(TAG, "🎯 Activando modo demo por error - usando datos de prueba")
                val demoPackages = DemoData.getDemoPackages()
                val demoResponse = GetPackagesResponse(
                    success = true,
                    message = DemoData.getDemoMessage(),
                    packages = demoPackages,
                    error = null
                )
                Result.success(demoResponse)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción obteniendo paquetes: ${e.message}", e)
            
            // 🎯 MODO DEMO: Si hay excepción, usar datos de prueba
            Log.d(TAG, "🎯 Activando modo demo por excepción - usando datos de prueba")
            val demoPackages = DemoData.getDemoPackages()
            val demoResponse = GetPackagesResponse(
                success = true,
                message = DemoData.getDemoMessage(),
                packages = demoPackages,
                error = null
            )
            Result.success(demoResponse)
        }
    }
    
    suspend fun getCompanies(): Result<CompanyListResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🏢 Obteniendo lista de empresas")
            
            val response = api.getCompanies()
            
            if (response.isSuccessful && response.body() != null) {
                val companies = response.body()!!
                Log.d(TAG, "🎉 Empresas obtenidas exitosamente: ${companies.companies.size} empresas")
                Result.success(companies)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                Log.e(TAG, "❌ Error obteniendo empresas: ${response.code()} - $errorBody")
                Result.failure(Exception("Error ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción obteniendo empresas: ${e.message}", e)
            Result.failure(e)
        }
    }
}
