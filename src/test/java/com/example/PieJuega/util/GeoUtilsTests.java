package com.example.PieJuega.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeoUtilsTests {

    @Test
    void calculatesDistanceInKilometers() {
        Double distance = GeoUtils.distanceKm(
                10.9878,
                -74.7889,
                11.0008,
                -74.8068
        );

        assertThat(distance).isEqualTo(2.4);
    }

    @Test
    void returnsNullWithoutCompleteCoordinates() {
        assertThat(GeoUtils.distanceKm(null, -74.78, 11.0, -74.8)).isNull();
    }
}
