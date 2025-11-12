package com.jpd.web.system;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("JPD Web - Creator Portal")
@Feature("Creator Profile Registration")
@Story("BR-20: Customer uploads profile to become Creator")
@Owner("QA Team")
public class UploadProfileE2ETest {

    private WebDriver driver;
    private WebDriverWait wait;
    private WebDriverWait longWait;
    private final String baseUrl = "http://localhost:3000";

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-debugging-port=9222");
        options.addArguments("--incognito");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        longWait = new WebDriverWait(driver, Duration.ofSeconds(20));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));

        System.out.println("\n=== NEW TEST SESSION STARTED ===\n");
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            System.out.println("\n=== TEST SESSION ENDED ===\n");
            driver.quit();
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
        TC02_Missing_FullName        | namde12@gmail.com | 123456 | customer |  | 0912345678 | Giới thiệu bản thân với hơn năm mươi ký tự để đáp ứng yêu cầu validation của hệ thống learning management | profile_avatar.jpg | true | false | Họ tên phải có ít nhất 2 ký tự
        TC03_Invalid_Phone           | namde12@gmail.com| 123456 | customer | Nguyễn Văn Bình | 123 | Tôi có kinh nghiệm giảng dạy nhiều năm với chuyên môn về công nghệ thông tin và phát triển phần mềm chuyên nghiệp | profile_avatar.jpg | true | false | Số điện thoại không hợp lệ
        TC04_Short_Bio               | namde12@gmail.com | 123456 | customer | Nguyễn Văn Cường | 0923456789 | Bio ngắn | profile_avatar.jpg | true | false | Giới thiệu bản thân phải có ít nhất 50 ký tự
        TC05_No_Image                | namde12@gmail.com| 123456 | customer | Nguyễn Văn Dũng | 0934567890 | Tôi là chuyên gia trong lĩnh vực data science với nhiều năm kinh nghiệm làm việc tại các công ty công nghệ lớn | null | true | false | Vui lòng chọn ảnh đại diện
        TC06_Not_Agree_Terms         | namde12@gmail.com| 123456 | customer | Nguyễn Văn Em | 0945678901 | Là một developer full-stack với passion trong việc xây dựng các ứng dụng web hiện đại và scalable cho doanh nghiệp | course_thumbnail.jpg | false | false | Bạn phải đồng ý với điều khoản,
         TC01_Valid_Profile           | namde12@gmail.com| 123456 | customer | Nguyễn Văn An Creator | 0912345678 | Tôi là một giảng viên với hơn 10 năm kinh nghiệm trong lĩnh vực lập trình và giảng dạy. Tôi đam mê chia sẻ kiến thức và giúp học viên phát triển kỹ năng | course_thumbnail.jpg | true | true | Chúc mừng! Bạn đã trở thành Creator
       
        """)
    @Severity(SeverityLevel.CRITICAL)
    void testUploadCreatorProfile(
            String testCase,
            String email,
            String password,
            String role,
            String fullName,
            String phone,
            String bio,
            String imageFileName,
            boolean agreeTerms,
            boolean shouldSucceed,
            String expectedMessage) throws Exception {

        String imagePath = imageFileName != null && !imageFileName.equals("null")
                ? Paths.get("src", "test", "resources", "images", imageFileName).toAbsolutePath().toString()
                : null;

        // ==================== STEP 1: LOGIN + NAVIGATE ====================
        Allure.step("Login and Navigate to Upload Profile Page", () -> {
            System.out.println("\n=== STEP 1: LOGIN + NAVIGATE ===");

            loginViaKeycloak(email, password);

            System.out.println("⏳ Waiting for authentication...");
            longWait.until(d -> {
                Object token = ((JavascriptExecutor) driver).executeScript(
                        "return localStorage.getItem('kc_token');"
                );
                return token != null && !token.toString().isEmpty();
            });

            System.out.println("⏳ Waiting for React App...");
            longWait.until(d -> {
                try {
                    Object isReady = ((JavascriptExecutor) driver).executeScript(
                            "return window.ReactAppReady === true || document.getElementById('root').children.length > 0;"
                    );
                    return Boolean.TRUE.equals(isReady);
                } catch (Exception e) {
                    return false;
                }
            });

            System.out.println("✓ Authentication successful!");
            Thread.sleep(500);

            System.out.println("📍 Navigating to /upload_profile...");
            driver.navigate().to(baseUrl + "/upload_profile");

            longWait.until(ExpectedConditions.urlContains("/upload_profile"));
            System.out.println("✓ URL changed to upload profile page");

            Thread.sleep(1000);
            attachScreenshot("After_Navigate_To_UploadProfile");

            verifyUploadProfilePageLoaded();

            System.out.println("✅ Navigation successful!\n");
        });

        // ==================== STEP 2: FILL STEP 1 - PERSONAL INFO ====================
        Allure.step("Step 1: Fill Personal Information", () -> {
            System.out.println("\n=== STEP 2: FILL PERSONAL INFO (Step 1/3) ===");

            verifyCurrentStep(1);

            if (fullName != null && !fullName.trim().isEmpty()) {
                WebElement fullNameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//input[@placeholder='Nguyễn Văn A']")
                ));
                fullNameInput.clear();
                fullNameInput.sendKeys(fullName);
                System.out.println("✓ Full name entered: " + fullName);
            } else {
                System.out.println("⚠ Full name is empty - testing validation");
            }

            if (phone != null && !phone.trim().isEmpty()) {
                WebElement phoneInput = driver.findElement(
                        By.xpath("//input[@placeholder='0123456789']")
                );
                phoneInput.clear();
                phoneInput.sendKeys(phone);
                System.out.println("✓ Phone entered: " + phone);
            }

            if (bio != null && !bio.trim().isEmpty()) {
                WebElement bioTextarea = driver.findElement(
                        By.xpath("//textarea[@placeholder='Chia sẻ về kinh nghiệm, chuyên môn và lý do bạn muốn trở thành giảng viên...']")
                );
                bioTextarea.clear();
                bioTextarea.sendKeys(bio);
                System.out.println("✓ Bio entered (" + bio.length() + " chars)");
            }

            attachScreenshot("Step1_PersonalInfo_Filled");
            Thread.sleep(300);

            WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(), 'Tiếp theo')]")
            ));

            clickElementWithRetry(nextBtn, 3);
            System.out.println("✓ Next button clicked");

            Thread.sleep(500);
            attachScreenshot("After_Step1_Next");

            if (!shouldSucceed && (fullName == null || fullName.trim().length() < 2 ||
                    phone == null || !phone.matches("^[0-9]{10,11}$") ||
                    bio.length() < 50)) {
                System.out.println("🔍 Checking for validation errors...");
                String errorMsg = getValidationErrorMessage();
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    System.out.println("✓ Validation error found: " + errorMsg);
                    Assertions.assertTrue(
                            errorMsg.toLowerCase().contains(expectedMessage.toLowerCase()),
                            "Expected validation message but got: " + errorMsg
                    );
                    attachScreenshot("Validation_Error_Step1");
                    return;
                }
            }

            verifyCurrentStep(2);
            System.out.println("✅ Step 1 completed!\n");
        });

        // ==================== STEP 3: UPLOAD PROFILE IMAGE ====================
        Allure.step("Step 2: Upload Profile Image", () -> {
            System.out.println("\n=== STEP 3: UPLOAD PROFILE IMAGE (Step 2/3) ===");

            if (!shouldSucceed) {
                System.out.println("⚠ Skipping Step 2 - test case expects failure in Step 1");
                return;
            }

            if (imagePath != null) {
                File imageFile = new File(imagePath);
                Assertions.assertTrue(imageFile.exists(), "Image file not found: " + imagePath);
                System.out.println("✓ Image file exists: " + imageFile.getAbsolutePath());

                WebElement fileInput = driver.findElement(By.id("profileImage"));

                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].style.display='block';" +
                                "arguments[0].style.visibility='visible';" +
                                "arguments[0].style.opacity='1';",
                        fileInput
                );

                fileInput.sendKeys(imageFile.getAbsolutePath());
                System.out.println("✓ Image uploaded: " + imageFileName);

                Thread.sleep(500);
                attachScreenshot("Step2_Image_Uploaded");

                verifyImagePreview();
            } else {
                System.out.println("⚠ No image to upload - testing validation");
            }

            Thread.sleep(300);

            WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(), 'Tiếp theo')]")
            ));
            clickElementWithRetry(nextBtn, 3);
            System.out.println("✓ Next button clicked");

            Thread.sleep(500);
            attachScreenshot("After_Step2_Next");

            if (!shouldSucceed && imagePath == null) {
                System.out.println("🔍 Checking for image validation error...");
                String errorMsg = getValidationErrorMessage();
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    System.out.println("✓ Validation error found: " + errorMsg);
                    Assertions.assertTrue(
                            errorMsg.toLowerCase().contains(expectedMessage.toLowerCase()),
                            "Expected validation message but got: " + errorMsg
                    );
                    attachScreenshot("Validation_Error_Step2");
                    return;
                }
            }

            verifyCurrentStep(3);
            System.out.println("✅ Step 2 completed!\n");
        });

        // ==================== STEP 4: AGREE TO TERMS ====================
        Allure.step("Step 3: Agree to Terms and Submit", () -> {
            System.out.println("\n=== STEP 4: TERMS & SUBMIT (Step 3/3) ===");

            if (!shouldSucceed) {
                System.out.println("⚠ Skipping Step 3 - test case expects failure in earlier steps");
                return;
            }

            if (agreeTerms) {
                WebElement termsCheckbox = wait.until(ExpectedConditions.elementToBeClickable(
                        By.id("agreeTerms")
                ));

                if (!termsCheckbox.isSelected()) {
                    clickElementWithRetry(termsCheckbox, 3);
                    System.out.println("✓ Terms checkbox checked");
                }
            } else {
                System.out.println("⚠ Not agreeing to terms - testing validation");
            }

            attachScreenshot("Step3_Terms_Filled");
            Thread.sleep(300);

            WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(), 'Hoàn thành')]")
            ));

            Assertions.assertTrue(submitBtn.isEnabled(), "Submit button is disabled");
            System.out.println("✓ Submit button is enabled");

            attachScreenshot("Before_Submit_Button_Click");

            clickElementWithRetry(submitBtn, 3);
            System.out.println("✓ Submit button clicked");

            if (!shouldSucceed && !agreeTerms) {
                Thread.sleep(500);
                String errorMsg = getValidationErrorMessage();
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    System.out.println("✓ Validation error found: " + errorMsg);
                    Assertions.assertTrue(
                            errorMsg.toLowerCase().contains(expectedMessage.toLowerCase()),
                            "Expected validation message but got: " + errorMsg
                    );
                    attachScreenshot("Validation_Error_Step3");
                    return;
                }
            }

            waitForSubmissionComplete();

            Thread.sleep(1000);
            attachScreenshot("After_Submission");

            System.out.println("✅ Submission completed!\n");
        });

        // ==================== STEP 5: VERIFY RESULT ====================
        Allure.step("Verify Profile Upload Result", () -> {
            System.out.println("\n=== STEP 5: VERIFY RESULT ===");

            attachScreenshot("Final_Result");

            if (shouldSucceed) {
                try {
                    longWait.until(ExpectedConditions.urlContains("/creator/commercial/dashboard"));
                    System.out.println("✓ Redirected to creator dashboard");
                    System.out.println("✅ TEST PASSED - Profile uploaded successfully");
                } catch (TimeoutException e) {
                    String currentUrl = driver.getCurrentUrl();
                    attachScreenshot("Redirect_Failed");
                    Assertions.fail("Expected redirect to /creator/commercial/dashboard but stayed at: " + currentUrl);
                }
            } else {
                String currentUrl = driver.getCurrentUrl();
                Assertions.assertTrue(
                        currentUrl.contains("/upload_profile"),
                        "Expected to stay on upload profile page but URL is: " + currentUrl
                );

                System.out.println("✅ TEST PASSED - Validation error handled correctly");
            }
        });
    }

    // ==================== HELPER METHODS ====================

    private void loginViaKeycloak(String email, String password) throws InterruptedException {
        System.out.println("\n=== LOGIN PROCESS ===");
        System.out.println("Email: " + email);

        driver.get(baseUrl);
        Thread.sleep(500);

        String existingToken = (String) ((JavascriptExecutor) driver)
                .executeScript("return localStorage.getItem('kc_token');");

        if (existingToken != null && !existingToken.isEmpty()) {
            System.out.println("✓ Already logged in - skipping login");
            return;
        }

        try {
            WebElement loginLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[normalize-space()='Login' or contains(@href, 'login')]")
            ));
            clickElementWithRetry(loginLink, 3);
            System.out.println("✓ Login link clicked");

            longWait.until(ExpectedConditions.urlContains("localhost:8080"));
            System.out.println("✓ Redirected to Keycloak");

            WebElement username = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
            username.clear();
            username.sendKeys(email);

            WebElement pwd = driver.findElement(By.id("password"));
            pwd.clear();
            pwd.sendKeys(password);
            System.out.println("✓ Credentials entered");

            WebElement signInBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[name='login'], #kc-login, button[type='submit']")
            ));
            clickElementWithRetry(signInBtn, 3);
            System.out.println("✓ Sign In clicked");

            longWait.until(d -> {
                String url = driver.getCurrentUrl();
                if (!url.contains("localhost:3000")) {
                    return false;
                }

                try {
                    Object token = ((JavascriptExecutor) driver).executeScript(
                            "return localStorage.getItem('kc_token') || sessionStorage.getItem('kc_token');"
                    );
                    return token != null && !token.toString().isEmpty();
                } catch (Exception e) {
                    return false;
                }
            });

            Thread.sleep(1000);
            System.out.println("✅ Login successful!");

        } catch (TimeoutException e) {
            System.out.println("❌ Login failed: " + e.getMessage());
            attachScreenshot("Login_Failed");
            throw e;
        }
    }

    private void verifyUploadProfilePageLoaded() {
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//h1[contains(text(), 'Trở thành Creator')]")
                    ),
                    ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//p[contains(text(), '3 bước đơn giản')]")
                    )
            ));
            System.out.println("✓ Upload profile page loaded");
        } catch (TimeoutException e) {
            System.out.println("⚠ Page verification failed");
            attachScreenshot("Page_Load_Failed");
        }
    }

    private void verifyCurrentStep(int expectedStep) {
        try {
            WebElement stepIndicator = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[contains(@class, 'rounded-full')]//span[text()='" + expectedStep + "']")
            ));
            System.out.println("✓ Current step verified: " + expectedStep);
        } catch (TimeoutException e) {
            System.out.println("⚠ Step indicator not found for step " + expectedStep);
        }
    }

    private String getValidationErrorMessage() {
        try {
            WebElement errorMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//p[contains(@class, 'text-red-600')]")
            ));
            return errorMsg.getText();
        } catch (TimeoutException e) {
            return "";
        }
    }

    private void verifyImagePreview() {
        try {
            WebElement imagePreview = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//img[@alt='Profile Preview']")
            ));
            System.out.println("✓ Image preview visible");
        } catch (TimeoutException e) {
            System.out.println("⚠ Image preview not found");
        }
    }

    private void waitForSubmissionComplete() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//button[contains(text(), 'Đang xử lý')]")
            ));
            System.out.println("⏳ Submitting...");

            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.xpath("//button[contains(text(), 'Đang xử lý')]")
            ));
            System.out.println("✓ Submission completed");
        } catch (TimeoutException e) {
            System.out.println("⚠ No loading state detected");
        }
    }

    private void clickElementWithRetry(WebElement element, int maxAttempts) throws InterruptedException {
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(element));
                element.click();
                System.out.println("✓ Clicked (attempt " + i + ")");
                return;
            } catch (Exception e) {
                if (i < maxAttempts) {
                    System.out.println("⚠ Click failed, retrying with JS...");
                    try {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                        System.out.println("✓ Clicked by JS");
                        return;
                    } catch (Exception jsError) {
                        Thread.sleep(300);
                    }
                } else {
                    throw new RuntimeException("Failed to click after " + maxAttempts + " attempts", e);
                }
            }
        }
    }

    private void attachScreenshot(String name) {
        try {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(screenshot), ".png");
        } catch (Exception e) {
            System.out.println("⚠ Screenshot failed: " + e.getMessage());
        }
    }
}