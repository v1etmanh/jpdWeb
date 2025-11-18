package com.jpd.web.system.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class Waits {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    public static WebElement waitForVisible(WebDriver driver, By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(WebDriver driver, By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static boolean waitForGone(WebDriver driver, By locator) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static boolean waitForText(WebDriver driver, By locator, String text) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public static void sleepSilently(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
    }

    public static boolean waitUntil(WebDriver driver, ExpectedCondition<Boolean> condition) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT).until(condition);
    }
}
