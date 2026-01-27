package com.ginga.naviai.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.ginga.naviai.auth.validation.StrongPassword;

public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 30)
    private String username;

    @NotBlank
    @Email
    @Pattern(regexp = ".+@ginga\\.info$", message = "メールアドレスは @ginga.info ドメインである必要があります")
    private String email;

    @NotBlank
    @Size(min = 8)
    @StrongPassword
    private String password;

    @Size(max = 100)
    private String displayName;

    public RegisterRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
