package com.minhpt.hrmtoolnextgen.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhpt.hrmtoolnextgen.dto.user.UserInfoDto;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;
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
        private record StringToUserInfoDtoConverter(ObjectMapper objectMapper) implements Converter<String, UserInfoDto> {

        @Override
            public UserInfoDto convert(String source) {
                try {
                    if (source.trim().isEmpty()) {
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
        private record StringToEUserRoleCollectionConverter(
            ObjectMapper objectMapper) implements Converter<String, Collection<EUserRole>> {

        @Override
            public Collection<EUserRole> convert(String source) {
                if (source.trim().isEmpty()) {
                    return new ArrayList<>();
                }

                String trimmed = source.trim();
                // Accept JSON arrays (e.g., ["HR","ADMIN"]) or plain comma/space-separated (e.g., HR,ADMIN)
                // First try JSON
                try {
                    List<EUserRole> roles = objectMapper.readValue(
                            trimmed,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, EUserRole.class)
                    );
                    if (roles != null) {
                        return roles;
                    }
                } catch (Exception ignored) {
                    // fall through to simple parsing
                }

                // Fallback: split by comma/space and map to enum
                try {
                    String[] parts = trimmed.split("[,\\s]+");
                    List<EUserRole> roles = new ArrayList<>();
                    for (String part : parts) {
                        if (part.isBlank()) {
                            continue;
                        }
                        roles.add(EUserRole.valueOf(part.trim()));
                    }
                    return roles;
                } catch (Exception e) {
                    log.warn("Failed to parse Collection<EUserRole> from string: {}", source, e);
                    return new ArrayList<>();
                }
            }
        }
}

