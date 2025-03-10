package com.example.mentalhealthdiary.api;

import com.example.mentalhealthdiary.api.model.ChatRequest;
import com.example.mentalhealthdiary.api.model.ChatResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ChatApi {
    @Headers({
        "Content-Type: application/json",
        "Authorization: Bearer sk-rcqdbrkvekqbaanjqwnvnnxbgkjkqyqxcuxkwrerfcslkecj"
    })
    @POST("/v1/chat/completions")
    Call<ChatResponse> sendMessage(@Body ChatRequest request);
} 