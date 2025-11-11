package com.jpd.web.system.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import com.jpd.web.system.utils.DriverFactory;

public abstract class BaseTest {
    protected WebDriver driver;

    @BeforeEach  // ✅ TẠO MỚI driver cho MỖI test
    void setup() {
        System.out.println("\n=== NEW TEST SESSION STARTED ===\n");
        driver = DriverFactory.createDriver();
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(10));
    }

    @AfterEach  // ✅ ĐÓNG driver sau mỗi test
    void tearDown() {
        if (driver != null) {
            System.out.println("\n=== TEST SESSION ENDED ===\n");
            driver.quit();
        }
    }
}