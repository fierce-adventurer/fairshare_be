package com.fairshare;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextTest {

    @Test
    void productionBeansCanStartTogether() {
        // Context startup is the assertion. This intentionally keeps the real auth and Redis beans.
    }
}
