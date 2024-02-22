package com.example.registrationloginplainID.service;

import kong.unirest.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SqlAuthorizerService {

    private final RestTemplate restTemplate;

    @Value("${plainid.sqlpdp.url}")
    private String sqlAuthorizerApiUrl;


    @Value("${plainid.sqlpdp.clientID}")
    private String clientId;

    @Value("${plainid.sqlpdp.clientSecret}")
    private String clientSecret;
    @Autowired
    public SqlAuthorizerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Method to send requests to the PlainID SQL Authorizer API
     * @param sqlQuery
     * @param email
     * @return
     */
    public String sendQueryToAuthorizer(String sqlQuery, String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Construct the request body JSON
        JSONObject requestJson = new JSONObject();
        requestJson.put("sql", sqlQuery); // Ensure correct field name
        requestJson.put("clientId", clientId);
        requestJson.put("clientSecret", clientSecret);
        requestJson.put("user", email); // Replace with dynamic user if necessary
        // Add flags and other necessary fields

        HttpEntity<String> requestEntity = new HttpEntity<>(requestJson.toString(), headers);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(sqlAuthorizerApiUrl, requestEntity, String.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            // Extract and return the modified SQL query or handle the response
            JSONObject responseBodyJson = new JSONObject(responseEntity.getBody());
            if(responseBodyJson.has("error") && !responseBodyJson.getString("error").isEmpty()) {
                throw new RuntimeException("Error from SQL Authorizer: " + responseBodyJson.getString("error"));
            }
            return responseBodyJson.getString("sql"); // Assuming 'sql' contains the modified query
        } else {
            // Handle non-OK responses appropriately
            throw new RuntimeException("Failed to get response from SQL Authorizer, Status: " + responseEntity.getStatusCode());
        }
    }
}
