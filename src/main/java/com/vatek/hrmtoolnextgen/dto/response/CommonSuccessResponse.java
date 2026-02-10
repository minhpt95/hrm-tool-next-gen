package com.vatek.hrmtoolnextgen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(builderMethodName = "commonSuccessResponseBuilder")
public class CommonSuccessResponse<T> extends CommonResponse {
    private T data;
}
