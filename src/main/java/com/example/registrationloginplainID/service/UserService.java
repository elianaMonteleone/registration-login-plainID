package com.example.registrationloginplainID.service;


import com.example.registrationloginplainID.dto.UserDto;
import com.example.registrationloginplainID.entity.Role;
import com.example.registrationloginplainID.entity.User;

import java.util.List;

public interface UserService {
    void saveUser(UserDto userDto);

    User findUserByEmail(String email);

    List<UserDto> findAllUsers();


    String getUserCountry(String userEmail);
}
