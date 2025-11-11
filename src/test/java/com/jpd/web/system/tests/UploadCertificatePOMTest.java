package com.jpd.web.system.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.*;
import com.jpd.web.system.pages.*;
import com.jpd.web.system.utils.TestDataProvider;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

@Epic("JPD Web - Creator Portal")
@Feature("Certificate Management")
@Story("BR-18: Creator uploads certificate")
public class UploadCertificatePOMTest extends BaseTest {

    private final String BASE_URL = "http://localhost:3000";
    private static LoginPage loginPage;
    private static CreatorProfilePage profilePage;

    @BeforeAll
    static void init() {  // ✅ MUST BE STATIC
        // Note: driver will be initialized in BaseTest's @BeforeEach
        // So we can't initialize pages here. Move to test method instead.
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.jpd.web.system.utils.TestDataProvider#uploadCases")  // ✅ FULL PACKAGE PATH
    @Severity(SeverityLevel.CRITICAL)
    void testUpload(String name, String email, String pwd, String file, boolean success, String toast) {
        // Initialize pages here (after driver is ready)
        loginPage = new LoginPage(driver);
        profilePage = new CreatorProfilePage(driver);

        Allure.step("Login & Navigate", () -> {
            loginPage.login(BASE_URL, email, pwd);
            profilePage.goTo(BASE_URL);
            attach("After Login");
        });

        Allure.step("Upload File: " + file, () -> {
            CertificateUploadModal modal = profilePage.openModal();
            modal.upload(file);
            modal.submit();
            assertTrue(modal.isClosed(), "Modal should close");
            attach("After Upload");
        });

        Allure.step("Verify Result", () -> {
            String msg = getToast();
            if (success) {
                assertTrue(msg.toLowerCase().contains("success") || msg.contains("thành công"),
                        "Expected success, got: " + msg);
            } else {
                assertTrue(msg.contains(toast) || msg.toLowerCase().contains("lỗi"),
                        "Expected error, got: " + msg);
            }
            attach("Final Result");
        });
    }

    private String getToast() {
        try {
            return driver.findElement(By.cssSelector(".Toastify__toast-body, .swal2-html-container"))
                    .getText().trim();
        } catch (Exception e) {
            return profilePage.hasViewButton() ? "Thành công" : "Không xác định";
        }
    }

    private void attach(String name) {
        try {
            byte[] shot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(shot), ".png");
        } catch (Exception e) {
            System.out.println("⚠ Screenshot failed: " + e.getMessage());
        }
    }
}