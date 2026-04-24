package com.example.petbook.data.api

import com.example.petbook.data.api.model.*

import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @GET("api/genre")
    fun getGenres(): Call<GenreResponse>

    @GET("api/buku")
    fun getBooks(): Call<BookResponse>

    @GET("api/penulis")
    fun getAuthors(): Call<AuthorResponse>

    @GET("api/penerbit")
    fun getPublishers(): Call<PublisherResponse>

    @POST("api/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/logout")
    fun logout(@Header("Authorization") token: String): Call<LogoutResponse>

    @POST("api/transaksi/create")
    fun createBorrow(
        @Header("Authorization") token: String,
        @Body request: BorrowRequest
    ): Call<BorrowResponse>

    @POST("api/denda/tambah")
    fun createFine(
        @Header("Authorization") token: String,
        @Body request: FineRequest
    ): Call<FineResponse>

    @PATCH("api/denda/edit/{id}")
    fun updateFine(
        @Header("Authorization") token: String,
        request1: Int,
        @Body request: FineRequest
    ): Call<FineResponse>

    @GET("api/transaksi")
    fun getAllTransactions(@Header("Authorization") token: String): Call<HistoryResponse>

    @GET("api/transaksi/user/{id}")
    fun getHistoryByUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int): Call<HistoryResponse>

    @GET("api/denda")
    fun getFines(@Header("Authorization") token: String): Call<FineResponse>

    @GET("api/mahasantri")
    fun getMahasantri(@Header("Authorization") token: String): Call<MahasantriResponse>

    @PATCH("api/user/update/{id}")
    fun updateUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int,
        @Body request: UpdateUserRequest
    ): Call<BorrowResponse>

    @PATCH("api/buku/update/{id}")
    fun updateBook(
        @Header("Authorization") token: String,
        @Path("id") bookId: Int,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Call<UpdateBookResponse>

    @GET("api/user")
    fun getUsers(@Header("Authorization") token: String): Call<UserResponse>
}
