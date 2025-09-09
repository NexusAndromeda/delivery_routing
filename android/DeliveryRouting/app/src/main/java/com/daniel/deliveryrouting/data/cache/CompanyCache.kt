package com.daniel.deliveryrouting.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.daniel.deliveryrouting.data.api.models.Company
import com.daniel.deliveryrouting.data.api.models.CompanyListResponse
import com.daniel.deliveryrouting.data.api.models.SelectedCompany
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Sistema de cache local para empresas de Colis Privé
 */
class CompanyCache(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("company_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_COMPANIES_CACHE = "companies_cache"
        private const val KEY_SELECTED_COMPANY = "selected_company"
        private const val KEY_CACHE_TIMESTAMP = "cache_timestamp"
        private const val CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 horas
    }
    
    /**
     * Guardar lista de empresas en cache
     */
    fun saveCompanies(companyListResponse: CompanyListResponse) {
        val json = gson.toJson(companyListResponse)
        prefs.edit()
            .putString(KEY_COMPANIES_CACHE, json)
            .putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Obtener lista de empresas del cache
     */
    fun getCompanies(): CompanyListResponse? {
        val json = prefs.getString(KEY_COMPANIES_CACHE, null) ?: return null
        val timestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0)
        
        // Verificar si el cache ha expirado
        if (System.currentTimeMillis() - timestamp > CACHE_DURATION) {
            clearCache()
            return null
        }
        
        return try {
            gson.fromJson(json, CompanyListResponse::class.java)
        } catch (e: Exception) {
            clearCache()
            null
        }
    }
    
    /**
     * Verificar si hay cache válido
     */
    fun hasValidCache(): Boolean {
        val timestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0)
        return System.currentTimeMillis() - timestamp <= CACHE_DURATION
    }
    
    /**
     * Guardar empresa seleccionada
     */
    fun saveSelectedCompany(company: SelectedCompany) {
        val json = gson.toJson(company)
        prefs.edit()
            .putString(KEY_SELECTED_COMPANY, json)
            .apply()
    }
    
    /**
     * Obtener empresa seleccionada
     */
    fun getSelectedCompany(): SelectedCompany? {
        val json = prefs.getString(KEY_SELECTED_COMPANY, null) ?: return null
        return try {
            gson.fromJson(json, SelectedCompany::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Limpiar cache
     */
    fun clearCache() {
        prefs.edit()
            .remove(KEY_COMPANIES_CACHE)
            .remove(KEY_CACHE_TIMESTAMP)
            .apply()
    }
    
    /**
     * Limpiar empresa seleccionada
     */
    fun clearSelectedCompany() {
        prefs.edit()
            .remove(KEY_SELECTED_COMPANY)
            .apply()
    }
    
    /**
     * Obtener estadísticas del cache
     */
    fun getCacheStats(): CacheStats {
        val companies = getCompanies()
        val selectedCompany = getSelectedCompany()
        
        return CacheStats(
            hasValidCache = hasValidCache(),
            companiesCount = companies?.companies?.size ?: 0,
            hasSelectedCompany = selectedCompany != null,
            lastUpdate = prefs.getLong(KEY_CACHE_TIMESTAMP, 0)
        )
    }
}

/**
 * Estadísticas del cache
 */
data class CacheStats(
    val hasValidCache: Boolean,
    val companiesCount: Int,
    val hasSelectedCompany: Boolean,
    val lastUpdate: Long
)
