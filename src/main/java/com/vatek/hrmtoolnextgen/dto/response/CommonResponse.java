package com.vatek.hrmtoolnextgen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatusCode;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(builderMethodName = "commonResponseBuilder")
public class CommonResponse {
    private String message;
    private HttpStatusCode httpStatusCode;
    private String path;
}
