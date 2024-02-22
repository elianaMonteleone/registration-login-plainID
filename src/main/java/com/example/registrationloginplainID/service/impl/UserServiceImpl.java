package com.example.registrationloginplainID.service.impl;



import com.example.registrationloginplainID.dto.UserDto;
import com.example.registrationloginplainID.entity.Role;
import com.example.registrationloginplainID.entity.User;
import com.example.registrationloginplainID.repository.RoleRepository;
import com.example.registrationloginplainID.repository.UserRepository;
import com.example.registrationloginplainID.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.GrantedAuthority;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }



    @Override
    public void saveUser(UserDto userDto) {
        // Convert UserDto to User and then save
        // Convert UserDto to User and then save
        User user = convertToEntity(userDto);

        // Set roles based on the checkboxes
        List<Role> roles = new ArrayList<>();

        if (userDto.isAdmin()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN_SUPER");
            if (adminRole == null) {
                adminRole = createRole("ROLE_ADMIN_SUPER");
            }
            roles.add(adminRole);
        }

        if (userDto.isSuperAdmin()) {
            Role superAdminRole = roleRepository.findByName("ROLE_ADMIN_NORMAL");
            if (superAdminRole == null) {
                superAdminRole = createRole("ROLE_ADMIN_NORMAL");
            }
            roles.add(superAdminRole);
        }

        if (userDto.isUser()) {
            Role userRole = roleRepository.findByName("ROLE_USER");
            if (userRole == null) {
                userRole = createRole("ROLE_USER");
            }
            roles.add(userRole);
        }

        user.setRoles(roles);

        // Save the user entity
        userRepository.save(user);
    }

    private User convertToEntity(UserDto userDto) {
        User user = new User();
        user.setName(userDto.getFirstName() + " " + userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setAddress(userDto.getAddress());
        user.setCity(userDto.getCity());
        user.setPhonenumber(userDto.getPhonenumber());
        user.setBelgianIdNumber(userDto.getBelgianIdNumber());
        user.setCountry(userDto.getCountry());

        // Set roles based on the checkboxes
        List<Role> roles = new ArrayList<>();

        if (userDto.isSuperAdmin()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN_SUPER");
            if (adminRole == null) {
                adminRole = new Role("ROLE_ADMIN_SUPER");
                roleRepository.save(adminRole);
            }
            roles.add(adminRole);
        }

        if (userDto.isAdmin()) {
            Role superAdminRole = roleRepository.findByName("ROLE_ADMIN_NORMAL");
            if (superAdminRole == null) {
                superAdminRole = new Role("ROLE_ADMIN_NORMAL");
                roleRepository.save(superAdminRole);
            }
            roles.add(superAdminRole);
        }

        if (userDto.isUser()) {
            Role userRole = roleRepository.findByName("ROLE_USER");
            if (userRole == null) {
                userRole = new Role("ROLE_USER");
                roleRepository.save(userRole);
            }
            roles.add(userRole);
        }

        user.setRoles(roles);

        return user;
    }








    private Role findOrCreateRole(String roleName) {
        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            role = createRole(roleName);
        }
        return role;
    }

    private Role createRole(String roleName) {
        roleName = roleName.trim();
        Role role = new Role();
        role.setName(roleName);
        return roleRepository.save(role);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<UserDto> findAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map((user) -> mapToUserDto(user))
                .collect(Collectors.toList());
    }

    private UserDto mapToUserDto(User user){
        UserDto userDto = new UserDto();
        String[] str = user.getName().split(" ");
        userDto.setFirstName(str[0]);
        userDto.setLastName(str[1]);
        userDto.setEmail(user.getEmail());
        userDto.setAddress(user.getAddress());
        userDto.setCity(user.getCity());
        userDto.setPhonenumber(user.getPhonenumber());
        userDto.setBelgianIdNumber(user.getBelgianIdNumber());
        userDto.setCountry(user.getCountry());
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName) // Assuming Role entity has a getName() method.
                .collect(Collectors.toList());
        userDto.setRoleNames(roleNames);
        return userDto;
    }

    private Role checkRoleExist(){
        Role role = new Role();
        role.setName("SUPER_ADMIN");
        return roleRepository.save(role);
    }


    public String getUserCountry(String userEmail) {
        // Assuming UserRepository is a Spring Data repository for accessing user data
        User user = userRepository.findByEmail(userEmail);
        if (user != null) {
            return user.getCountry(); // Assuming the country information is stored in the User entity
        } else {
            return null; // User not found or country information not available
        }
    }



}