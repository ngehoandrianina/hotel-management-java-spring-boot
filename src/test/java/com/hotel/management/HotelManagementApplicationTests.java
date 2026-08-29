package com.hotel.management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Chargement du contexte Spring")
class HotelManagementApplicationTests {

    @Test
    @DisplayName("Le contexte Spring doit se charger correctement")
    void contextLoads() {
    }
}
