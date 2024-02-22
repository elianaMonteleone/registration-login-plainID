package com.example.registrationloginplainID.config;

import com.example.registrationloginplainID.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;


import java.io.IOException;
import java.util.Set;

public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    //if you're not an ADMIN you can't see the table with the users

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        if (roles.contains("ROLE_ADMIN")) {
            response.sendRedirect("/main"); // Redirect to the users page if the user has ROLE_ADMIN authority
        } else {
            response.sendRedirect("/main"); // Redirect to the index page otherwise
        }

    }

}
