package com.jpd.web.system.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import com.jpd.web.system.utils.DriverFactory;

public abstract class BaseTest {
    protected static WebDriver driver;

    @BeforeAll
    static void setup() {
        driver = DriverFactory.createDriver();
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(10));
    }

    @AfterEach
    void reset() {
        if (driver != null) {
            driver.manage().deleteAllCookies();
            ((JavascriptExecutor) driver).executeScript("localStorage.clear(); sessionStorage.clear();");
        }
    }
}
