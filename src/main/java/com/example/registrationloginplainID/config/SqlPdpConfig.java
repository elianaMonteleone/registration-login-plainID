package com.example.registrationloginplainID.config;

import com.plainid.libs.sqlpdplib.structs.QueryModificationFlags;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SqlPdpConfig {

    @Bean
    public QueryModificationFlags queryModificationFlags() {
        // Configure QueryModificationFlags according to your needs
        return new QueryModificationFlags(true, true, false, true, true, "OR", true);
    }


    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
