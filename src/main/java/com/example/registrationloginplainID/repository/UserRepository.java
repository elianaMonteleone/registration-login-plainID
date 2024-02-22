package com.example.registrationloginplainID.repository;


import com.example.registrationloginplainID.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);

    User findUserById(Long id);

}