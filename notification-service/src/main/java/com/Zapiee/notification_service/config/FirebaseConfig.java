package com.Zapiee.notification_service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource; // CRITICAL: Must use Spring's Resource class

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Value("${app.firebase-config-file}")
    private Resource configFile;

    @PostConstruct
    public void initialize(){
        try {
            // Read the json credentials file from resources
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(configFile.getInputStream()))
                    .build();

            // Initialize the app if no instance exists yet
            if(FirebaseApp.getApps().isEmpty()){
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Application initialized successfully for service.");
            }
        } catch (IOException e){
            System.err.println("Fatal: Failed to initialize Firebase SDK: " + e.getMessage());
        }
    }
}
