package com.jpd.web.system.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;

public class CertificateUploadModal extends BasePage {

    private final By fileInput = By.xpath("//input[@type='file']");
    private final By uploadBtn = By.xpath("//button[@type='submit' and contains(text(),'Tải lên')]");

    public CertificateUploadModal(WebDriver driver) {
        super(driver);
        waitVisible(By.xpath("//h2[contains(text(),'Tải lên chứng chỉ')]"));
    }

    public void upload(String fileName) {
        String path = new File("src/test/resources/certificates/" + fileName).getAbsolutePath();
        WebElement input = driver.findElement(fileInput);

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].style.display='block'; arguments[0].style.visibility='visible';", input);

        input.sendKeys(path);

        // Chờ preview
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//p[contains(text(),'" + fileName + "')]")
            ));
        } catch (Exception e) {
            System.out.println("Không thấy preview");
        }
    }

    public void submit() {
        WebElement btn = waitClickable(uploadBtn);
        jsClick(btn);

        // Chờ loading
        try {
            wait.until(ExpectedConditions.textToBePresentInElement(btn, "Đang tải lên"));
            wait.until(ExpectedConditions.not(
                    ExpectedConditions.attributeContains(btn, "disabled", "true")
            ));
        } catch (Exception e) {
            System.out.println("Không thấy loading");
        }
    }

    public boolean isClosed() {
        try {
            driver.findElement(fileInput);
            return false;
        } catch (NoSuchElementException e) {
            return true;
        }
    }
}
