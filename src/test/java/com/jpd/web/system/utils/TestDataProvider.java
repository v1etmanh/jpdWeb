package com.jpd.web.system.utils;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class TestDataProvider {

    /**
     * Provides test data for certificate upload test cases
     * @return Stream of Arguments containing: name, email, password, filename, shouldSucceed, expectedMessage
     */
    public static Stream<Arguments> uploadCases() {
        return Stream.of(
                Arguments.of(
                        "TC01_Valid_PDF",
                        "vaanthanh2005@gmail.com",
                        "123456",  // ✅ REMOVED COMMA
                        "valid_certificate.pdf",
                        true,
                        "uploaded successfully"
                ),
                Arguments.of(
                        "TC02_Valid_JPG",
                        "vaanthanh2005@gmail.com",
                        "123456",  // ✅ REMOVED COMMA
                        "certificate.jpg",
                        true,
                        "uploaded successfully"
                ),
                Arguments.of(
                        "TC03_Too_Large",
                        "vaanthanh2005@gmail.com",
                        "123456",  // ✅ REMOVED COMMA
                        "large_6mb.pdf",
                        false,
                        "File size must not exceed 5MB"
                ),
                Arguments.of(
                        "TC04_Invalid_Format",
                        "vaanthanh2005@gmail.com",
                        "123456",  // ✅ REMOVED COMMA
                        "invalid.txt",
                        false,
                        "Only PDF, JPG, JPEG, PNG"
                )
        );
    }
}