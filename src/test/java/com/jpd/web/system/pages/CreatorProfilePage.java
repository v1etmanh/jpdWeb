package com.jpd.web.system.pages;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class CreatorProfilePage extends BasePage {

    private final By title = By.xpath("//h1[contains(text(),'Thông tin tài khoản Creator')]");
    private final By certRow = By.xpath("//h3[contains(text(),'Chứng chỉ cá nhân')]//ancestor::div[contains(@class,'flex items-center p-6')]");
    private final By setupBtn = By.xpath(".//button[contains(text(),'Thiết lập') or contains(text(),'Chỉnh sửa')]");

    public CreatorProfilePage(WebDriver driver) {
        super(driver);
    }

    public void goTo(String baseUrl) {
        driver.navigate().to(baseUrl + "/creator/profile");
        wait.until(ExpectedConditions.urlContains("/creator/profile"));
        waitVisible(title);
    }

    public CertificateUploadModal openModal() {
        WebElement row = waitVisible(certRow);
        WebElement btn = row.findElement(setupBtn);
        scrollTo(btn);

        // Retry click
        for (int i = 0; i < 3; i++) {
            try {
                jsClick(btn);
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//h2[contains(text(),'Tải lên chứng chỉ')]")
                ));
                return new CertificateUploadModal(driver);
            } catch (Exception e) {
                System.out.println("Retry click modal: " + (i+1));
                try { Thread.sleep(1000); } catch (Exception ignored) {}
            }
        }
        throw new RuntimeException("Không mở được modal");
    }

    public boolean hasViewButton() {
        try {
            return driver.findElement(By.xpath("//button[contains(text(),'Xem')]")).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}
