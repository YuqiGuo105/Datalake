package com.example.datalake.ingestionsvc.dto;

import java.time.Instant;

public record PresignedUrlResponse(String uploadUrl, Instant expiresAt) {}
