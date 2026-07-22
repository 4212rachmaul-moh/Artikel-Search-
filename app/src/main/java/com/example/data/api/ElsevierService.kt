package com.example.data.api

import com.example.data.model.ScopusSearchResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ElsevierService {
    @GET("content/search/scopus")
    suspend fun searchScopus(
        @Query("query") query: String,
        @Query("count") count: Int = 25,
        @Query("start") start: Int = 0,
        @Query("httpAccept") accept: String = "application/json"
    ): ScopusSearchResponse

    @Streaming
    @GET("content/article/doi/{doi}")
    suspend fun downloadPdfByDoi(
        @Path(value = "doi", encoded = true) doi: String,
        @Query("httpAccept") accept: String = "application/pdf"
    ): Response<ResponseBody>
}
