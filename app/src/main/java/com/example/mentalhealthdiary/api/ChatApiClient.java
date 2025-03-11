package com.example.mentalhealthdiary.api;

import android.content.Context;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Dns;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.example.mentalhealthdiary.config.RemoteConfig;

public class ChatApiClient {
    private static final List<String> API_IPS = Arrays.asList(
        "106.14.246.150",
        "139.196.152.242",
        "47.102.215.139",
        "47.102.37.23",
        "101.132.62.140",
        "47.103.87.49",
        "47.101.39.152"
    );
    private static int currentIpIndex = 0;
    private static ChatApi instance;

    public static ChatApi getInstance(Context context) {
        if (instance == null) {
            // 自定义DNS解析
            Dns customDns = hostname -> {
                if ("api.siliconflow.cn".equals(hostname)) {
                    List<InetAddress> addresses = new ArrayList<>();
                    for (String ip : API_IPS) {
                        try {
                            addresses.add(InetAddress.getByName(ip));
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }
                    return addresses;
                }
                return Dns.SYSTEM.lookup(hostname);
            };

            // 信任所有证书配置
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
            };

            OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(createSSLContext(trustAllCerts).getSocketFactory(), 
                                (X509TrustManager)trustAllCerts[0])
                .hostnameVerifier((hostname, session) -> true)
                .addInterceptor(IP_INTERCEPTOR)
                .dns(customDns)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

            // 使用RemoteConfig获取配置
            String baseUrl = RemoteConfig.getApiBaseUrl();
            String apiKey = RemoteConfig.getApiKey();
            
            // 修改Retrofit配置
            Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

            instance = retrofit.create(ChatApi.class);
        }
        return instance;
    }

    // 添加重试拦截器
    static class RetryInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            int tryCount = 0;
            
            while (tryCount < 3 && response == null) {
                try {
                    response = chain.proceed(request);
                } catch (Exception e) {
                    if (tryCount >= 2) throw e;
                    tryCount++;
                }
            }
            return response;
        }
    }

    private static SSLContext createSSLContext(TrustManager[] trustManagers) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("SSL Context初始化失败", e);
        }
    }

    private static final Interceptor IP_INTERCEPTOR = chain -> {
        Request original = chain.request();
        String newUrl = original.url().toString()
            .replace("api.siliconflow.cn", API_IPS.get(currentIpIndex));
        currentIpIndex = (currentIpIndex + 1) % API_IPS.size();
        
        return chain.proceed(original.newBuilder()
            .url(newUrl)
            .build());
    };
} 