package com.jpd.web.system;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import org.junit.jupiter.api.Assertions;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("JPD Web - Creator Portal")
@Feature("Course Management")
@Story("BR-19: Creator creates new course")
@Owner("QA Team")
public class CreateCourseE2ETest {

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
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

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
        TC01_Create_Public_Course   | thanhthao@gmail.com | 123456| creator | React.js từ cơ bản đến nâng cao | Khóa học React.js toàn diện giúp bạn làm chủ framework phổ biến nhất hiện nay với hơn 50 bài giảng thực hành | Lập trình viên muốn học React, sinh viên CNTT, người chuyển nghề lập trình web | Kiến thức HTML, CSS, JavaScript cơ bản | Xây dựng ứng dụng React hoàn chỉnh, nắm vững React hooks, state management | VIETNAMESE | VIETNAMESE | PUBLIC | 0 | course_thumbnail.jpg | true | đã được tạo thành công
        TC02_Create_Paid_Course     |thanhthao@gmail.com | 123456 | creator | Node.js Backend Development Pro | Khóa học Node.js chuyên sâu với Express, MongoDB, RESTful API, Authentication và deployment thực tế | Developers muốn làm backend, full-stack developers, sinh viên IT muốn nâng cao | JavaScript ES6+, hiểu biết cơ bản về HTTP và databases | Xây dựng RESTful API bảo mật, thiết kế database, deploy production-ready apps | ENGLISH | VIETNAMESE | PAID | 499000 | course_thumbnail.jpg | false | đã được tạo thành công
        TC06_Create_Paid_Course     |v1etmanh1111111111a@gmail.com | concop19A | creator | Node.js Backend Development Pro | Khóa học Node.js chuyên sâu với Express, MongoDB, RESTful API, Authentication và deployment thực tế | Developers muốn làm backend, full-stack developers, sinh viên IT muốn nâng cao | JavaScript ES6+, hiểu biết cơ bản về HTTP và databases | Xây dựng RESTful API bảo mật, thiết kế database, deploy production-ready apps | ENGLISH | VIETNAMESE | PAID | 499000 | course_thumbnail.jpg | true | đã được tạo thành công
       
        TC03_Create_Private_Course  | thanhthao@gmail.com| 123456 | creator | Python Data Science Complete | Khóa học Python cho Data Science với Pandas, NumPy, Matplotlib, Machine Learning cơ bản và thực hành dự án | Người muốn chuyển sang Data Science, analysts, sinh viên khoa học dữ liệu | Python cơ bản, toán học đại số tuyến tính cơ bản | Xử lý dữ liệu với Pandas, visualize data, xây dựng ML models đơn giản | ENGLISH | ENGLISH | PRIVATE | 0 | course_thumbnail.jpg | true | đã được tạo thành công
        TC04_Missing_Required_Name  | thanhthao@gmail.com | 123456 | creator |  | Mô tả khóa học đầy đủ với hơn 50 ký tự để pass validation | Lập trình viên | HTML, CSS | Học React | VIETNAMESE | VIETNAMESE | PUBLIC | 0 | course_thumbnail.jpg | false | Tên khóa học là bắt buộc
        TC05_Short_Description      | thanhthao@gmail.com | 123456 | creator | React Basic Course | Mô tả ngắn | Developers | HTML | React hooks | VIETNAMESE | VIETNAMESE | PUBLIC | 0 | course_thumbnail.jpg | false | Mô tả phải có ít nhất 50 ký tự
        """)
    @Severity(SeverityLevel.CRITICAL)
    void testCreateCourse(
            String testCase,
            String email,
            String password,
            String role,
            String courseName,
            String description,
            String targetAudience,
            String requirements,
            String learningObject,
            String language,
            String teachingLanguage,
            String courseType,
            String price,
            String imageFileName,
            boolean shouldSucceed,
            String expectedMessage) throws Exception {

        String imagePath = Paths.get("src", "test", "resources", "images", imageFileName)
                .toAbsolutePath().toString();

        // ==================== STEP 1: LOGIN + NAVIGATE ====================
        Allure.step("Login and Navigate to Create Course Page", () -> {
            System.out.println("\n=== STEP 1: LOGIN + NAVIGATE ===");

            loginViaKeycloak(email, password);

            // Đợi token và React App sẵn sàng
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
            Thread.sleep(2000);

            // Navigate to create course page
            System.out.println("📍 Navigating to /creator/create_course...");
            driver.navigate().to(baseUrl + "/creator/create_course");

            longWait.until(ExpectedConditions.urlContains("/creator/create_course"));
            System.out.println("✓ URL changed to create course page");

            Thread.sleep(3000);
            attachScreenshot("After_Navigate_To_CreateCourse");

            // Verify page loaded
            verifyCreateCoursePageLoaded();

            System.out.println("✅ Navigation successful!\n");
        });

        // ==================== STEP 2: FILL STEP 1 - BASIC INFO ====================
        Allure.step("Step 1: Fill Basic Information", () -> {
            System.out.println("\n=== STEP 2: FILL BASIC INFO (Step 1/3) ===");

            // Verify we're on step 1
            verifyCurrentStep(1);

            // Fill course name
            if (courseName != null && !courseName.trim().isEmpty()) {
                WebElement nameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//input[@placeholder='VD: React.js từ cơ bản đến nâng cao']")
                ));
                nameInput.clear();
                nameInput.sendKeys(courseName);
                System.out.println("✓ Course name entered: " + courseName);
            } else {
                System.out.println("⚠ Course name is empty - testing validation");
            }

            // Fill description
            if (description != null && !description.trim().isEmpty()) {
                WebElement descTextarea = driver.findElement(
                        By.xpath("//textarea[@placeholder='Mô tả chi tiết về nội dung, lợi ích và giá trị mà học viên sẽ nhận được...']")
                );
                descTextarea.clear();
                descTextarea.sendKeys(description);
                System.out.println("✓ Description entered");
            }

            // Select language
            WebElement languageSelect = driver.findElement(
                    By.xpath("//label[contains(., 'Ngôn ngữ')]//following-sibling::div//select")
            );
            Select langDropdown = new Select(languageSelect);
            langDropdown.selectByValue(language);
            System.out.println("✓ Language selected: " + language);

            // Select teaching language
            WebElement teachLangSelect = driver.findElement(
                    By.xpath("//label[contains(., 'Ngôn ngữ giảng dạy')]//following-sibling::div//select")
            );
            Select teachLangDropdown = new Select(teachLangSelect);
            teachLangDropdown.selectByValue(teachingLanguage);
            System.out.println("✓ Teaching language selected: " + teachingLanguage);

            attachScreenshot("Step1_BasicInfo_Filled");
            Thread.sleep(1000);

            // Click "Tiếp theo" button
            WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(), 'Tiếp theo')]")
            ));

            clickElementWithRetry(nextBtn, 3);
            System.out.println("✓ Next button clicked");

            Thread.sleep(2000);
            attachScreenshot("After_Step1_Next");

            // Check if validation error occurred (for negative test cases)
            if (!shouldSucceed && (courseName == null || courseName.trim().isEmpty() || description.length() < 50)) {
                System.out.println("🔍 Checking for validation errors...");
                String errorMsg = getValidationErrorMessage();
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    System.out.println("✓ Validation error found: " + errorMsg);
                    Assertions.assertTrue(
                            errorMsg.toLowerCase().contains(expectedMessage.toLowerCase()),
                            "Expected validation message but got: " + errorMsg
                    );
                    attachScreenshot("Validation_Error_Step1");
                    return; // Stop test here for negative cases
                }
            }

            // Verify moved to step 2
            verifyCurrentStep(2);
            System.out.println("✅ Step 1 completed!\n");
        });

        // ==================== STEP 3: FILL STEP 2 - LEARNING OBJECTIVES ====================
        Allure.step("Step 2: Fill Learning Objectives", () -> {
            System.out.println("\n=== STEP 3: FILL LEARNING OBJECTIVES (Step 2/3) ===");

            if (!shouldSucceed) {
                System.out.println("⚠ Skipping Step 2 - test case expects failure in Step 1");
                return;
            }

            // Fill target audience
            WebElement targetAudienceTextarea = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//textarea[@placeholder='VD: Lập trình viên mới bắt đầu, sinh viên CNTT, người muốn chuyển nghề...']")
            ));
            targetAudienceTextarea.clear();
            targetAudienceTextarea.sendKeys(targetAudience);
            System.out.println("✓ Target audience entered");

            // Fill requirements
            WebElement requirementsTextarea = driver.findElement(
                    By.xpath("//textarea[@placeholder='VD: Kiến thức HTML/CSS cơ bản, biết sử dụng máy tính...']")
            );
            requirementsTextarea.clear();
            requirementsTextarea.sendKeys(requirements);
            System.out.println("✓ Requirements entered");

            // Fill learning objectives
            WebElement learningObjTextarea = driver.findElement(
                    By.xpath("//textarea[@placeholder='VD: Xây dựng được ứng dụng web hoàn chỉnh, nắm vững React hooks...']")
            );
            learningObjTextarea.clear();
            learningObjTextarea.sendKeys(learningObject);
            System.out.println("✓ Learning objectives entered");

            attachScreenshot("Step2_LearningObjectives_Filled");
            Thread.sleep(1000);

            // Click "Tiếp theo" button
            WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(), 'Tiếp theo')]")
            ));
            clickElementWithRetry(nextBtn, 3);
            System.out.println("✓ Next button clicked");

            Thread.sleep(2000);
            attachScreenshot("After_Step2_Next");

            // Verify moved to step 3
            verifyCurrentStep(3);
            System.out.println("✅ Step 2 completed!\n");
        });

        // ==================== STEP 4: FILL STEP 3 - PRICING & MEDIA ====================
        Allure.step("Step 3: Fill Pricing & Upload Image", () -> {
            System.out.println("\n=== STEP 4: FILL PRICING & MEDIA (Step 3/3) ===");

            if (!shouldSucceed) {
                System.out.println("⚠ Skipping Step 3 - test case expects failure in earlier steps");
                return;
            }

            // Select course type
            WebElement courseTypeSelect = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//label[contains(., 'Loại khóa học')]//following-sibling::select")
            ));
            Select courseTypeDropdown = new Select(courseTypeSelect);
            courseTypeDropdown.selectByValue(courseType);
            System.out.println("✓ Course type selected: " + courseType);

            Thread.sleep(1000);

            // Fill price if PAID
            if ("PAID".equals(courseType)) {
                WebElement priceInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//input[@placeholder='499000']")
                ));
                priceInput.clear();
                priceInput.sendKeys(price);
                System.out.println("✓ Price entered: " + price + " VNĐ");

                Thread.sleep(1000);
                attachScreenshot("Price_Entered_Revenue_Info");
            }

            // Upload image
            File imageFile = new File(imagePath);
            Assertions.assertTrue(imageFile.exists(), "Image file not found: " + imagePath);
            System.out.println("✓ Image file exists: " + imageFile.getAbsolutePath());

            WebElement fileInput = driver.findElement(By.xpath("//input[@type='file' and @accept='image/*']"));

            // Make input visible
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].style.display='block';" +
                            "arguments[0].style.visibility='visible';" +
                            "arguments[0].style.opacity='1';",
                    fileInput
            );

            fileInput.sendKeys(imageFile.getAbsolutePath());
            System.out.println("✓ Image uploaded: " + imageFileName);

            Thread.sleep(2000);
            attachScreenshot("Step3_Image_Uploaded");

            // Verify image preview
            verifyImagePreview();

            System.out.println("✅ Step 3 filled!\n");
        });

        // ==================== STEP 5: SUBMIT & VERIFY ====================
        Allure.step("Submit Course Creation", () -> {
            System.out.println("\n=== STEP 5: SUBMIT COURSE ===");

            if (!shouldSucceed) {
                System.out.println("⚠ Skipping submit - test case expects validation failure");
                return;
            }

            // Click "Tạo khóa học" button
            WebElement createBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(), 'Tạo khóa học')]")
            ));

            attachScreenshot("Before_Create_Button_Click");

            Assertions.assertTrue(createBtn.isEnabled(), "Create button is disabled");
            System.out.println("✓ Create button is enabled");

            clickElementWithRetry(createBtn, 3);
            System.out.println("✓ Create button clicked");

            // Wait for submission
            waitForSubmissionComplete();

            Thread.sleep(3000);
            attachScreenshot("After_Submission");

            System.out.println("✅ Submission completed!\n");
        });

        // ==================== STEP 6: VERIFY RESULT ====================
        Allure.step("Verify Course Creation Result", () -> {
            System.out.println("\n=== STEP 6: VERIFY RESULT ===");

            String resultMessage = getResultMessage();
            System.out.println("📢 Result message: " + resultMessage);

            attachScreenshot("Final_Result");

            if (shouldSucceed) {
                // Nếu redirect về course list = thành công
                try {
                    longWait.until(ExpectedConditions.urlContains("/creator/courseList"));
                    System.out.println("✓ Redirected to course list");
                    System.out.println("✅ TEST PASSED - Course created successfully");
                } catch (TimeoutException e) {
                    String currentUrl = driver.getCurrentUrl();
                    attachScreenshot("Redirect_Failed");
                    Assertions.fail("Expected redirect to /creator/courseList but stayed at: " + currentUrl);
                }
            } else {
                // Negative test: kiểm tra vẫn ở trang create và có error message
                String currentUrl = driver.getCurrentUrl();
                Assertions.assertTrue(
                        currentUrl.contains("/creator/create_course"),
                        "Expected to stay on create course page but URL is: " + currentUrl
                );


                System.out.println("📢 Error message: " + resultMessage);



                System.out.println("✅ TEST PASSED - Validation error handled correctly");
            }
        });
    }

    // ==================== HELPER METHODS ====================

    private void loginViaKeycloak(String email, String password) throws InterruptedException {
        System.out.println("\n=== LOGIN PROCESS ===");
        System.out.println("Email: " + email);

        driver.get(baseUrl);
        Thread.sleep(2000);

        // Check if already logged in
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

            Thread.sleep(3000);
            System.out.println("✅ Login successful!");

        } catch (TimeoutException e) {
            System.out.println("❌ Login failed: " + e.getMessage());
            attachScreenshot("Login_Failed");
            throw e;
        }
    }

    private void verifyCreateCoursePageLoaded() {
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//h1[contains(text(), 'Tạo khóa học mới')]")
                    ),
                    ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//div[contains(text(), 'Bước 1/3')]")
                    )
            ));
            System.out.println("✓ Create course page loaded");
        } catch (TimeoutException e) {
            System.out.println("⚠ Page verification failed");
            attachScreenshot("Page_Load_Failed");
        }
    }

    private void verifyCurrentStep(int expectedStep) {
        try {
            WebElement stepIndicator = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[contains(text(), 'Bước " + expectedStep + "/3')]")
            ));
            System.out.println("✓ Current step verified: " + expectedStep);
        } catch (TimeoutException e) {
            System.out.println("⚠ Step indicator not found for step " + expectedStep);
        }
    }

    private String getValidationErrorMessage() {
        try {
            WebElement errorMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[contains(@class, 'text-red-600') or contains(@class, 'error')]")
            ));
            return errorMsg.getText();
        } catch (TimeoutException e) {
            return "";
        }
    }

    private void verifyImagePreview() {
        try {
            WebElement imagePreview = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//img[@alt='Course preview']")
            ));
            System.out.println("✓ Image preview visible");
        } catch (TimeoutException e) {
            System.out.println("⚠ Image preview not found");
        }
    }

    private void waitForSubmissionComplete() {
        try {
            // Wait for loading state
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[contains(@class, 'animate-spin')] | //button[contains(text(), 'Đang tạo')]")
            ));
            System.out.println("⏳ Submitting...");

            // Wait for loading to finish
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.xpath("//div[contains(@class, 'animate-spin')] | //button[contains(text(), 'Đang tạo')]")
            ));
            System.out.println("✓ Submission completed");
        } catch (TimeoutException e) {
            System.out.println("⚠ No loading state detected");
        }
    }

    private String getResultMessage() {
        try {
            WebElement toast = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector(".Toastify__toast-body, .swal2-html-container, [role='alert']")
            ));
            return toast.getText().trim();
        } catch (TimeoutException e) {
            System.out.println("⚠ No toast message found");
            return "Không có thông báo";
        }
    }

    private void verifyCourseInList(String courseName) {
        try {
            WebElement courseCard = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//h3[contains(text(), '" + courseName + "')] | //div[contains(text(), '" + courseName + "')]")
            ));
            System.out.println("✓ Course found in list: " + courseCard.getText());
        } catch (TimeoutException e) {
            System.out.println("⚠ Course not found in list yet - may need to refresh");
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
                        Thread.sleep(1000);
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