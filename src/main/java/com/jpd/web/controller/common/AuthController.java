package com.jpd.web.controller.common;
//1. AuthController.java - PKCE VERSION

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

 @Value("${keycloak.auth-uri}")
 private String keycloakAuthUri;

 @Value("${keycloak.token-uri}")
 private String keycloakTokenUri;

 @Value("${keycloak.logout-uri}")
 private String keycloakLogoutUri;

 @Value("${keycloak.client-id}")
 private String clientId;

 @Value("${app.frontend-url}")
 private String frontendUrl;

 private final RestTemplate restTemplate = new RestTemplate();
 private final Map<String, String> codeVerifierStore = new HashMap<>();

 @GetMapping("/login-url")
 public ResponseEntity<?> getLoginUrl(@RequestParam String redirectUri) {
     try {
    	 System.out.print("da");
         String codeVerifier = generateCodeVerifier();
         String codeChallenge = generateCodeChallenge(codeVerifier);
         String state = generateState();

         codeVerifierStore.put(state, codeVerifier);

         String loginUrl = keycloakAuthUri +
                 "?client_id=" + clientId +
                 "&response_type=code" +
                 "&redirect_uri=" + redirectUri +
                 "&code_challenge=" + codeChallenge +
                 "&code_challenge_method=S256" +
                 "&state=" + state +
                 "&scope=openid profile email";

         return ResponseEntity.ok(Map.of(
                 "loginUrl", loginUrl,
                 "state", state
         ));
     } catch (Exception e) {
    	 System.out.print(e);
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                 .body(Map.of("error", "Failed to generate login URL"));
         
     }
 }

 @PostMapping("/callback")
 public ResponseEntity<?> handleCallback(@RequestBody Map<String, String> callbackData,
                                         HttpServletResponse response) {
     String code = callbackData.get("code");
     String state = callbackData.get("state");
     String redirectUri = callbackData.get("redirectUri");

     if (code == null || state == null) {
         return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                 .body(Map.of("error", "Missing code or state"));
     }

     String codeVerifier = codeVerifierStore.remove(state);
     if (codeVerifier == null) {
         return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                 .body(Map.of("error", "Invalid state"));
     }

     MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
     formData.add("grant_type", "authorization_code");
     formData.add("client_id", clientId);
     formData.add("code", code);
     formData.add("redirect_uri", redirectUri);
     formData.add("code_verifier", codeVerifier);

     HttpHeaders headers = new HttpHeaders();
     headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

     HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

     try {
         ResponseEntity<Map> keycloakResponse = restTemplate.postForEntity(keycloakTokenUri, request, Map.class);
         Map<String, Object> tokens = keycloakResponse.getBody();

         setTokenCookies(response,
                 (String) tokens.get("access_token"),
                 (String) tokens.get("refresh_token"),
                 (Integer) tokens.get("expires_in"),
                 (Integer) tokens.get("refresh_expires_in"));

         return ResponseEntity.ok(Map.of(
                 "success", true,
                 "expiresIn", tokens.get("expires_in")
         ));
     } catch (Exception e) {
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                 .body(Map.of("error", "Token exchange failed: " + e.getMessage()));
     }
 }

 @PostMapping("/refresh")
 public ResponseEntity<?> refreshToken(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                       HttpServletResponse response) {
     if (refreshToken == null) {
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                 .body(Map.of("error", "No refresh token"));
     }

     MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
     formData.add("grant_type", "refresh_token");
     formData.add("client_id", clientId);
     formData.add("refresh_token", refreshToken);

     HttpHeaders headers = new HttpHeaders();
     headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

     HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

     try {
         ResponseEntity<Map> keycloakResponse = restTemplate.postForEntity(keycloakTokenUri, request, Map.class);
         Map<String, Object> tokens = keycloakResponse.getBody();

         setTokenCookies(response,
                 (String) tokens.get("access_token"),
                 (String) tokens.get("refresh_token"),
                 (Integer) tokens.get("expires_in"),
                 (Integer) tokens.get("refresh_expires_in"));

         return ResponseEntity.ok(Map.of("success", true));
     } catch (Exception e) {
         clearTokenCookies(response);
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                 .body(Map.of("error", "Invalid refresh token"));
     }
 }

 @PostMapping("/logout")
 public ResponseEntity<?> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                 HttpServletResponse response) {
     if (refreshToken != null) {
         try {
             MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
             formData.add("client_id", clientId);
             formData.add("refresh_token", refreshToken);

             HttpHeaders headers = new HttpHeaders();
             headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

             HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
             restTemplate.postForEntity(keycloakLogoutUri, request, String.class);
         } catch (Exception e) {
             System.err.println("Keycloak logout failed: " + e.getMessage());
         }
     }

     clearTokenCookies(response);
     return ResponseEntity.ok(Map.of("success", true));
 }

 private String generateCodeVerifier() {
     SecureRandom secureRandom = new SecureRandom();
     byte[] codeVerifier = new byte[32];
     secureRandom.nextBytes(codeVerifier);
     return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
 }

 private String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
     byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
     MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
     messageDigest.update(bytes, 0, bytes.length);
     byte[] digest = messageDigest.digest();
     return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
 }

 private String generateState() {
     SecureRandom secureRandom = new SecureRandom();
     byte[] state = new byte[16];
     secureRandom.nextBytes(state);
     return Base64.getUrlEncoder().withoutPadding().encodeToString(state);
 }

 private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken,
                               Integer accessExpiry, Integer refreshExpiry) {
     Cookie accessCookie = new Cookie("access_token", accessToken);
     accessCookie.setHttpOnly(true);
     accessCookie.setSecure(false);
     accessCookie.setPath("/");
     accessCookie.setMaxAge(accessExpiry != null ? accessExpiry : 300);
     accessCookie.setAttribute("SameSite", "Lax");
     response.addCookie(accessCookie);

     if (refreshToken != null) {
         Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
         refreshCookie.setHttpOnly(true);
         refreshCookie.setSecure(false);
         refreshCookie.setPath("/");
         refreshCookie.setMaxAge(refreshExpiry != null ? refreshExpiry : 1800);
         refreshCookie.setAttribute("SameSite", "Lax");
         response.addCookie(refreshCookie);
     }
 }

 private void clearTokenCookies(HttpServletResponse response) {
     Cookie accessCookie = new Cookie("access_token", "");
     accessCookie.setHttpOnly(true);
     accessCookie.setSecure(false);
     accessCookie.setPath("/");
     accessCookie.setMaxAge(0);
     response.addCookie(accessCookie);

     Cookie refreshCookie = new Cookie("refresh_token", "");
     refreshCookie.setHttpOnly(true);
     refreshCookie.setSecure(false);
     refreshCookie.setPath("/");
     refreshCookie.setMaxAge(0);
     response.addCookie(refreshCookie);
 }
}