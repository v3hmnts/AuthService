package authService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.sql.Date;

public record RegistrationRequest(
        @NotBlank(message = "Name shouldn't be empty")
        @Size(min = 3, max = 100, message = "Name length should be between 3 and 100 characters")
        String username,

        @NotBlank(message = "Surname shouldn't be empty")
        @Size(min = 3, max = 100, message = "Surname length should be between 3 and 100 characters")
        String surname,

        @NotBlank(message = "Password is required")
        String password,

        @Past(message = "Birth date should be in past")
        Date birthDate,

        @Email(message = "Email should be valid")
        @Size(min = 6, max = 255, message = "Email length should be between 6 and 255 characters")
        String email
) {
}
