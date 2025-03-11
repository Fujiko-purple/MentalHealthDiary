package com.example.mentalhealthdiary.config;

public class ApiEndpoint {
    private final String domain;
    private final String[] ips;
    private final String authFormat;
    private final boolean needsSSL;
    private final String modelName;
    
    public ApiEndpoint(String domain, String[] ips, String authFormat, 
                      boolean needsSSL, String modelName) {
        this.domain = domain;
        this.ips = ips;
        this.authFormat = authFormat;
        this.needsSSL = needsSSL;
        this.modelName = modelName;
    }
    
    public String getDomain() { return domain; }
    public String[] getIps() { return ips; }
    public String getAuthFormat() { return authFormat; }
    public boolean needsSSL() { return needsSSL; }
    public String getModelName() { return modelName; }
} 