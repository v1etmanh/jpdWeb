package com.jpd.web.system.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class DriverFactory {
    public static WebDriver createDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--start-maximized", "--disable-notifications",
                "--no-sandbox", "--disable-dev-shm-usage",
                "--disable-gpu", "--remote-debugging-port=9222"
        );
        return new ChromeDriver(options);
    }
}