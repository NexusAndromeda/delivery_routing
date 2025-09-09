package com.daniel.deliveryrouting.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.daniel.deliveryrouting.data.api.models.LoginSuccessData
import com.daniel.deliveryrouting.data.api.models.SelectedCompany

class LoginCache(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("login_cache", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_SOCIETE = "societe"
        private const val KEY_MATRICULE = "matricule"
        private const val KEY_SELECTED_COMPANY_NAME = "selected_company_name"
        private const val KEY_SELECTED_COMPANY_CODE = "selected_company_code"
        private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"
    }
    
    fun saveLoginData(loginData: LoginSuccessData, selectedCompany: SelectedCompany?) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USERNAME, loginData.username)
            putString(KEY_SOCIETE, loginData.username.split("_").firstOrNull() ?: "")
            putString(KEY_MATRICULE, loginData.matricule)
            putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
            
            selectedCompany?.let { company ->
                putString(KEY_SELECTED_COMPANY_NAME, company.name)
                putString(KEY_SELECTED_COMPANY_CODE, company.code)
            }
            
            apply()
        }
    }
    
    fun getLoginData(): LoginSuccessData? {
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return null
        }
        
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val societe = prefs.getString(KEY_SOCIETE, null) ?: return null
        val matricule = prefs.getString(KEY_MATRICULE, null) ?: return null
        
        return LoginSuccessData(
            username = username,
            matricule = matricule,
            token = "" // Token no se guarda por seguridad
        )
    }
    
    fun getSelectedCompany(): SelectedCompany? {
        val name = prefs.getString(KEY_SELECTED_COMPANY_NAME, null) ?: return null
        val code = prefs.getString(KEY_SELECTED_COMPANY_CODE, null) ?: return null
        
        return SelectedCompany(
            name = name,
            code = code
        )
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun clearLoginData() {
        prefs.edit().clear().apply()
    }
    
    fun isLoginExpired(maxAgeHours: Long = 24): Boolean {
        val loginTime = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        val maxAgeMillis = maxAgeHours * 60 * 60 * 1000
        
        return (currentTime - loginTime) > maxAgeMillis
    }
}
