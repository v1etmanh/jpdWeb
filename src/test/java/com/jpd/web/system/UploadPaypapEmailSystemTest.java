package com.jpd.web.system;

import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.ByteArrayInputStream;
import java.time.Duration;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("JPD Web - Creator Portal")
@Feature("Payment Settings")
@Story("BR-19: Creator sets PayPal email")
@Owner("QA Team")
public class UploadPaypapEmailSystemTest {

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
        options.addArguments("--remote-debugging-port=9223");
        options.addArguments("--incognito");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        longWait = new WebDriverWait(driver, Duration.ofSeconds(60));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            	TC01_Valid_Email       | thi@gmail.com | 123456 | creator | qa.paypal@test.com | true  | updated successfully
            	TC02_Invalid_Format    | thi@gmail.com | 123456 | creator | not-an-email       | false | invalid email
            	TC03_Empty_Value       | thi@gmail.com | 123456 | creator |                    | false | required
            """)
    @Severity(SeverityLevel.CRITICAL)
    void testUploadPaypalEmail(
            String testCase,
            String email,
            String password,
            String role,
            String paypalEmail,
            boolean shouldSucceed,
            String expectedMessage) throws Exception {

        Allure.step("Login + Navigate to Payment Settings", () -> {
            loginViaKeycloak(email, password);

            // Đợi token xuất hiện
            longWait.until(d -> {
                Object token = ((JavascriptExecutor) driver).executeScript(
                        "return localStorage.getItem('kc_token');");
                return token != null && !token.toString().isEmpty();
            });

            // Đợi ứng dụng React sẵn sàng
            longWait.until(d -> {
                try {
                    Object isReady = ((JavascriptExecutor) driver).executeScript(
                            "return window.ReactAppReady === true || document.getElementById('root').children.length > 0;");
                    return Boolean.TRUE.equals(isReady);
                } catch (Exception e) {
                    return false;
                }
            });

            attachScreenshot("After_Login");

            // Điều hướng tới trang cài đặt thanh toán/creator profile (linh hoạt)
            driver.navigate().to(baseUrl + "/creator/profile");
            longWait.until(ExpectedConditions.or(
                    ExpectedConditions.urlContains("/creator/profile"),
                    ExpectedConditions.urlContains("/creator/settings"),
                    ExpectedConditions.urlContains("/settings")));

            attachScreenshot("On_Profile");
        });

        Allure.step("Open PayPal Email section", () -> {
            // Cuộn tới đúng card 'Thanh toán'
            WebElement paymentCard = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[.//text()[contains(., 'Thanh toán')]]" +
                            "[.//text()[contains(., 'Thiết lập phương thức thanh toán')]]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", paymentCard);

            // Click nút 'Thiết lập' hoặc 'Chỉnh sửa' trong card này
            WebElement setupBtn = paymentCard.findElement(
                    By.xpath(
                            ".//button[normalize-space()='Thiết lập' or contains(., 'Thiết lập') or contains(., 'Chỉnh sửa')]"));
            clickElementWithRetry(setupBtn, 3);

            // Chờ modal 'Thiết lập PayPal' xuất hiện
            longWait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//h2[contains(., 'Thiết lập PayPal')]")));

            attachScreenshot("Paypal_Section_Open");
        });

        Allure.step("Enter PayPal email and save", () -> {
            // Xác định modal 'Thiết lập PayPal'
            WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//h2[contains(., 'Thiết lập PayPal')]/ancestor::div[@role='dialog']")));

            // Tìm input trong modal theo label/loại
            WebElement emailInput = null;
            By[] candidates = new By[] {
                    By.cssSelector("div[role='dialog'] input[type='email']"),
                    By.xpath("//div[@role='dialog']//label[contains(., 'Email PayPal')]/following::input[1]"),
                    By.xpath("//h2[contains(., 'Thiết lập PayPal')]/ancestor::div[@role='dialog']//input")
            };
            for (By by : candidates) {
                try {
                    emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
                    break;
                } catch (TimeoutException ignored) {
                }
            }
            Assertions.assertNotNull(emailInput, "PayPal email input not found");

            emailInput.clear();
            if (paypalEmail != null && !paypalEmail.isEmpty()) {
                emailInput.sendKeys(paypalEmail);
            }

            attachScreenshot("Paypal_Email_Entered");

            // Click 'Lưu email' trong modal
            WebElement saveBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath(
                            "//div[@role='dialog']//button[normalize-space()='Lưu email' or contains(., 'Lưu email') or @type='submit']")));
            clickElementWithRetry(saveBtn, 3);

            // Chờ dấu hiệu hoàn tất: banner thành công, thông báo lỗi hoặc toast
            try {
                longWait.until(ExpectedConditions.or(
                        ExpectedConditions.visibilityOfElementLocated(By.xpath(
                                "//div[@role='dialog']//*[contains(., 'Email PayPal đã được cập nhật thành công')]")),
                        ExpectedConditions.visibilityOfElementLocated(By.xpath(
                                "//div[@role='dialog']//*[contains(., 'Có lỗi xảy ra')]")),
                        ExpectedConditions.visibilityOfElementLocated(
                                By.cssSelector(".Toastify__toast-body, [role='alert'], .swal2-html-container"))));
            } catch (TimeoutException ignored) {
            }
        });

        Allure.step("Verify result", () -> {
            String result = getToastOrInlineMessage();
            attachScreenshot("Result");

            if (shouldSucceed) {
                boolean ok = containsAnyIgnoreCase(result, "success", "thành công", "updated", expectedMessage);
                Assertions.assertTrue(ok, "Expected success but got: " + result);
            } else {
                boolean isError = containsAnyIgnoreCase(result, "error", "lỗi", "invalid", "required", expectedMessage)
                        || result.isBlank();
                Assertions.assertTrue(isError, "Expected validation error but got: " + result);
            }
        });
    }

    // ==================== HELPERS ====================

    private void loginViaKeycloak(String email, String password) throws InterruptedException {
        driver.get(baseUrl);
        Thread.sleep(1500);

        // Nếu đã có token thì bỏ qua
        String existingToken = (String) ((JavascriptExecutor) driver)
                .executeScript("return localStorage.getItem('kc_token');");
        if (existingToken != null && !existingToken.isEmpty()) {
            return;
        }

        try {
            WebElement loginLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[normalize-space()='Login' or contains(@href, 'login')]")));
            clickElementWithRetry(loginLink, 3);

            longWait.until(ExpectedConditions.urlContains("localhost:8080"));

            WebElement username = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
            username.clear();
            username.sendKeys(email);

            WebElement pwd = driver.findElement(By.id("password"));
            pwd.clear();
            pwd.sendKeys(password);

            WebElement signInBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[name='login'], #kc-login, button[type='submit']")));
            clickElementWithRetry(signInBtn, 3);

            longWait.until(d -> {
                String url = driver.getCurrentUrl();
                String token = (String) ((JavascriptExecutor) driver)
                        .executeScript("return localStorage.getItem('kc_token');");
                return url.contains(baseUrl) && token != null && !token.isEmpty();
            });

            Thread.sleep(1500);
        } catch (TimeoutException e) {
            attachScreenshot("Login_Failed");
            debugCurrentState();
            throw e;
        }
    }

    private WebElement findPaypalSection() {
        // Cố gắng tìm card/section có chữ PayPal / Thanh toán / Payment
        try {
            return wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//*[contains(translate(., 'PAYPALTHANHTOANPAYMENT', 'paypalthanhtoanpayment'), 'paypal') "
                            +
                            "or contains(translate(., 'PAYPALTHANHTOANPAYMENT', 'paypalthanhtoanpayment'), 'thanh toán') "
                            +
                            "or contains(translate(., 'PAYPALTHANHTOANPAYMENT', 'paypalthanhtoanpayment'), 'payment')]")));
        } catch (TimeoutException e) {
            attachScreenshot("Paypal_Section_Not_Found");
            return null;
        }
    }

    private void waitForSaveComplete() {
        // Chờ toast hoặc nút bật/tắt trạng thái
        try {
            longWait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector(
                            ".Toastify__toast-body, [role='alert'], .swal2-html-container, .notification, .toast-body")),
                    ExpectedConditions
                            .invisibilityOfElementLocated(By.xpath("//button[@type='submit' and @disabled]"))));
        } catch (TimeoutException ignored) {
        }
    }

    private String getToastOrInlineMessage() {
        // Toast
        try {
            WebElement toast = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector(
                            ".Toastify__toast-body, [role='alert'], .swal2-html-container, .notification, .toast-body")));
            return toast.getText().trim();
        } catch (TimeoutException ignored) {
        }

        // Inline error near input
        try {
            WebElement inline = driver.findElement(By.xpath(
                    "//input[@type='email' or contains(@id,'paypal')]/ancestor::*[1]//span[contains(@class,'error') or contains(@class,'text-red') or contains(@class,'invalid')]"));
            return inline.getText().trim();
        } catch (NoSuchElementException ignored) {
        }

        // Success banner inside modal
        try {
            WebElement successBanner = driver.findElement(By.xpath(
                    "//div[@role='dialog']//*[contains(., 'Email PayPal đã được cập nhật thành công')]"));
            if (successBanner.isDisplayed()) {
                return successBanner.getText().trim();
            }
        } catch (NoSuchElementException ignored) {
        }

        // Error banner inside modal
        try {
            WebElement errorBanner = driver.findElement(By.xpath(
                    "//div[@role='dialog']//*[contains(., 'Có lỗi xảy ra')]"));
            if (errorBanner.isDisplayed()) {
                return errorBanner.getText().trim();
            }
        } catch (NoSuchElementException ignored) {
        }

        return "";
    }

    private void clickElementWithRetry(WebElement element, int maxAttempts) throws InterruptedException {
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(element));
                element.click();
                return;
            } catch (Exception e) {
                if (i < maxAttempts) {
                    try {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                        return;
                    } catch (Exception jsError) {
                        Thread.sleep(500);
                    }
                } else {
                    throw new RuntimeException("Failed to click after " + maxAttempts + " attempts", e);
                }
            }
        }
    }

    private void debugCurrentState() {
        try {
            String url = driver.getCurrentUrl();
            String title = driver.getTitle();
            LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
            System.out.println("URL: " + url + " | Title: " + title);
            int count = 0;
            for (LogEntry entry : logs) {
                if (entry.getLevel().toString().contains("SEVERE") && count++ < 3) {
                    System.out.println("Console: " + entry.getMessage());
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void attachScreenshot(String name) {
        try {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(screenshot), ".png");
        } catch (Exception ignored) {
        }
    }

    private boolean containsAnyIgnoreCase(String haystack, String... needles) {
        String lower = haystack == null ? "" : haystack.toLowerCase();
        for (String n : needles) {
            if (n != null && lower.contains(n.toLowerCase()))
                return true;
        }
        return false;
    }
}
