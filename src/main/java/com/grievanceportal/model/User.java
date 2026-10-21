package com.grievanceportal.model;

import java.io.Serializable;

/** A logged-in user. Never holds the password hash. */
public final class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final String username;
    private final String fullName;
    private final Role role;

    public User(long id, String username, String fullName, Role role) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }
}
