package com.jpd.web.system.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginPage extends BasePage {

    private final By loginLink = By.xpath("//a[normalize-space()='Login']");
    private final By username = By.id("username");
    private final By password = By.id("password");
    private final By signIn = By.cssSelector("input[name='login'], #kc-login");

    private WebDriverWait longWait;

    public LoginPage(WebDriver driver) {
        super(driver);
        this.longWait = new WebDriverWait(driver, Duration.ofSeconds(60));
    }

    public void login(String baseUrl, String email, String pwd) {
        System.out.println("\n=== LOGIN PROCESS ===");
        System.out.println("Email: " + email);

        navigate(baseUrl);

        try {
            Thread.sleep(2000); // Đợi page load
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // ✅ FIX: CHECK if already logged in (không phải WAIT cho token)
        String existingToken = (String) ((JavascriptExecutor) driver)
                .executeScript("return localStorage.getItem('kc_token');");

        if (existingToken != null && !existingToken.isEmpty()) {
            System.out.println("✓ Already logged in - skipping login");
            waitForReactReady();
            return; // ✅ RETURN NGAY nếu đã login
        }

        System.out.println("⏳ No token found - proceeding with login...");

        // Click login link
        try {
            WebElement link = waitClickable(loginLink);
            jsClick(link);
            System.out.println("✓ Login link clicked");
        } catch (Exception e) {
            System.out.println("❌ Cannot find login link: " + e.getMessage());
            throw new RuntimeException("Login link not found", e);
        }

        // Wait for Keycloak redirect
        longWait.until(ExpectedConditions.urlContains("localhost:8080"));
        System.out.println("✓ Redirected to Keycloak");

        // Enter credentials
        type(username, email);
        type(password, pwd);
        System.out.println("✓ Credentials entered");

        // Click Sign In
        WebElement signInBtn = waitClickable(signIn);
        jsClick(signInBtn);
        System.out.println("✓ Sign In clicked");

        // ✅ Wait for redirect back AND token storage
        longWait.until(d -> {
            try {
                String currentUrl = d.getCurrentUrl();

                // Check if redirected back to app (not Keycloak)
                boolean redirectedBack = currentUrl.contains(baseUrl) &&
                        !currentUrl.contains("localhost:8080");

                if (!redirectedBack) {
                    return false;
                }

                // Check for token
                Object tokenObj = ((JavascriptExecutor) d)
                        .executeScript("return localStorage.getItem('kc_token');");

                String token = tokenObj != null ? tokenObj.toString() : null;
                boolean hasToken = token != null && !token.isEmpty() && !"null".equals(token);

                if (hasToken) {
                    System.out.println("✓ Login successful - Token stored");
                    return true;
                }

                return false;

            } catch (Exception e) {
                return false;
            }
        });

        // Wait for React to be ready
        waitForReactReady();

        System.out.println("✅ Login completed!\n");
    }

    private void waitForReactReady() {
        try {
            System.out.println("⏳ Waiting for React to be ready...");

            longWait.until(d -> {
                try {
                    Object isReady = ((JavascriptExecutor) d).executeScript(
                            "return window.ReactAppReady === true || document.getElementById('root').children.length > 0;"
                    );
                    return Boolean.TRUE.equals(isReady);
                } catch (Exception e) {
                    return false;
                }
            });

            System.out.println("✓ React App Ready!");
            Thread.sleep(2000); // Extra wait

        } catch (Exception e) {
            System.out.println("⚠ React ready check timeout - continuing anyway");
        }
    }
}