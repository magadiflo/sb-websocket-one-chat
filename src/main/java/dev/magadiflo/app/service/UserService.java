package dev.magadiflo.app.service;

import dev.magadiflo.app.document.User;

import java.util.List;

public interface UserService {
    void saveUser(User user);

    void disconnect(User user);

    List<User> findConnectedUsers();
}
