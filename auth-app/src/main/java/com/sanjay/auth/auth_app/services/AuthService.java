package com.sanjay.auth.auth_app.services;

import com.sanjay.auth.auth_app.dtos.UserDto;

public interface AuthService {
    UserDto registerUser(UserDto userDto);

    //User login next
}
