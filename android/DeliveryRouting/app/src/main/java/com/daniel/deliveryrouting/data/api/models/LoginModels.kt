package com.daniel.deliveryrouting.data.api.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("societe") val societe: String
)

data class AuthData(
    @SerializedName("matricule") val matricule: String,
    @SerializedName("token") val token: String
)

data class ErrorData(
    @SerializedName("message") val message: String
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("authentication") val authentication: AuthData?,
    @SerializedName("error") val error: ErrorData?
)

data class LoginSuccessData(
    val username: String,
    val matricule: String,
    val token: String
)