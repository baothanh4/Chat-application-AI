package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.chatapplication.Model.Enum.ModerationReportStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateModerationReportStatusRequest {

    @NotNull
    private ModerationReportStatus status;

    @Size(max = 2000)
    private String moderatorNote;
}

