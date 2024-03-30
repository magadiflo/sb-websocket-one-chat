package dev.magadiflo.app.repository;

import dev.magadiflo.app.document.Status;
import dev.magadiflo.app.document.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {
    List<User> findAllByStatus(Status status);
}
