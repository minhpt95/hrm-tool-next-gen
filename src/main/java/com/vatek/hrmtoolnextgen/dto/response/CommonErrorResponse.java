package com.vatek.hrmtoolnextgen.dto.response;

import com.vatek.hrmtoolnextgen.exception.InternalServerException;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatusCode;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class CommonErrorResponse extends CommonResponse {

    @Builder(builderMethodName = "commonErrorResponseBuilder")
    public CommonErrorResponse(String message, HttpStatusCode httpStatusCode, String path) {
        super(message, httpStatusCode, path);
    }
}
