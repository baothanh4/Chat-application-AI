package org.example.chatapplication.DTO.Request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.chatapplication.Model.Enum.UserRole;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleRequest {

    @NotNull
    private UserRole role;
}

