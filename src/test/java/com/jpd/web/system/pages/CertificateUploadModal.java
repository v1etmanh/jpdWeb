package com.jpd.web.system.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;

public class CertificateUploadModal extends BasePage {

    private final By modalTitle = By.xpath("//h2[contains(text(),'Tải lên chứng chỉ')]");
    private final By fileInput = By.xpath("//input[@type='file' and @accept='.pdf,.jpg,.jpeg,.png']");
    private final By uploadBtn = By.xpath("//button[@type='submit' and (contains(text(),'Tải lên') or contains(text(),'Upload'))]");

    private WebDriverWait longWait;

    public CertificateUploadModal(WebDriver driver) {
        super(driver);
        this.longWait = new WebDriverWait(driver, Duration.ofSeconds(60));

        // Verify modal opened
        try {
            waitVisible(modalTitle);
            System.out.println("✓ Modal opened successfully");
        } catch (Exception e) {
            System.out.println("❌ Modal did not open");
            throw new RuntimeException("Certificate upload modal not found", e);
        }
    }

    public void upload(String fileName) {
        System.out.println("\n=== UPLOADING FILE: " + fileName + " ===");

        String path = new File("src/test/resources/certificates/" + fileName).getAbsolutePath();
        File file = new File(path);

        if (!file.exists()) {
            throw new RuntimeException("File not found: " + path);
        }

        System.out.println("✓ File exists: " + path);

        // Make file input visible
        WebElement input = driver.findElement(fileInput);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].style.display='block';" +
                        "arguments[0].style.visibility='visible';" +
                        "arguments[0].style.opacity='1';" +
                        "arguments[0].style.position='fixed';" +
                        "arguments[0].style.zIndex='99999';",
                input
        );

        // Upload file
        input.sendKeys(path);
        System.out.println("✓ File selected");

        // Wait for preview (optional - không bắt buộc)
        try {
            Thread.sleep(1500);
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//p[contains(text(),'" + fileName + "')]")
            ));
            System.out.println("✓ File preview visible");
        } catch (Exception e) {
            System.out.println("⚠ File preview not found (might be OK for invalid files)");
        }
    }

    public void submit() {
        System.out.println("\n=== SUBMITTING UPLOAD ===");

        try {
            WebElement btn = waitClickable(uploadBtn);
            System.out.println("✓ Upload button found");

            jsClick(btn);
            System.out.println("✓ Upload button clicked");

            // ✅ FIX: Wait for upload complete without StaleElementReferenceException
            waitForUploadComplete();

        } catch (Exception e) {
            System.out.println("❌ Failed to submit: " + e.getMessage());

            // Check if button exists at all
            try {
                driver.findElement(uploadBtn);
                System.out.println("Button exists but not clickable");
            } catch (NoSuchElementException ex) {
                System.out.println("❌ Upload button not found - file might be invalid");
            }

            throw new RuntimeException("Failed to submit upload", e);
        }
    }

    private void waitForUploadComplete() {
        System.out.println("⏳ Waiting for upload to complete...");

        By uploadButtonLocator = By.xpath(
                "//button[@type='submit' and (contains(text(),'Tải lên') or contains(text(),'Upload') or contains(text(),'Đang tải'))]"
        );

        try {
            // Phase 1: Wait for loading state (disabled button)
            boolean loadingDetected = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(driver -> {
                        try {
                            WebElement btn = driver.findElement(uploadButtonLocator);
                            String disabled = btn.getAttribute("disabled");
                            String text = btn.getText();

                            boolean isLoading = "true".equals(disabled) ||
                                    text.contains("Đang tải") ||
                                    text.contains("Loading");

                            if (isLoading) {
                                System.out.println("✓ Loading state detected");
                                return true;
                            }
                            return false;
                        } catch (NoSuchElementException | StaleElementReferenceException e) {
                            return false;
                        }
                    });

            if (loadingDetected) {
                // Phase 2: Wait for loading to complete
                longWait.until(driver -> {
                    try {
                        WebElement btn = driver.findElement(uploadButtonLocator);
                        String disabled = btn.getAttribute("disabled");
                        String text = btn.getText();

                        boolean stillLoading = "true".equals(disabled) ||
                                text.contains("Đang tải") ||
                                text.contains("Loading");

                        if (!stillLoading) {
                            System.out.println("✓ Upload completed");
                            return true;
                        }
                        return false;

                    } catch (NoSuchElementException | StaleElementReferenceException e) {
                        // Button disappeared = modal closed = upload complete
                        System.out.println("✓ Upload completed - button disappeared");
                        return true;
                    }
                });
            } else {
                System.out.println("⚠ No loading state detected - upload might be instant");
            }

        } catch (Exception e) {
            System.out.println("⚠ Timeout waiting for upload: " + e.getMessage());
        }

        try {
            Thread.sleep(2000); // Extra wait
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("✅ Upload process finished");
    }

    public boolean isClosed() {
        try {
            Thread.sleep(2000); // Wait for modal to close
            driver.findElement(modalTitle);
            System.out.println("⚠ Modal still open");
            return false;
        } catch (NoSuchElementException e) {
            System.out.println("✓ Modal closed");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}