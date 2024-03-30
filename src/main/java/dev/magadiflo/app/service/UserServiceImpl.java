package dev.magadiflo.app.service;

import dev.magadiflo.app.document.Status;
import dev.magadiflo.app.document.User;
import dev.magadiflo.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public void saveUser(User user) {
        user.setStatus(Status.ONLINE);
        this.userRepository.save(user);
    }

    @Override
    public void disconnect(User user) {
        Optional<User> userDBOptional = this.userRepository.findById(user.getNickName());
        if (userDBOptional.isPresent()) {
            User userDB = userDBOptional.get();
            userDB.setStatus(Status.OFFLINE);
            this.userRepository.save(userDB);
        }
    }

    @Override
    public List<User> findConnectedUsers() {
        return this.userRepository.findAllByStatus(Status.ONLINE);
    }
}
