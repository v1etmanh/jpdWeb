package com.jpd.web.system.utils;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class TestDataProvider {
    public static Stream<Arguments> uploadCases() {
        return Stream.of(
                Arguments.of("TC01_Valid_PDF", "leducsucute2005@gmail.com", "Leducsu0342005,", "valid_certificate.pdf", true, "uploaded successfully"),
                Arguments.of("TC02_Valid_JPG", "leducsucute2005@gmail.com", "Leducsu0342005,", "certificate.jpg", true, "uploaded successfully"),
                Arguments.of("TC03_Too_Large", "leducsucute2005@gmail.com", "Leducsu0342005,", "large_6mb.pdf", false, "File size must not exceed 5MB"),
                Arguments.of("TC04_Invalid_Format", "leducsucute2005@gmail.com", "Leducsu0342005,", "invalid.txt", false, "Only PDF, JPG, JPEG, PNG")
        );
    }
}
