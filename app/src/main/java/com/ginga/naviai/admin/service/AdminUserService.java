package com.ginga.naviai.admin.service;

import com.ginga.naviai.admin.dto.*;

public interface AdminUserService {
    PagedResponse<UserListResponse> list(String q, String role, String status, Integer page, Integer perPage);
    UserListResponse get(String id);
    UserListResponse update(String id, UserUpdateRequest req);
}
