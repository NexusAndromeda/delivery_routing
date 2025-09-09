package com.daniel.deliveryrouting.data.api

import com.daniel.deliveryrouting.data.api.models.LoginRequest
import com.daniel.deliveryrouting.data.api.models.LoginResponse
import com.daniel.deliveryrouting.data.api.models.GetPackagesRequest
import com.daniel.deliveryrouting.data.api.models.GetPackagesResponse
import com.daniel.deliveryrouting.data.api.models.MobileTourneeRequest
import com.daniel.deliveryrouting.data.api.models.MobileTourneeResponse
import com.daniel.deliveryrouting.data.api.models.UpdatePackageStatusRequest
import com.daniel.deliveryrouting.data.api.models.UpdatePackageStatusResponse
import com.daniel.deliveryrouting.data.api.models.MobileStatsResponse
import com.daniel.deliveryrouting.data.api.models.CompanyListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface BackendApi {
    
    // ===== API COLIS PRIVÉ ORIGINAL =====
    
    @POST("api/colis-prive/auth")
    @Headers(
        "Accept-Charset: UTF-8",
        "Content-Type: application/json; charset=UTF-8",
        "Connection: Keep-Alive",
        "Accept-Encoding: gzip",
        "User-Agent: okhttp/3.4.1"
    )
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/colis-prive/packages")
    @Headers(
        "Accept-Charset: UTF-8",
        "Content-Type: application/json; charset=UTF-8",
        "Connection: Keep-Alive",
        "Accept-Encoding: gzip",
        "User-Agent: okhttp/3.4.1"
    )
    suspend fun getPackages(
        @Body request: GetPackagesRequest
    ): Response<GetPackagesResponse>
    
    // ===== API MÓVIL NUEVA =====
    
    @POST("api/mobile/tournee")
    @Headers(
        "Accept-Charset: UTF-8",
        "Content-Type: application/json; charset=UTF-8",
        "Connection: Keep-Alive",
        "Accept-Encoding: gzip",
        "User-Agent: okhttp/3.4.1"
    )
    suspend fun getMobileTournee(
        @Body request: MobileTourneeRequest
    ): Response<MobileTourneeResponse>
    
    @POST("api/mobile/package/update-status")
    @Headers(
        "Accept-Charset: UTF-8",
        "Content-Type: application/json; charset=UTF-8",
        "Connection: Keep-Alive",
        "Accept-Encoding: gzip",
        "User-Agent: okhttp/3.4.1"
    )
    suspend fun updatePackageStatus(
        @Body request: UpdatePackageStatusRequest
    ): Response<UpdatePackageStatusResponse>
    
    @GET("api/mobile/stats")
    @Headers(
        "Accept-Charset: UTF-8",
        "Content-Type: application/json; charset=UTF-8",
        "Connection: Keep-Alive",
        "Accept-Encoding: gzip",
        "User-Agent: okhttp/3.4.1"
    )
    suspend fun getMobileStats(): Response<MobileStatsResponse>
    
    // ===== API EMPRESAS =====
    
    @GET("api/colis-prive/companies")
    @Headers(
        "Accept-Charset: UTF-8",
        "Content-Type: application/json; charset=UTF-8",
        "Connection: Keep-Alive",
        "Accept-Encoding: gzip",
        "User-Agent: okhttp/3.4.1"
    )
    suspend fun getCompanies(): Response<CompanyListResponse>
}
