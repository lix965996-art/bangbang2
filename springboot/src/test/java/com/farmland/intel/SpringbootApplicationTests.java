package com.farmland.intel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestConfig.class)
class SpringbootApplicationTests {

    @Test
    void contextLoads() {
    }

}
