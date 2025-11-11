package com.jpd.web.system.pages;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class LoginPage extends BasePage {

    private final By loginLink = By.xpath("//a[normalize-space()='Login']");
    private final By username = By.id("username");
    private final By password = By.id("password");
    private final By signIn = By.cssSelector("input[name='login'], #kc-login");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void login(String baseUrl, String email, String pwd) {
        navigate(baseUrl);
        wait.until(d -> {
            String token = (String) ((JavascriptExecutor) d)
                    .executeScript("return localStorage.getItem('access_token');");
            return token != null && !token.isEmpty();
        });
        System.out.println("Already logged in");

        // Click login
        jsClick(waitClickable(loginLink));
        wait.until(ExpectedConditions.urlContains("localhost:8080"));

        type(username, email);
        type(password, pwd);
        jsClick(waitClickable(signIn));

        // Chờ token kc_token
        wait.until(d -> {
            String token = (String) ((JavascriptExecutor) d)
                    .executeScript("return localStorage.getItem('kc_token');");
            return token != null && !token.isEmpty();
        });
        System.out.println("Login thành công, token có");
    }
}
