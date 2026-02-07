package com.ginga.naviai.user.service;

import com.ginga.naviai.auth.dto.UserResponse;
import com.ginga.naviai.auth.entity.User;
import com.ginga.naviai.auth.repository.UserRepository;
import com.ginga.naviai.user.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        UserResponse res = new UserResponse();
        res.setId(user.getId());
        res.setUsername(user.getUsername());
        res.setEmail(user.getEmail());
        res.setDisplayName(user.getDisplayName());
        res.setCreatedAt(user.getCreatedAt());
        return res;
    }
}
