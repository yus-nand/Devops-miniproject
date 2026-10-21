package com.grievanceportal.service;

import com.grievanceportal.dao.UserDao;
import com.grievanceportal.model.User;
import java.sql.SQLException;
import java.util.Optional;

public class AuthService {

    private final UserDao userDao = new UserDao();

    /** Returns the user for valid credentials, or empty for anything else. */
    public Optional<User> login(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return Optional.empty();
        }
        return userDao.authenticate(username.trim(), password);
    }
}
