package com.farmland.intel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"websocket.enabled=false", "onenet.enabled=false"})
class SpringbootApplicationTests {

    @Test
    void contextLoads() {
    }
}
