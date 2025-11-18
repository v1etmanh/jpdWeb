package com.jpd.web.system.pages;


import com.jpd.web.system.utils.Waits;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class CreateCoursePage extends BasePage {

    public CreateCoursePage(WebDriver driver) {
        super(driver);
    }

    private final By btnNext = By.xpath("//button[.//span[normalize-space()='Tiếp theo'] or normalize-space()='Tiếp theo']");
    private final By btnBack = By.xpath("//button[.//span[normalize-space()='Quay lại'] or normalize-space()='Quay lại']");
    private final By btnSubmit = By.xpath("//button[contains(normalize-space(),'Tạo khóa học')]"); // has icon and text

    // Step 1 fields
    public void fillStep1(String name, String description, String language, String teachingLanguage) {
        selectByLabelText("Tên khóa học", name);
        WebElement desc = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//label[normalize-space()='Mô tả khóa học']/following::textarea[1]")));
        desc.clear(); desc.sendKeys(description);

        if (language != null && !language.isBlank()) {
            chooseSelectAfterLabel("Ngôn ngữ", language);
        }
        if (teachingLanguage != null && !teachingLanguage.isBlank()) {
            chooseSelectAfterLabel("Ngôn ngữ giảng dạy", teachingLanguage);
        }
    }

    // Step 2 fields
    public void fillStep2(String targetAudience, String requirement, String learningObject) {
        selectByLabelText("Đối tượng học viên", targetAudience);
        selectByLabelText("Yêu cầu tiên quyết", requirement);
        selectByLabelText("Mục tiêu học tập", learningObject);
    }

    // Step 3 fields
    private final By fileInput = By.cssSelector("input[type='file'][accept^='image/']");
    public void fillStep3(String courseType, String imageAbsolutePath) {
        if (courseType != null && !courseType.isBlank()) {
            chooseSelectAfterLabel("Loại khóa học", courseType);
        }
        if (imageAbsolutePath != null && !imageAbsolutePath.isBlank()) {
            uploadFileToInput(fileInput, imageAbsolutePath);
        }
    }

    public void next() { Waits.waitForClickable(driver, btnNext).click(); }
    public void back() { Waits.waitForClickable(driver, btnBack).click(); }
    public void submit() { Waits.waitForClickable(driver, btnSubmit).click(); }

    /**
     * Wait for success by either:
     *  - Seeing success text anywhere on page body, or
     *  - Navigating to /creator/courseList
     */
    public boolean waitForSuccessOrRedirect() {
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            return w.until(d -> {
                try {
                    if (d.getCurrentUrl().contains("/creator/courseList")) return true;
                    String body = d.findElement(By.tagName("body")).getText();
                    if (body.contains("Khóa học đã được tạo thành công!")) return true;
                } catch (StaleElementReferenceException ignored) {}
                return false;
            });
        } catch (TimeoutException e) {
            return false;
        }
    }
}
