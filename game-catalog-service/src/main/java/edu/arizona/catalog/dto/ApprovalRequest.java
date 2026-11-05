package edu.arizona.catalog.dto;

import edu.arizona.catalog.model.enums.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalRequest {

    @NotNull
    private ApprovalStatus status;
}
