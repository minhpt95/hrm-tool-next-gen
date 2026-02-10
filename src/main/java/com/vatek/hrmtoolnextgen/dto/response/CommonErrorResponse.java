package com.vatek.hrmtoolnextgen.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder(builderMethodName = "commonErrorResponseBuilder")
public class CommonErrorResponse extends CommonResponse {
}
