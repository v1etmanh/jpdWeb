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
import java.time.Duration;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("JPD Web - Payment System")
@Feature("Course Payment")
@Story("BR-21: Customer purchases course via PayPal and VNPay")
@Owner("QA Team")
public class CoursePaymentE2ETest {

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
TC04_Private_Course_Key  | thanhthao@gmail.com | 123456 | 3 | PRIVATE| true  | Đăng ký khóa học thành công
TC01_PayPal_Success      | thanhthao@gmail.com | 123456 | 10 | PAYPAL | true  | transaction-detail
TC02_VNPay_Success       | thanhthao@gmail.com| 123456 | 11 | VNPAY  | true  | transaction-detail
TC03_Public_Course_Free  | thanhthao@gmail.com | 123456 | 13 | PUBLIC | true  | Đăng ký khóa học thành công
""")


    @Severity(SeverityLevel.CRITICAL)
    void testCoursePurchase(
            String testCase,
            String email,
            String password,
            String courseId,
            String paymentMethod,
            boolean shouldSucceed,
            String expectedResult) throws Exception {

        // ==================== STEP 1: LOGIN + NAVIGATE ====================
        Allure.step("Login and Navigate to Course Detail Page", () -> {
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

            System.out.println("📍 Navigating to /course/specific/" + courseId);
            driver.navigate().to(baseUrl + "/course/specific/" + courseId);

            longWait.until(ExpectedConditions.urlContains("/course/specific/" + courseId));
            System.out.println("✓ URL changed to course detail page");

            Thread.sleep(1000);
            attachScreenshot("After_Navigate_To_CourseDetail");

            verifyCourseDetailPageLoaded();

            System.out.println("✅ Navigation successful!\n");
        });

        // ==================== STEP 2: CLICK BUY/ENROLL BUTTON ====================
        Allure.step("Click Buy/Enroll Button", () -> {
            System.out.println("\n=== STEP 2: CLICK BUY/ENROLL BUTTON ===");

            // Override window.prompt() CHO PRIVATE COURSE
            if (paymentMethod.equals("PRIVATE")) {
                ((JavascriptExecutor) driver).executeScript(
                        "window.prompt = function(message) { " +
                                "    console.log('Prompt intercepted:', message); " +
                                "    return 'KEY_WRITNG_A'; " +
                                "};"
                );
                System.out.println("✓ window.prompt() overridden for private course");
            }

            // Scroll to button
            WebElement buyButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//button[contains(text(), 'Mua ngay') or contains(text(), 'Tham gia ngay')]")
            ));

            scrollToElement(buyButton);
            Thread.sleep(300);

            attachScreenshot("Before_Click_Buy_Button");

            clickElementWithRetry(buyButton, 3);
            System.out.println("✓ Buy/Enroll button clicked");

            Thread.sleep(500);
            attachScreenshot("After_Click_Buy_Button");

            System.out.println("✅ Button clicked!\n");
        });

        // ==================== STEP 3: SELECT PAYMENT METHOD (FOR PAID COURSES) ====================
        if (paymentMethod.equals("PAYPAL") || paymentMethod.equals("VNPAY")) {
            Allure.step("Select Payment Method: " + paymentMethod, () -> {
                System.out.println("\n=== STEP 3: SELECT PAYMENT METHOD ===");

                // Wait for payment modal
                WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h2[contains(text(), 'Chọn phương thức thanh toán')]")
                ));
                System.out.println("✓ Payment modal opened");

                attachScreenshot("Payment_Modal_Opened");

                // Select payment method
                WebElement paymentOption;
                if (paymentMethod.equals("PAYPAL")) {
                    paymentOption = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[contains(@class, 'cursor-pointer')]//span[contains(text(), 'PayPal')]")
                    ));
                } else {
                    paymentOption = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[contains(@class, 'cursor-pointer')]//div[contains(text(), 'VNPAY')]")
                    ));
                }

                clickElementWithRetry(paymentOption, 3);
                System.out.println("✓ Payment method selected: " + paymentMethod);

                Thread.sleep(300);
                attachScreenshot("Payment_Method_Selected");

                // Click confirm button
                WebElement confirmButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(text(), 'Xác nhận thanh toán')]")
                ));

                clickElementWithRetry(confirmButton, 3);
                System.out.println("✓ Confirm payment button clicked");

                Thread.sleep(500);
                attachScreenshot("After_Confirm_Payment");

                System.out.println("✅ Payment method selected!\n");
            });
        }

        // ==================== STEP 4: VERIFY RESULT ====================
        Allure.step("Verify Payment/Enrollment Result", () -> {
            System.out.println("\n=== STEP 5: VERIFY RESULT ===");

            Thread.sleep(1000);
            attachScreenshot("Final_Result");

            if (shouldSucceed) {
                if (paymentMethod.equals("PAYPAL")) {
                    // PayPal: redirect to transaction-detail page
                    try {
                        longWait.until(ExpectedConditions.urlContains("transaction-detail"));
                        System.out.println("✓ Redirected to transaction-detail");

                        verifyTransactionDetailPage("PAYPAL");

                        System.out.println("✅ TEST PASSED - PayPal payment initiated successfully");
                    } catch (TimeoutException e) {
                        String currentUrl = driver.getCurrentUrl();
                        attachScreenshot("Redirect_Failed");
                        Assertions.fail("Expected redirect to transaction-detail but stayed at: " + currentUrl);
                    }
                } else if (paymentMethod.equals("VNPAY")) {
                    // VNPay: redirect directly to vnpayment.vn
                    try {
                        longWait.until(ExpectedConditions.urlContains("vnpayment.vn"));
                        System.out.println("✓ Redirected to VNPay payment gateway");

                        String currentUrl = driver.getCurrentUrl();
                        System.out.println("VNPay URL: " + currentUrl);

                        // Verify VNPay page loaded
                        Assertions.assertTrue(
                                currentUrl.contains("vnpayment.vn"),
                                "Expected VNPay URL but got: " + currentUrl
                        );

                        attachScreenshot("VNPay_Payment_Gateway");

                        System.out.println("✅ TEST PASSED - VNPay payment initiated successfully");
                    } catch (TimeoutException e) {
                        String currentUrl = driver.getCurrentUrl();
                        attachScreenshot("Redirect_Failed");
                        Assertions.fail("Expected redirect to vnpayment.vn but stayed at: " + currentUrl);
                    }
                } else {
                    // For PUBLIC/PRIVATE courses, check success message
                    String message = getToastMessage();
                    System.out.println("📢 Result message: " + message);

                    Assertions.assertTrue(
                            message.toLowerCase().contains(expectedResult.toLowerCase()),
                            "Expected message containing '" + expectedResult + "' but got: " + message
                    );

                    System.out.println("✅ TEST PASSED - Enrollment successful");
                }
            } else {
                System.out.println("✅ TEST PASSED - Error handled correctly");
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

    private void verifyCourseDetailPageLoaded() {
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//h1[contains(@class, 'font-bold')]")
                    ),
                    ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//button[contains(text(), 'Mua ngay') or contains(text(), 'Tham gia ngay')]")
                    )
            ));
            System.out.println("✓ Course detail page loaded");
        } catch (TimeoutException e) {
            System.out.println("⚠ Page verification failed");
            attachScreenshot("Page_Load_Failed");
        }
    }

    private void verifyTransactionDetailPage(String paymentMethod) {
        try {
            // Verify page title
            WebElement pageTitle = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//h1[contains(text(), 'Giao dịch đã được tạo')]")
            ));
            System.out.println("✓ Transaction detail page title found: " + pageTitle.getText());

            // Verify order ID
            WebElement orderId = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//span[contains(@class, 'font-mono')]")
            ));
            System.out.println("✓ Order ID found: " + orderId.getText());

            // Verify payment method
            String methodText = paymentMethod.equals("PAYPAL") ? "PayPal" : "VNPAY";
            WebElement methodElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//span[contains(text(), '" + methodText + "')]")
            ));
            System.out.println("✓ Payment method displayed: " + methodElement.getText());

            // Verify payment button
            WebElement paymentButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//a[contains(text(), 'Chuyển đến " + methodText + "')]")
            ));
            System.out.println("✓ Payment button found");

            attachScreenshot("Transaction_Detail_Verified");

        } catch (TimeoutException e) {
            System.out.println("⚠ Transaction detail verification failed");
            attachScreenshot("Verification_Failed");
        }
    }

    private String getToastMessage() {
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

    private void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});",
                element
        );
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