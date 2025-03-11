package com.example.mentalhealthdiary.service;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ChatApi {
    @POST("chat/completions")
    Call<ChatResponse> chat(
        @Header("Authorization") String authorization,
        @Body ChatRequest request
    );
} 