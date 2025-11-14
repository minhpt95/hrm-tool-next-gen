package com.vatek.hrmtoolnextgen.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectMapperConfig {

    @Bean(name = "objectMapper")
    public ObjectMapper objectMapper()
    {
        var om = new ObjectMapper();
        om.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES,true);
        om.configure(JsonGenerator.Feature.IGNORE_UNKNOWN,true);
        om.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,false);
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return om;
    }
}
