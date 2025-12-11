package com.vatek.hrmtoolnextgen;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.ZonedDateTime;
import java.util.TimeZone;

@SpringBootApplication
@Log4j2
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
@EnableWebMvc
@EnableCaching
@EnableAspectJAutoProxy(exposeProxy = true)
@RequiredArgsConstructor
@EnableJpaRepositories(basePackages = "com.vatek.hrmtoolnextgen.repository.jpa")
@EnableRedisRepositories(basePackages = "com.vatek.hrmtoolnextgen.repository.redis")
@EntityScan(basePackages = "com.vatek.hrmtoolnextgen.entity")
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class HrmToolNextGenApplication {

    final Environment env;

    public static void main(String[] args) {
        SpringApplication.run(HrmToolNextGenApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void setApplicationTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        log.info("TimeZone : {} , ZoneDateTime : {} ", TimeZone::getDefault, ZonedDateTime::now);
    }


    @EventListener(ApplicationReadyEvent.class)
    public void clearAllToken(){
        log.info("Application Name : {}", () -> env.getProperty("application.name"));
        log.info("Build Version : {}", () -> env.getProperty("build.version"));
        log.info("Build Timestamp : {}", () -> env.getProperty("build.timestamp"));
    }
}
