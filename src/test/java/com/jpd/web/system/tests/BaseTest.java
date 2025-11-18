package com.jpd.web.system.tests;

import com.jpd.web.system.utils.AuthUtil;
import com.jpd.web.system.utils.DriverFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.WebDriver;

public abstract class BaseTest {

    protected static WebDriver driver;
    protected static String baseUrl;

    @BeforeAll
    public static void globalSetUp() {
        driver = DriverFactory.create();
        baseUrl = System.getProperty("selenium.baseUrl", "http://localhost:3000");
        driver.get(baseUrl);
        // Optional auth bootstrap; reload app after localStorage is set
        AuthUtil.bootstrapLocalStorage(driver);
        driver.navigate().refresh();
    }

    @AfterAll
    public static void globalTearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
