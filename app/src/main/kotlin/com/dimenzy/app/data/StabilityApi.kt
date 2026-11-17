package com.dimenzy.app.data

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming

/** Stability AI Stable Fast 3D: one photo in, one GLB out. Every other field stays at its default. */
interface StabilityApi {
  @Multipart
  @Streaming
  @POST("v2beta/3d/stable-fast-3d")
  suspend fun generate(
    @Header("authorization") authorization: String,
    @Part image: MultipartBody.Part,
  ): Response<ResponseBody>
}
