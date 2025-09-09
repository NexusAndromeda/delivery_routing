package com.daniel.deliveryrouting.data.api.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos para las empresas de Colis Privé
 */
data class Company(
    @SerializedName("code")
    val code: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String? = null
)

/**
 * Respuesta de la API para la lista de empresas
 */
data class CompanyListResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("companies")
    val companies: List<Company>,
    
    @SerializedName("message")
    val message: String? = null
)

/**
 * Empresa seleccionada para usar en autenticación
 */
data class SelectedCompany(
    val name: String,
    val code: String
)
