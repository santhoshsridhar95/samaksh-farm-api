package com.samaksh.farms.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@RequiredArgsConstructor
public class GoogleTokenVerifier {

    private final ObjectMapper objectMapper;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    public GoogleProfile verify(
            String idToken
    ) {

        if (googleClientId == null ||
                googleClientId.isBlank()) {

            throw new IllegalStateException(
                    "Google login is not configured"
            );
        }

        try {
            URI uri =
                    UriComponentsBuilder
                            .fromUriString("https://oauth2.googleapis.com/tokeninfo")
                            .queryParam("id_token", idToken)
                            .build()
                            .toUri();

            HttpRequest request =
                    HttpRequest.newBuilder(uri)
                            .GET()
                            .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient()
                            .send(
                                    request,
                                    HttpResponse.BodyHandlers.ofString()
                            );

            if (response.statusCode() != 200) {
                throw new IllegalArgumentException(
                        "Invalid Google token"
                );
            }

            GoogleProfile profile =
                    objectMapper.readValue(
                            response.body(),
                            GoogleProfile.class
                    );

            if (!googleClientId.equals(profile.getAudience())) {
                throw new IllegalArgumentException(
                        "Google token audience mismatch"
                );
            }

            if (!Boolean.parseBoolean(profile.getEmailVerified())) {
                throw new IllegalArgumentException(
                        "Google email is not verified"
                );
            }

            return profile;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Unable to verify Google token"
            );
        }
    }

    @Data
    public static class GoogleProfile {

        private String email;

        private String name;

        @JsonProperty("email_verified")
        private String emailVerified;

        @JsonProperty("aud")
        private String audience;
    }
}
