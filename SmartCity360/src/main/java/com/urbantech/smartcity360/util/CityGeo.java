package com.urbantech.smartcity360.util;

import com.urbantech.smartcity360.model.ZoneEntity;

import java.util.concurrent.ThreadLocalRandom;

public final class CityGeo {

    private static final double BASE_LATITUDE = 6.9271;
    private static final double BASE_LONGITUDE = 79.8612;
    private static final double STREET_SPACING_DEGREES = 0.004;
    private static final double JITTER_DEGREES = 0.0015;
    private static final int STREETS_PER_ROW = 3;

    private CityGeo() { }

    public static double latitude(ZoneEntity street, int streetIndex) {
        return street.getLatitude() != null
                ? street.getLatitude()
                : BASE_LATITUDE + (streetIndex / STREETS_PER_ROW) * STREET_SPACING_DEGREES;
    }

    public static double longitude(ZoneEntity street, int streetIndex) {
        return street.getLongitude() != null
                ? street.getLongitude()
                : BASE_LONGITUDE + (streetIndex % STREETS_PER_ROW) * STREET_SPACING_DEGREES;
    }

    public static double jitter(ThreadLocalRandom random) {
        return random.nextDouble(-JITTER_DEGREES, JITTER_DEGREES);
    }
}