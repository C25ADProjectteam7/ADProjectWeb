package com.expensehub.webbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class WebBackendApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 容器能正常启动、所有 Bean 能正确装配
    }
}
