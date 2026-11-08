package com.example.userservice.dao;

import com.example.userservice.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserDao {

    User create(User user);

    Optional<User> findById(Long id);

    List<User> findAll();

    User update(User user);

    boolean deleteById(Long id);
}