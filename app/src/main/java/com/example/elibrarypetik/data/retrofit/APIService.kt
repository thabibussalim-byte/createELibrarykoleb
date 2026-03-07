package com.example.eatik.data.retrofit

import com.example.eatik.data.response.MenuResponseItem
import com.example.eatik.ui.MenuResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.*
import retrofit2.http.*


interface APIService {

    @GET("menu")
    suspend fun getAllMenu(): Response<List<MenuResponseItem>>


    @Multipart
    @POST("menu") // Sesuaikan endpoint Anda
    suspend fun uploadMenu(
        @Part("nama") nama: RequestBody,
        @Part("harga") harga: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody,
        @Part("kategori") kategori: RequestBody,
        @Part("status") status: RequestBody,
        @Part foto: MultipartBody.Part
    ): MenuResponse


    @DELETE("menu/{id}")
    suspend fun deleteMenu(@Path("id") id: Int): Response<Void>

}