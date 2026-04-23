package com.sidesignal.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 시간 의존 로직 테스트 제어용 설정
@Configuration(proxyBeanMethods = false)
public class TimeConfig {

    // 서버 기준 시간
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
