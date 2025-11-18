package com.jpd.web.system.pages;


import com.jpd.web.system.utils.Waits;
import org.openqa.selenium.*;

public class CreatorSidebar extends BasePage {

    public CreatorSidebar(WebDriver driver) {
        super(driver);
    }

    // Floating toggle button (draggable). Based on inline style from the app.
    private final By floatingToggle = By.cssSelector("button[style*='position: fixed'][style*='border-radius: 50%'][style*='width: 40px'][style*='height: 40px']");

    // "Create Your Course" link within the dropdown
    private final By linkCreateCourse = By.xpath("//a[@href='/creator/create_course' and normalize-space()='Create Your Course']");

    public void openMenu() {
        Waits.waitForClickable(driver, floatingToggle).click();
    }

    public void clickCreateYourCourse() {
        Waits.waitForClickable(driver, linkCreateCourse).click();
    }

    public void openCreateCourseViaSidebar() {
        openMenu();
        clickCreateYourCourse();
    }
}
