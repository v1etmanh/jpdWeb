package com.jpd.web.system.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    protected WebElement waitForVisibility(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }
    protected void selectByLabelText(String labelText, String value) {
        By label = By.xpath("//label[normalize-space()='" + labelText + "']/following::*[self::input or self::textarea][1]");
        WebElement input = waitForVisibility(label);
        input.clear();
        input.sendKeys(value);
    }

    protected void chooseSelectAfterLabel(String labelText, String optionText) {
        By select = By.xpath("//label[normalize-space()='" + labelText + "']/following::select[1]");
        wait.until(ExpectedConditions.elementToBeClickable(select)).click();
        // click the <option> by exact text
        By option = By.xpath("//label[normalize-space()='" + labelText + "']/following::select[1]/option[normalize-space()='" + optionText + "']");
        driver.findElement(option).click();
    }

    protected void uploadFileToInput(By fileInput, String absolutePath) {
        WebElement input = driver.findElement(fileInput);
        ((JavascriptExecutor)driver).executeScript("arguments[0].style.display='block';", input);
        input.sendKeys(absolutePath);
    }


    public String currentUrl() { return driver.getCurrentUrl(); }


    //click safe
    protected void click(By locator){
        waitForVisibility(locator).click();
    }
    //send keys safe
    protected void type(By locator, String text) {
        WebElement element = waitForVisibility(locator);
        element.clear();
        element.sendKeys(text);
    }
    //get text safe
    protected String getTextSafe(By locator) {
        return waitForVisibility(locator).getText();
    }
    //navigate to url
    public void navigateTo(String url) {
        driver.get(url);
    }
    //check if element is displayed
    protected boolean isElementVisible(By locator) {
        try {
            return waitForVisibility(locator).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }
    protected void scrollIntoView(WebElement el) {
        try {
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].scrollIntoView({block:'center', inline:'center'});", el);
        } catch (Exception ignored) {
            // fallback bằng Actions nếu JS thất bại
            try {
                new Actions(driver).moveToElement(el).perform();
            } catch (Exception ignore2) { /* no-op */ }
        }
    }

    protected void scrollIntoView(By locator) {
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        scrollIntoView(el);
    }
}
