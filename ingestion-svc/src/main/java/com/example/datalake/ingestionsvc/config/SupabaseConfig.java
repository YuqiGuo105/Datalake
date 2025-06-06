package com.example.datalake.ingestionsvc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SupabaseConfig {
    // Base URL of your Supabase project (without “/storage/v1”)
    @Value("${supabase.url}")
    private String supabaseUrl;

    // The anon or service‐role key from Supabase Dashboard → Settings → API
    @Value("${supabase.key}")
    private String supabaseKey;

    @Bean
    public WebClient supabaseWebClient() {
        // We point at the Storage REST path under /storage/v1
        String storageEndpoint = supabaseUrl + "/storage/v1";

        return WebClient.builder()
                .baseUrl(storageEndpoint)
                // These headers go on EVERY request to Supabase Storage
                .defaultHeader("apikey", supabaseKey)
                .defaultHeader("Authorization", "Bearer " + supabaseKey)
                .build();
    }
}
