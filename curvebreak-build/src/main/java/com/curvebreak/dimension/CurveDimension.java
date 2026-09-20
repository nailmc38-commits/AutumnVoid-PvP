package com.curvebreak.dimension;

public record CurveDimension(
        String id,
        String displayName,
        int requiredTier,
        int stability,
        String description
) {}
