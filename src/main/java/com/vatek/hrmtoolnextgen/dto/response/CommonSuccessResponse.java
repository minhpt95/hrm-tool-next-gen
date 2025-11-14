package com.vatek.hrmtoolnextgen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;


@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonSuccessResponse<T> extends CommonResponse{
    T data;

    @Builder(builderMethodName = "commonSuccessResponseBuilder")
    public CommonSuccessResponse(String path, T data) {
        super("Successfully", HttpStatus.OK, path);
        this.data = data;
    }
}
