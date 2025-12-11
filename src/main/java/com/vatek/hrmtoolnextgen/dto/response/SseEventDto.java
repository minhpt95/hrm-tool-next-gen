package com.vatek.hrmtoolnextgen.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SseEventDto {
    private String id;
    private String event;
    private Object data;
    private String comment;
    private LocalDateTime timestamp;

    public static SseEventDto create(String event, Object data) {
        return SseEventDto.builder()
                .event(event)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SseEventDto create(String id, String event, Object data) {
        return SseEventDto.builder()
                .id(id)
                .event(event)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

