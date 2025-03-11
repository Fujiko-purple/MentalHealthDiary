package com.example.mentalhealthdiary.config;

import okhttp3.Dns;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class ApiEndpointConfig {
    private static final Map<String, ApiEndpoint> ENDPOINTS = new HashMap<String, ApiEndpoint>() {{
        put("qinyan", new ApiEndpoint(
            "love.qinyan.xyz",
            null,  // 不需要自定义DNS
            "Bearer %s",
            true,
            "claude-3-5-sonnet-20241022"
        ));
    }};

    public static ApiEndpoint getEndpoint(String baseUrl) {
        for (ApiEndpoint endpoint : ENDPOINTS.values()) {
            if (baseUrl.contains(endpoint.getDomain())) {
                return endpoint;
            }
        }
        return null;
    }

    public static boolean needsCustomDns(String baseUrl) {
        ApiEndpoint endpoint = getEndpoint(baseUrl);
        return endpoint != null && endpoint.getIps() != null;
    }

    public static boolean needsSSLTrustAll(String baseUrl) {
        // 对所有API都信任SSL证书
        return true;
    }

    public static String formatAuthHeader(String baseUrl, String apiKey) {
        ApiEndpoint endpoint = getEndpoint(baseUrl);
        if (endpoint != null) {
            return String.format(endpoint.getAuthFormat(), apiKey);
        }
        return "Bearer " + apiKey; // 默认格式
    }

    public static Dns getCustomDns() {
        return hostname -> {
            for (ApiEndpoint endpoint : ENDPOINTS.values()) {
                if (hostname.equals(endpoint.getDomain())) {
                    List<InetAddress> addresses = new ArrayList<>();
                    for (String ip : endpoint.getIps()) {
                        try {
                            addresses.add(InetAddress.getByName(ip));
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!addresses.isEmpty()) {
                        return addresses;
                    }
                }
            }
            
            // 如果没有找到预设IP或解析失败，使用系统DNS
            try {
                return Dns.SYSTEM.lookup(hostname);
            } catch (UnknownHostException e) {
                throw new RuntimeException("DNS解析失败: " + hostname, e);
            }
        };
    }

    public static ApiEndpoint getDefaultEndpoint() {
        return ENDPOINTS.values().iterator().next();
    }
} 