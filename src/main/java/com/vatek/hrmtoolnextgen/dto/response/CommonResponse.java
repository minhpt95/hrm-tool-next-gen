package com.vatek.hrmtoolnextgen.dto.response;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatusCode;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommonResponse {
    private String message;
    private HttpStatusCode httpStatusCode;
    private String path;
}
