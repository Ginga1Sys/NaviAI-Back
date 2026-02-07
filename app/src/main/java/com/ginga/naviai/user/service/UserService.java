package com.ginga.naviai.user.service;

import com.ginga.naviai.auth.dto.UserResponse;

public interface UserService {
    UserResponse getCurrentUser(Long userId);
}
