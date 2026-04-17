package com.example.petbook.data.api

import com.example.petbook.data.api.model.AuthorResponse
import com.example.petbook.data.api.model.BookResponse
import com.example.petbook.data.api.model.GenreResponse
import com.example.petbook.data.api.model.LoginRequest
import com.example.petbook.data.api.model.LoginResponse
import com.example.petbook.data.api.model.PublisherResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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

}