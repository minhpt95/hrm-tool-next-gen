package com.minhpt.hrmtoolnextgen.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Desired final set of users assigned to a device. Server diffs against the current state — users in this list and not currently assigned will be added, users currently assigned but not in this list will be removed. Sending an empty list detaches all users.")
public class ManageDeviceUsersDto {

    @NotNull(message = "userIds must not be null")
    @Schema(
            description = "IDs of users that should remain assigned to the device after this call",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "[1, 4]"
    )
    private List<@NotNull Long> userIds;
}
