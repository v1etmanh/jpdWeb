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
import org.openqa.selenium.support.ui.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;
import java.time.Duration;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("JPD Web - Creator Portal")
@Feature("Course Management")
@Story("BR-19: Creator creates a course")
@Owner("QA Team")
public class CreateCourseSystemTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private WebDriverWait longWait;
    private final String baseUrl = System.getProperty("selenium.baseUrl", "http://localhost:3000");

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
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        longWait = new WebDriverWait(driver, Duration.ofSeconds(60));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        System.out.println("\n=== NEW TEST SESSION STARTED (CreateCourse) ===\n");
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
        TC01_Create_Public     | tanvinhza@gmail.com | 123 | Công khai (Miễn phí) | Khóa học Selenium E2E | true  | Khóa học đã được tạo thành công!
        TC02_Missing_Name      | tanvinhza@gmail.com | 123 | Công khai (Miễn phí) |                         | false | Tên khóa học là bắt buộc
        """)
    @Severity(SeverityLevel.CRITICAL)
    void testCreateCourse(
            String testCase,
            String email,
            String password,
            String courseType,
            String courseName,
            boolean shouldSucceed,
            String expectedMessage
    ) throws Exception {

        String imagePath = Paths.get("src", "test", "resources", "test-course.png").toAbsolutePath().toString();

        // ===== STEP 1: LOGIN + OPEN SIDEBAR (AUTO ZOOM 90%) =====
        Allure.step("Login + Open Creator Sidebar (zoom 90%)", () -> {
            loginViaKeycloak(email, password);

            // Wait React ready
            longWait.until(d -> {
                try {
                    Object isReady = ((JavascriptExecutor) d).executeScript(
                            "return window.ReactAppReady === true || document.getElementById('root')?.children?.length > 0;"
                    );
                    return Boolean.TRUE.equals(isReady);
                } catch (Exception e) {
                    return false;
                }
            });
            attachScreenshot("After_Login");

            openSidebarMenu(); // auto setZoom90% bên trong
            attachScreenshot("Sidebar_Open");
        });

        // ===== STEP 2: MANAGER =====
        Allure.step("Click 'Manager' in Sidebar", () -> {
            WebElement managerLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[@href='/creator/courseList' and normalize-space()='Manager']")
            ));
            clickWithRetry(managerLink, 3);

            longWait.until(ExpectedConditions.urlContains("/creator/courseList"));
            attachScreenshot("Manager_Page_Loaded");
        });

        // ===== STEP 2.5: FROM MANAGER TO CREATE COURSE =====
        Allure.step("From Manager, go to 'Create Course'", () -> {
            goToCreateCourseFromManager();
            longWait.until(ExpectedConditions.urlContains("/creator/create_course"));
            attachScreenshot("CreateCourse_Page_Loaded");
        });

        // ===== STEP 3: FILL STEPS & SUBMIT =====
        Allure.step("Fill steps & Submit", () -> {
            // Step 1
            if (courseName != null && !courseName.trim().isEmpty()) {
                typeAfterLabel("Tên khóa học", courseName.trim());
            }
            // Nếu mô tả là textarea chuẩn => dùng typeTextAreaAfterLabel; nếu là editor contenteditable => dùng typeContentEditableAfterLabel
            if (!typeTextAreaAfterLabel("Mô tả khóa học",
                    "Khóa học thực hành Selenium 4 + JUnit 5, theo mô hình POM. " +
                            "Hướng dẫn chờ thông minh với WebDriverWait, POM components, và best practices.")) {
                typeContentEditableAfterLabel("Mô tả khóa học",
                        "Khóa học thực hành Selenium 4 + JUnit 5, theo mô hình POM. " +
                                "Hướng dẫn chờ thông minh với WebDriverWait, POM components, và best practices.");
            }

            selectOptionAfterLabel("Ngôn ngữ", "Tiếng Việt");
            selectOptionAfterLabel("Ngôn ngữ giảng dạy", "Tiếng Việt");
            clickNext();

            // Step 2
            typeAfterLabel("Đối tượng học viên", "Người mới học test automation");
            typeAfterLabel("Yêu cầu tiên quyết", "Biết Java căn bản, HTML/CSS");
            typeAfterLabel("Mục tiêu học tập", "Viết được E2E test ổn định với waits thông minh");
            clickNext();

            // Step 3
            selectOptionAfterLabel("Loại khóa học", courseType);
            uploadImage(imagePath);
            clickSubmit();
        });

        // ===== STEP 4: VERIFY =====
        Allure.step("Verify result (toast text or redirect)", () -> {
            boolean ok = waitForSuccessOrRedirect();
            attachScreenshot("After_Submit");

            if (shouldSucceed) {
                Assertions.assertTrue(ok, "Kỳ vọng thành công nhưng không có toast/redirect!");
                String toast = tryGetToastText();
                System.out.println("Toast: " + toast);
                Assertions.assertTrue(
                        toast.toLowerCase().contains("thành công") ||
                                toast.toLowerCase().contains("success") ||
                                toast.toLowerCase().contains(expectedMessage.toLowerCase()),
                        "Không thấy thông báo thành công mong đợi. Thấy: " + toast
                );
            } else {
                String errorText = getAnyErrorText();
                System.out.println("Error/Validation: " + errorText);
                Assertions.assertTrue(
                        driver.getCurrentUrl().contains("/creator/create_course") ||
                                !tryGetToastText().toLowerCase().contains("thành công"),
                        "Kỳ vọng fail nhưng lại thành công"
                );
            }
        });
    }

    // ==================== FLOW HELPERS ====================

    /** Ưu tiên CTA trong trang Manager; fallback: mở Sidebar (auto zoom 90%) → click link; fallback cuối: navigate trực tiếp. */
    private void goToCreateCourseFromManager() {
        // Try 1: CTA/link trong content
        try {
            WebElement cta = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("(//main|//div[contains(@class,'container')])[1]//a[@href='/creator/create_course' or .//span[normalize-space()='Create Your Course'] or contains(normalize-space(),'Create') or contains(normalize-space(),'Tạo khóa học')]")
            ));
            scrollIntoView(cta);
            clickWithRetry(cta, 2);
            return;
        } catch (Exception ignored) {}

        // Try 2: Button
        try {
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("(//main|//div[contains(@class,'container')])[1]//button[contains(.,'Create') or contains(.,'Tạo khóa học')]")
            ));
            scrollIntoView(btn);
            clickWithRetry(btn, 2);
            return;
        } catch (Exception ignored) {}

        // Try 3: Sidebar
        try {
            openSidebarMenu(); // auto zoom 90%
            WebElement link = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[@href='/creator/create_course' and normalize-space()='Create Your Course']")
            ));
            clickWithRetry(link, 2);
            return;
        } catch (Exception ignored) {}

        // Try 4: Direct
        driver.navigate().to(baseUrl + "/creator/create_course");
    }

    /** Luôn scale 90% trước khi mở Sidebar. */
    private void openSidebarMenu() {
        setZoom90();
        WebElement toggleBtn = findSidebarToggle();
        Assertions.assertNotNull(toggleBtn, "Không tìm thấy nút mở Creator Sidebar");
        scrollIntoView(toggleBtn);
        clickWithRetry(toggleBtn, 3);
    }

    // ==================== LOW-LEVEL HELPERS (NHẬP DỮ LIỆU GIỐNG LỚP MẪU) ====================

    /** Zoom 90% như bạn yêu cầu để tránh che khuất. */
    private void setZoom90() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "document.body.style.zoom='90%';" +
                            "document.documentElement.style.zoom='90%';"
            );
            Thread.sleep(150);
        } catch (Exception ignored) {}
    }

    private void loginViaKeycloak(String email, String password) throws InterruptedException {
        System.out.println("\n=== LOGIN PROCESS ===");
        driver.get(baseUrl);
        Thread.sleep(1200);

        String existingToken = (String) ((JavascriptExecutor) driver).executeScript("return localStorage.getItem('kc_token');");
        if (existingToken != null && !existingToken.isEmpty()) {
            System.out.println("✓ Already logged in");
            return;
        }

        try {
            WebElement loginLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[normalize-space()='Login' or contains(@href, 'login')]")
            ));
            clickWithRetry(loginLink, 3);

            longWait.until(ExpectedConditions.urlContains("localhost:8080"));

            WebElement username = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
            username.clear(); username.sendKeys(email);

            WebElement pwd = driver.findElement(By.id("password"));
            pwd.clear(); pwd.sendKeys(password);

            WebElement signInBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[name='login'], #kc-login, button[type='submit']")));
            clickWithRetry(signInBtn, 3);

            longWait.until(d -> {
                String url = driver.getCurrentUrl();
                String token = (String) ((JavascriptExecutor) driver).executeScript("return localStorage.getItem('kc_token');");
                return url.contains(baseUrl) && token != null && !token.isEmpty();
            });

            Thread.sleep(1500);
        } catch (TimeoutException e) {
            attachScreenshot("Login_Failed");
            debugCurrentState();
            throw e;
        }
    }

    private WebElement findSidebarToggle() {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button[style*='position: fixed'][style*='border-radius: 50%'][style*='width: 40px'][style*='height: 40px']")));
        } catch (TimeoutException e) {
            attachScreenshot("Sidebar_Toggle_Not_Found");
            return null;
        }
    }

    // ======= INPUT STRATEGIES =======

    private void typeAfterLabel(String labelText, String value) {
        WebElement field = findFieldByLabel(labelText);
        Assertions.assertNotNull(field, "Không tìm thấy input cho label: " + labelText);
        typeSmartReact(field, value);
    }

    private boolean typeTextAreaAfterLabel(String labelText, String value) {
        String lx = labelXpath(labelText);
        try {
            By taBy = By.xpath(lx + "/following::textarea[1]");
            WebElement ta = wait.until(ExpectedConditions.visibilityOfElementLocated(taBy));
            scrollIntoView(ta);
            typeSmartReact(ta, value);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }


    /** Với editor contenteditable (Quill/Tiptap/MUI editor) */
    private void typeContentEditableAfterLabel(String labelText, String value) {
        WebElement editable = null;
        By[] candidates = new By[] {
                By.xpath("//label[normalize-space()='" + labelText + "']/following::*[@contenteditable='true'][1]"),
                By.xpath("//*[contains(@class,'ql-editor') or contains(@class,'ProseMirror')]")
        };
        for (By by : candidates) {
            try {
                editable = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
                break;
            } catch (TimeoutException ignored) {}
        }
        Assertions.assertNotNull(editable, "Không tìm thấy vùng soạn thảo cho: " + labelText);

        scrollIntoView(editable);
        try {
            editable.click();
        } catch (Exception ignored) {}

        ((JavascriptExecutor) driver).executeScript(
                "const el = arguments[0];" +
                        "el.innerHTML = '';" +
                        "el.textContent = arguments[1];" +
                        "el.dispatchEvent(new InputEvent('input', {bubbles:true}));" +
                        "el.dispatchEvent(new Event('change', {bubbles:true}));",
                editable, value
        );
    }

    /** Tìm field bằng nhiều chiến lược giống cách bạn làm ở 2 test classes trước. */
    private WebElement findFieldByLabel(String labelText) {
        String lx = labelXpath(labelText);
        By[] tries = new By[] {
                // 1) label ngay trước input/textarea
                By.xpath(lx + "/following::*[self::input or self::textarea][1]"),
                // 2) label[@for] → #id
                By.xpath(lx + "[@for]/following::input[@id=(" + lx + ")/@for][1]"),
                // 3) placeholder (phòng khi mapping khác)
                By.xpath("//input[@placeholder='" + labelText + "']"),
                By.xpath("//textarea[@placeholder='" + labelText + "']"),
                // 4) aria-label
                By.xpath("//input[@aria-label='" + labelText + "']"),
                By.xpath("//textarea[@aria-label='" + labelText + "']"),
                // 5) aria-labelledby
                By.xpath("//*[(self::input or self::textarea) and @aria-labelledby=(" + lx + ")/@id]"),
        };
        for (By by : tries) {
            try {
                WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
                return el;
            } catch (TimeoutException ignored) {}
        }
        attachScreenshot("Field_Not_Found_" + labelText);
        return null;
    }


    /** Nhập liệu bền vững trên React controlled input: sendKeys → JS set value + dispatch input/change. */
    private void typeSmartReact(WebElement el, String value) {
        scrollIntoView(el);
        wait.until(ExpectedConditions.visibilityOf(el));

        // Nếu bị readonly/disabled thì cố bỏ qua bằng JS
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].removeAttribute('readonly'); arguments[0].removeAttribute('disabled');", el);
        } catch (Exception ignored) {}

        try { el.clear(); } catch (Exception ignored) {}
        try {
            el.click();
        } catch (Exception ignored) {}

        try {
            el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            el.sendKeys(value);
            // Kiểm tra có nhận chưa
            String current = el.getAttribute("value");
            if (current != null && !current.isEmpty()) return;
        } catch (Exception ignored) {}

        // Fallback: JS set value + dispatch input/change cho React
        ((JavascriptExecutor) driver).executeScript(
                "const el=arguments[0], val=arguments[1];" +
                        "const proto = Object.getPrototypeOf(el);" +
                        "const desc = Object.getOwnPropertyDescriptor(proto, 'value');" +
                        "if (desc && desc.set) { desc.set.call(el, val); } else { el.value = val; }" +
                        "el.dispatchEvent(new Event('input', {bubbles:true}));" +
                        "el.dispatchEvent(new Event('change', {bubbles:true}));",
                el, value
        );
    }

    /** Select option sau label: thử <select>, nếu không có thì thử role=combobox (React Select/MUI) */
    /** Chọn option cho cả <select> thường và combobox, dùng matcher label bỏ '*' */
    private void selectOptionAfterLabel(String labelText, String optionText) {
        String lx = labelXpath(labelText);

        // 1) Native <select>
        try {
            By selectBy = By.xpath(lx + "/following::select[1]");
            WebElement selectEl = wait.until(ExpectedConditions.elementToBeClickable(selectBy));
            scrollIntoView(selectEl);
            selectEl.click();

            By optionBy = By.xpath(lx + "/following::select[1]/option[normalize-space()='" + optionText + "']");
            WebElement optionEl = wait.until(ExpectedConditions.elementToBeClickable(optionBy));
            optionEl.click();
            return;
        } catch (TimeoutException ignored) {}

        // 2) Combobox custom
        try {
            By triggerBy = By.xpath(lx + "/following::*[@role='combobox' or self::div or self::button][1]");
            WebElement trigger = wait.until(ExpectedConditions.elementToBeClickable(triggerBy));
            scrollIntoView(trigger);
            clickWithRetry(trigger, 2);

            try {
                WebElement comboInput = trigger.findElement(By.xpath(".//input"));
                comboInput.sendKeys(optionText);
                Thread.sleep(50);
            } catch (Exception ignored) {}

            By option = By.xpath("//div[@role='listbox']//*[@role='option' and normalize-space()='" + optionText + "'] | //div//*[normalize-space()='" + optionText + "']");
            WebElement item = wait.until(ExpectedConditions.elementToBeClickable(option));
            item.click();
            return;
        } catch (TimeoutException ignored) {}

        // 3) Fallback generic
        try {
            By dd = By.xpath(lx + "/following::*[self::div or self::button][1]");
            WebElement t = wait.until(ExpectedConditions.elementToBeClickable(dd));
            t.click();
            WebElement item = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[normalize-space()='" + optionText + "']")));
            item.click();
        } catch (TimeoutException e) {
            attachScreenshot("Select_Failed_" + labelText);
            throw e;
        }
    }

    private void clickNext() {
        WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[.//span[normalize-space()='Tiếp theo'] or normalize-space()='Tiếp theo']")));
        clickWithRetry(nextBtn, 2);
    }

    private void clickSubmit() {
        WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(normalize-space(),'Tạo khóa học')]")));
        clickWithRetry(submitBtn, 2);
    }

    private void uploadImage(String path) {
        By fileInputBy = By.cssSelector("input[type='file'][accept^='image/']");
        WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(fileInputBy));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].style.display='block';" +
                        "arguments[0].style.visibility='visible';" +
                        "arguments[0].style.opacity='1';" +
                        "arguments[0].style.position='fixed';" +
                        "arguments[0].style.zIndex='999999';", fileInput);
        fileInput.sendKeys(path);
        sleep(50);
        attachScreenshot("Image_Selected");
    }

    private boolean waitForSuccessOrRedirect() {
        try {
            return longWait.until(d -> {
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

    private String tryGetToastText() {
        try {
            WebElement toast = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector(".Toastify__toast-body, .swal2-html-container, [role='alert'], .toast-body, .notification")));
            return toast.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String getAnyErrorText() {
        try {
            WebElement err = driver.findElement(
                    By.xpath("//*[contains(@class, 'error') or contains(@class, 'text-red') or contains(text(),'bắt buộc') or contains(text(),'không hợp lệ')]"));
            return err.getText();
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    // ---------- utils ----------
    private void clickWithRetry(WebElement el, int max) {
        for (int i = 1; i <= max; i++) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(el));
                el.click();
                return;
            } catch (Exception e) {
                try {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                    return;
                } catch (Exception ignore) {
                    sleep(200);
                }
            }
        }
        throw new RuntimeException("Failed to click element after " + max + " attempts");
    }

    private void scrollIntoView(WebElement el) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        } catch (Exception ignored) {}
    }
    private String labelXpath(String labelText) {
        // ví dụ: //label[normalize-space(translate(., '*', ''))='Ngôn ngữ']
        return "//label[normalize-space(translate(., '*', ''))='" + labelText + "']";
    }

    private void debugCurrentState() {
        try {
            System.out.println("URL: " + driver.getCurrentUrl() + " | Title: " + driver.getTitle());
            LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
            int count = 0;
            for (LogEntry entry : logs) {
                if (entry.getLevel().toString().contains("SEVERE") && count++ < 3) {
                    System.out.println("Console: " + entry.getMessage());
                }
            }
        } catch (Exception ignored) {}
    }

    private void attachScreenshot(String name) {
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(png), ".png");
        } catch (Exception ignored) {}
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
