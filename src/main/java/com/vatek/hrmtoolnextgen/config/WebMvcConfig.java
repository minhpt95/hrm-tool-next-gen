package com.vatek.hrmtoolnextgen.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vatek.hrmtoolnextgen.dto.user.UserInfoDto;
import com.vatek.hrmtoolnextgen.enumeration.EUserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Log4j2
public class WebMvcConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToUserInfoDtoConverter(objectMapper));
        registry.addConverter(new StringToEUserRoleCollectionConverter(objectMapper));
    }

    /**
     * Converter to parse JSON string to UserInfoDto from form data
     */
    private class StringToUserInfoDtoConverter implements Converter<String, UserInfoDto> {
        private final ObjectMapper objectMapper;

        public StringToUserInfoDtoConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public UserInfoDto convert(String source) {
            try {
                if (source == null || source.trim().isEmpty()) {
                    return null;
                }
                // Try to parse as JSON first
                return objectMapper.readValue(source, UserInfoDto.class);
            } catch (Exception e) {
                log.warn("Failed to parse UserInfoDto from JSON string: {}", source, e);
                return null;
            }
        }
    }

    /**
     * Converter to parse JSON string to Collection<EUserRole> from form data
     */
    private class StringToEUserRoleCollectionConverter implements Converter<String, Collection<EUserRole>> {
        private final ObjectMapper objectMapper;

        public StringToEUserRoleCollectionConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Collection<EUserRole> convert(String source) {
            try {
                if (source == null || source.trim().isEmpty()) {
                    return new ArrayList<>();
                }
                // Try to parse as JSON array
                List<EUserRole> roles = objectMapper.readValue(
                    source,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, EUserRole.class)
                );
                return roles != null ? roles : new ArrayList<>();
            } catch (Exception e) {
                log.warn("Failed to parse Collection<EUserRole> from JSON string: {}", source, e);
                return new ArrayList<>();
            }
        }
    }
}

