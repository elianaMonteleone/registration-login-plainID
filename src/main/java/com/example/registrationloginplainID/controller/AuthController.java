package com.example.registrationloginplainID.controller;

import com.example.registrationloginplainID.dto.UserDto;
import com.example.registrationloginplainID.entity.Role;
import com.example.registrationloginplainID.entity.User;
import com.example.registrationloginplainID.repository.UserRepository;
import com.example.registrationloginplainID.service.SqlAuthorizerService;
import com.example.registrationloginplainID.service.UserService;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import kong.unirest.Unirest;
import kong.unirest.HttpResponse;
//import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AuthController {


    private UserService userService;

    @Autowired
    private UserRepository userRepository;



    @Autowired
    private SqlAuthorizerService sqlAuthorizerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);



    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // handler method to handle home page request
    @GetMapping("/index")
    public String home(){
        return "index";
    }


    // handler method to handle user registration form request
    @GetMapping("/register")
    public String showRegistrationForm(Model model){
        // create model object to store form data
        UserDto user = new UserDto();



        model.addAttribute("user", user);
        return "register";
    }





    @PostMapping("/register/save")
    public String registration(@ModelAttribute("user") UserDto userDto,
                               BindingResult result,
                               Model model) {
        User existingUser = userService.findUserByEmail(userDto.getEmail());

        if (existingUser != null && existingUser.getEmail() != null && !existingUser.getEmail().isEmpty()) {
            result.rejectValue("email", null,
                    "There is already an account registered with the same email");
        }

        if (result.hasErrors()) {
            model.addAttribute("user", userDto);
            return "/register";
        }

        userService.saveUser(userDto);
        return "redirect:/register?success";
    }


    // handler method to handle list of users
    @GetMapping("/users")
    public String users(Model model){
        List<UserDto> users = userService.findAllUsers();
        model.addAttribute("users", users);
        return "users";
    }


    /**
     * API that redirect to the login page
     * @return
     */
    @GetMapping("/login")
    public String login(){
        return "login";
    }


    /**
     *
     * @param model
     * @param authentication
     * @return main page, where you see the USER button and the info regarding the user logged-in
     */
    @GetMapping("/main")
    public String main(Model model, Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userService.findUserByEmail(userEmail); // Using your existing method

        if (user != null) {
            // Assuming you have a method to map User to UserDto
            UserDto userDto = mapToUserDto(user);

            model.addAttribute("userName", userDto.getFirstName()); // Assuming UserDto has getName method
            for (String role:
                    userDto.getRoleNames()) {
                if(role.equals("ROLE_ADMIN_SUPER")){
                model.addAttribute("userRoles", "Admin"); // Assuming UserDto has getRoleNames method
            } else if(role.equals("ROLE_ADMIN_NORMAL")){
                    model.addAttribute("userRoles", "Superuser");
                } else {
                    model.addAttribute("userRoles", "User");
                }
            }
        }

        return "main";
    }


    /**
     *
     * @param model
     * @param redirectAttributes
     * @param authentication
     * @return the page with the table of users, where you can see different columns depending on the ROLE that the user has.
     * It is controlled by the PERMIT/DENY API from PlainID
     */
    @PostMapping("/viewUserData")
    public String permitOrDenyAPI(
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        if (isAdminSuper(authentication) || isAdminNormal(authentication)) {
            // Retrieve the email directly from the authentication
            String userEmail = authentication.getName();

            // Call the external API for access decision
            HttpResponse<String> response = Unirest.post("http://10.253.228.65:8010/api/runtime/permit-deny/v4")
                    .header("Content-Type", "application/json")
                    .body("{\r\n" +
                            "    \"clientId\": \"PN2F3FWBGVYWJSGWR2MS\",\r\n" +
                            "    \"clientSecret\": \"bs4UE51RsaJSlqJLxNslxK0P8Bvl2p5OgxTwUhqr\",\r\n" +
                            "    \"entityId\": \"" + userEmail + "\",\r\n" +
                            "    \"responseType\": \"accessDecision\",\r\n" +
                            "    \"listOfResources\": [\r\n" +
                            "        {\r\n" +
                            "            \"resourceType\": \"viewUserData\",\r\n" +
                            "            \"resources\": [\r\n" +
                            "                {\r\n" +
                            "                    \"action\": \"POST\",\r\n" +
                            "                    \"path\": \"/users/1\"\r\n" +
                            "                }\r\n" +
                            "            ]\r\n" +
                            "        }\r\n" +
                            "    ],\r\n" +
                            "    \"includeDetails\": true,\r\n" +
                            "    \"includeIdentity\": true,\r\n" +
                            "    \"includeAccessPolicy\": true,\r\n" +
                            "    \"includeAssetAttributes\": true,\r\n" +
                            "    \"accessTokenFormat\": true,\r\n" +
                            "    \"includeDenyReason\": true,\r\n" +
                            "    \"combinedMultiValue\": true\r\n" +
                            "}")
                    .asString();

            // Handle the response as needed
            int statusCode = response.getStatus();
            String responseBody = response.getBody();
            System.out.println("Response body:" + responseBody);

            // Assuming responseBody is a String variable in your controller
            boolean isAdminSuper = responseBody.contains("ROLE_ADMIN_SUPER");
            System.out.println("isAdminSuper value in controller: " + isAdminSuper);

            // Add the isAdminSuper attribute to the model
            model.addAttribute("isAdminSuper", isAdminSuper);

            boolean isAdminNormal = isAdminNormal(authentication);
            model.addAttribute("isAdminNormal", isAdminNormal);



            List<String> usersInCity = getUsersInCity(userEmail);

            String userCountry = userService.getUserCountry(userEmail);

            if (statusCode == 200 && isAdminSuper) {
                // For successful access decision and ROLE_ADMIN_SUPER, proceed to the users page
                model.addAttribute("users", userService.findAllUsers());

                // Add the user's country to the model
                model.addAttribute("userCountry", userCountry);// Replace with your actual logic to retrieve users
                model.addAttribute("usersInCity", usersInCity);
                return "users";
            } else if (statusCode == 200 && isAdminNormal) {
                // For successful access decision and ROLE_ADMIN_NORMAL, proceed to the users page with limited columns
                model.addAttribute("users", userService.findAllUsers());

                model.addAttribute("userCountry", userCountry);
                model.addAttribute("usersInCity", usersInCity);
                return "users";
            }
            else {
                // Handle the case where access is denied or other errors
                redirectAttributes.addFlashAttribute("noRules", "Sorry, you don't have any rules.");
                return "redirect:/index";
            }
        } else {
            // For users without ROLE_ADMIN_SUPER, handle accordingly
            redirectAttributes.addFlashAttribute("notAllowed", "Sorry, you are not allowed to view the list of users.");
            return "redirect:/main";
        }

    }





    /**
     * API to retrieve the info of the policies and send the SQL statement dynamically
     * @param model
     * @param redirectAttributes
     * @param authentication
     * @return the page of the chat with the list of users available depending on the policy (same city)
     */
    @PostMapping("/chatWith")
    public String chatWithPeople(Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        String userEmail = authentication.getName();

        //String allowedCountry = getCountryByEmail(userEmail);

        // Construct the request body for the resolution API
        String resolutionRequestBody = String.format("{\r\n" +
                "    \"clientId\": \"PN2F3FWBGVYWJSGWR2MS\",\r\n" +
                "    \"clientSecret\": \"bs4UE51RsaJSlqJLxNslxK0P8Bvl2p5OgxTwUhqr\",\r\n" +
                "    \"entityId\": \"%s\",\r\n" +
                "    \"includeDetails\": true,\r\n" +
                "    \"includeIdentity\": true,\r\n" +
                "    \"includeAccessPolicy\": true,\r\n" +
                "    \"includeAssetAttributes\": true,\r\n" +
                "    \"accessTokenFormat\": true,\r\n" +
                "    \"includeDenyReason\": true,\r\n" +
                "    \"combinedMultiValue\": true\r\n" +
                "}", userEmail);

        HttpResponse<String> resolutionResponse = Unirest.post("http://10.253.228.65:8010/api/runtime/resolution/v3")
                .header("Content-Type", "application/json")
                .body(resolutionRequestBody)
                .asString();

           String allowedCity = "";

            // Construct the initial SQL query based on allowed city and columns
            String initialSqlQuery = "SELECT email, country FROM users";

            String response = resolutionResponse.getBody();
            System.out.println("Response from permission:" + response);

            // Send the initial query to the resql API
            String modifiedQuery = sqlAuthorizerService.sendQueryToAuthorizer(initialSqlQuery,userEmail);

            // Execute the modified query
            List<UserDto> users = executeModifiedQuery(modifiedQuery);
        for (UserDto user:
             users) {
            allowedCity = user.getCountry();

        }

        // Check if usersInCity is not null and set a flag accordingly



            // Prepare the data for the chat page
            //model.addAttribute("myCountry", allowedCountry);
            model.addAttribute("usersInCity", users);
            model.addAttribute("chatCity", allowedCity);

            return "chatPage";
    }

    private List<String> getUsersInCity(String userEmail) {
        // Construct the request body for the resolution API
        String resolutionRequestBody = String.format("{\r\n" +
                "    \"clientId\": \"PN2F3FWBGVYWJSGWR2MS\",\r\n" +
                "    \"clientSecret\": \"bs4UE51RsaJSlqJLxNslxK0P8Bvl2p5OgxTwUhqr\",\r\n" +
                "    \"entityId\": \"%s\",\r\n" +
                "    \"includeDetails\": true,\r\n" +
                "    \"includeIdentity\": true,\r\n" +
                "    \"includeAccessPolicy\": true,\r\n" +
                "    \"includeAssetAttributes\": true,\r\n" +
                "    \"accessTokenFormat\": true,\r\n" +
                "    \"includeDenyReason\": true,\r\n" +
                "    \"combinedMultiValue\": true\r\n" +
                "}", userEmail);

        HttpResponse<String> resolutionResponse = Unirest.post("http://10.253.228.65:8010/api/runtime/resolution/v3")
                .header("Content-Type", "application/json")
                .body(resolutionRequestBody)
                .asString();

        String initialSqlQuery = "SELECT email, country FROM users";

        String response = resolutionResponse.getBody();
        System.out.println("Response from permission:" + response);

        String modifiedQuery = sqlAuthorizerService.sendQueryToAuthorizer(initialSqlQuery,userEmail);

        List<UserDto> users = executeModifiedQuery(modifiedQuery);
        List<String> userCountry = new ArrayList<>();
        String loggedInUserCountry = "";
        for (UserDto user : users) {
            if (user.getEmail().equals(userEmail)) { // Get the logged-in user's country
                loggedInUserCountry = user.getCountry();
            } else {
                userCountry.add(user.getCountry()); // Add other users' countries
            }
        }

        List<String> filteredUserCountry = new ArrayList<>();
        if (!loggedInUserCountry.isEmpty()) {
            for (String country : userCountry) {
                if (!country.equals(loggedInUserCountry)) { // Exclude the logged-in user's country
                    filteredUserCountry.add(country);
                }
            }
        } else {
            filteredUserCountry.addAll(userCountry); // Include all countries if logged-in user's country is not available
        }

        return filteredUserCountry;
    }






    /**
     * Method to execute the modified query and map the result to UserDto
     * @param modifiedQuery
     * @return the list of users based on the query
     */
    private List<UserDto> executeModifiedQuery(String modifiedQuery) {
        if (modifiedQuery.trim().toUpperCase().startsWith("SELECT")) {
            RowMapper<UserDto> rowMapper = new RowMapper<UserDto>() {
                @Override
                public UserDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                    UserDto userDto = new UserDto();
                    userDto.setEmail(rs.getString("email"));
                    userDto.setCountry(rs.getString("country"));
                    return userDto;
                }
            };
            List<UserDto> users = jdbcTemplate.query(modifiedQuery, rowMapper);
            return users;
        } else {
            int rowsAffected = jdbcTemplate.update(modifiedQuery);
            // Handle the result of rowsAffected as needed
            return Collections.emptyList(); // or an appropriate response
        }
    }

    private UserDto convertToDto(User user) {
        // Method to convert User to UserDto
        UserDto dto = new UserDto();
        dto.setEmail(user.getEmail());
        // Set other properties as needed
        return dto;
    }

    @PostMapping("/startChat")
    public String startChat(@RequestParam String chatWithEmail, Model model, HttpServletRequest request) {
        // Validate that the email belongs to a valid user
        User userToChatWith = userService.findUserByEmail(chatWithEmail);
        if (userToChatWith != null) {
            // Store the user's email in the session for chat context
            request.getSession().setAttribute("chatWithEmail", chatWithEmail);
            model.addAttribute("chatWithUser", userToChatWith);
            return "chatPage"; // Redirect to the chat page
        } else {
            model.addAttribute("error", "User not found");
            return "redirect:/chat"; // Redirect back to the chat page with an error message
        }
    }


    @PostMapping("/sendMessage")
    public String sendMessage(@RequestParam String message, Model model, HttpServletRequest request) {
        // Retrieve the email of the user you are chatting with from the session
        String chatWithEmail = (String) request.getSession().getAttribute("chatWithEmail");

        if (chatWithEmail != null) {
            // Here, you would normally send the message. For now, just simulate it.
            model.addAttribute("messageSent", message);
            model.addAttribute("chatWithEmail", chatWithEmail);
            return "chatPage"; // Redirect back to the chat page
        } else {
            model.addAttribute("error", "No chat session found");
            return "redirect:/chat"; // Redirect back to the chat page with an error message
        }
    }




    private UserDto filterUser(UserDto user, Set<String> allowedColumns) {
        UserDto filteredUser = new UserDto();
        if (allowedColumns.contains("firstName")) {
            filteredUser.setFirstName(user.getFirstName());
        }
        if (allowedColumns.contains("lastName")) {
            filteredUser.setLastName(user.getLastName());
        }
        if (allowedColumns.contains("email")) {
            filteredUser.setEmail(user.getEmail());
        }
        if (allowedColumns.contains("belgian_id_number")) {
            filteredUser.setBelgianIdNumber(user.getBelgianIdNumber());
        }
        return  filteredUser;
    }


    private boolean isAdminSuper(Authentication authentication) {
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_SUPER"));
        }
        return false;
    }

    private boolean isAdminNormal(Authentication authentication) {
        // Check if the user has the ROLE_ADMIN_NORMAL authority
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_NORMAL"));
    }

    private boolean parseResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode attributesNode = jsonNode.path("identity").path("attributes");
            JsonNode userRolesNode = attributesNode.path("user_role");

            if (userRolesNode.isArray()) {
                for (JsonNode role : userRolesNode) {
                    if ("ROLE_ADMIN_SUPER".equals(role.asText())) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            // Handle the exception (e.g., log it) and return false
            return false;
        }
    }

    private UserDto mapToUserDto(User user) {
        UserDto userDto = new UserDto();
        // Map the User fields to UserDto fields
        userDto.setFirstName(user.getName());
        userDto.setLastName(user.getName()); // Assuming User has a lastName field
        // Add logic to extract and set roles from User to UserDto
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName) // Assuming Role has a getName method
                .collect(Collectors.toList());
        userDto.setRoleNames(roleNames);

        return userDto;
    }








}






