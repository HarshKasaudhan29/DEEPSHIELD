package com.detector.dto;

public class AuthDto {

    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String u) { this.username = u; }
        public String getEmail() { return email; }
        public void setEmail(String e) { this.email = e; }
        public String getPassword() { return password; }
        public void setPassword(String p) { this.password = p; }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String u) { this.username = u; }
        public String getPassword() { return password; }
        public void setPassword(String p) { this.password = p; }
    }

    public static class AuthResponse {
        private String token;
        private String username;
        private String email;
        private String message;

        public AuthResponse(String token, String username, String email, String message) {
            this.token = token;
            this.username = username;
            this.email = email;
            this.message = message;
        }

        public String getToken()    { return token; }
        public String getUsername() { return username; }
        public String getEmail()    { return email; }
        public String getMessage()  { return message; }
    }
}