package com.jpd.web.UnitTest;
import com.jpd.web.service.utils.RequestAttributeExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.*;

 public class RequestAttributeExtractorTest {

    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = Mockito.mock(HttpServletRequest.class);
    }

    @AfterEach
    void tearDown() {
        request = null;
    }

    @Test
    @DisplayName("Given creatorId=Long When extractCreatorId Then returns same value")
    void extractCreatorId_happyPath() {
        Mockito.when(request.getAttribute("creatorId")).thenReturn(42L);
        Long result = RequestAttributeExtractor.extractCreatorId(request);
        assertThat(result).isEqualTo(42L); // ≈ toEqual
    }

    @Test
    @DisplayName("Given missing creatorId When extractCreatorId Then throws NPE")
    void extractCreatorId_missing_throws() {
        Mockito.when(request.getAttribute("creatorId")).thenReturn(null);
        assertThatThrownBy(() -> RequestAttributeExtractor.extractCreatorId(request))
                .isInstanceOf(NullPointerException.class); // ≈ toThrow
    }

    @Test
    @DisplayName("Given wrong-type creatorId When extractCreatorId Then throws ClassCastException")
    void extractCreatorId_wrongType_throws() {
        Mockito.when(request.getAttribute("creatorId")).thenReturn("42");
        assertThatThrownBy(() -> RequestAttributeExtractor.extractCreatorId(request))
                .isInstanceOf(ClassCastException.class);
    }
}
