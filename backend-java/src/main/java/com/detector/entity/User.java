package com.detector.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String password;          // BCrypt hashed

    private List<String> roles;       // e.g. ["ROLE_USER", "ROLE_ADMIN"]

    private LocalDateTime createdAt;
    private boolean enabled;

    // ── Constructors ──────────────────────────────────────────

    public User() {}

    public User(String username, String email, String password, List<String> roles) {
        this.username   = username;
        this.email      = email;
        this.password   = password;
        this.roles      = roles;
        this.createdAt  = LocalDateTime.now();
        this.enabled    = true;
    }

    // ── Getters & Setters ─────────────────────────────────────

    public String getId()                     { return id; }
    public void   setId(String id)            { this.id = id; }

    public String getUsername()               { return username; }
    public void   setUsername(String u)       { this.username = u; }

    public String getEmail()                  { return email; }
    public void   setEmail(String e)          { this.email = e; }

    public String getPassword()               { return password; }
    public void   setPassword(String p)       { this.password = p; }

    public List<String> getRoles()            { return roles; }
    public void         setRoles(List<String> r) { this.roles = r; }

    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void          setCreatedAt(LocalDateTime c) { this.createdAt = c; }

    public boolean isEnabled()                { return enabled; }
    public void    setEnabled(boolean e)      { this.enabled = e; }
}
