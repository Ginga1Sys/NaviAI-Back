package com.ginga.naviai.auth.service;

import com.ginga.naviai.auth.dto.RegisterRequest;
import com.ginga.naviai.auth.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
}
