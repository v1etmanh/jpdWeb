package com.jpd.web.controller;

import com.jpd.web.controller.common.GlobalExceptionHandler;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.WishlistExistException;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.service.WishlistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WishlistController.class)
@Import(GlobalExceptionHandler.class)
class WishListControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WishlistService wishlistService;

    @MockitoBean
    JwtDecoder jwtDecoder;
@MockitoBean
    CustomerRepository customerRepository;
    static JwtRequestPostProcessor jwtWithEmail(String email) {
        return jwt().jwt(jwt -> jwt.claim("email", email));
    }

    @Nested
    @DisplayName("POST /api/wishlist/{courseId}")
    class AddWishlist {

        @Test
        @DisplayName("TC_HP_01: 201 Created when add wishlist successfully")
        void addWishlist_Success() throws Exception {
            long courseId = 101L;
            String email = "user@example.com";
            doNothing().when(wishlistService).addWishlist(email, courseId);

            mockMvc.perform(post("/api/wishlist/{courseId}", courseId)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));

            verify(wishlistService).addWishlist(email, courseId);
            verifyNoMoreInteractions(wishlistService);
        }

        @Test
        @DisplayName("TC_HP_02: 201 Created with Long.MAX_VALUE")
        void addWishlist_MaxCourseId() throws Exception {
            long courseId = Long.MAX_VALUE;
            String email = "maxid@example.com";
            doNothing().when(wishlistService).addWishlist(email, courseId);

            mockMvc.perform(post("/api/wishlist/{courseId}", String.valueOf(courseId))
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));

            verify(wishlistService).addWishlist(email, courseId);
        }

        @Test
        @DisplayName("TC_EC_01: 400 Bad Request when courseId is non-numeric")
        void addWishlist_CourseIdNotNumber() throws Exception {
            mockMvc.perform(post("/api/wishlist/{courseId}", "abc")
                            .with(jwtWithEmail("foo@bar.com")))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResolvedException()).isInstanceOf(MethodArgumentTypeMismatchException.class));

            verifyNoInteractions(wishlistService);
        }

        @Test
        @DisplayName("TC_EC_02: Missing email claim in JWT")
        void addWishlist_MissingEmailClaim_Null() throws Exception {
            // Email claim missing: jwt.getClaimAsString("email") returns null
            long courseId = 101;
            doNothing().when(wishlistService).addWishlist(null, courseId);

            mockMvc.perform(post("/api/wishlist/{courseId}", courseId)
                            .with(jwt()))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));

            verify(wishlistService).addWishlist(null, courseId);
        }

        @Test
        @DisplayName("TC_EC_03: Accept negative and zero courseId (no validation in controller)")
        void addWishlist_NegativeOrZeroCourseId() throws Exception {
            String email = "negzero@example.com";
            doNothing().when(wishlistService).addWishlist(email, -1L);

            mockMvc.perform(post("/api/wishlist/{courseId}", -1L)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));

            verify(wishlistService).addWishlist(email, -1L);

            doNothing().when(wishlistService).addWishlist(email, 0L);

            mockMvc.perform(post("/api/wishlist/{courseId}", 0L)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));

            verify(wishlistService).addWishlist(email, 0L);
        }

        // Commented: Edge TC_EC_04 (@Positive for courseId) – Only relevant if validation enabled

        @Test
        @DisplayName("TC_ES_01: 404 when CourseNotFoundException from service")
        void addWishlist_CourseNotFound() throws Exception {
            long courseId = 101;
            String email = "foo@example.com";
            doThrow(new CourseNotFoundException(courseId))
                    .when(wishlistService).addWishlist(email, courseId);

            mockMvc.perform(post("/api/wishlist/{courseId}", courseId)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message", containsString(String.valueOf(courseId))))
                    .andExpect(jsonPath("$.userMessage").value("Tài nguyên không được tìm thấy"))
                    .andExpect(jsonPath("$.path").value("/api/wishlist/" + courseId))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.traceId").exists());

            verify(wishlistService).addWishlist(email, courseId);
        }

        @Test
        @DisplayName("TC_ES_02: 409 when WishlistExistException from service")
        void addWishlist_WishlistExist() throws Exception {
            long courseId = 123;
            String email = "dup@example.com";
            doThrow(new WishlistExistException("already exists"))
                    .when(wishlistService).addWishlist(email, courseId);

            mockMvc.perform(post("/api/wishlist/{courseId}", courseId)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("WISHLIST_ALREADY_EXISTS"))
                    .andExpect(jsonPath("$.message", containsString("already exists")))
                    .andExpect(jsonPath("$.userMessage").value("Dữ liệu đã tồn tại hoặc bị trùng lặp"))
                    .andExpect(jsonPath("$.path").value("/api/wishlist/123"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.traceId").exists());

            verify(wishlistService).addWishlist(email, courseId);
        }

        @Test
        @DisplayName("TC_ES_03: 500 when NoSuchElementException from service")
        void addWishlist_NoSuchElement() throws Exception {
            long courseId = 111;
            String email = "unknown@example.com";
            doThrow(new NoSuchElementException("Customer not found"))
                    .when(wishlistService).addWishlist(email, courseId);

            mockMvc.perform(post("/api/wishlist/{courseId}", courseId)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message", containsString("Customer not found")))
                    .andExpect(jsonPath("$.userMessage").exists())
                    .andExpect(jsonPath("$.path").value("/api/wishlist/" + courseId))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.traceId").exists());

            verify(wishlistService).addWishlist(email, courseId);
        }

        @Test
        @DisplayName("TC_ES_04: 500 when RuntimeException from service")
        void addWishlist_RuntimeError() throws Exception {
            long courseId = 102;
            String email = "fail@example.com";
            doThrow(new RuntimeException("DB down"))
                    .when(wishlistService).addWishlist(email, courseId);

            mockMvc.perform(post("/api/wishlist/{courseId}", courseId)
                            .with(jwtWithEmail(email)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message", containsString("DB down")))
                    .andExpect(jsonPath("$.userMessage").exists())
                    .andExpect(jsonPath("$.path").value("/api/wishlist/102"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.traceId").exists());

            verify(wishlistService).addWishlist(email, courseId);
        }

        @Test
        @DisplayName("TC_ES_05: 403 when missing JWT")
        void addWishlist_MissingAuthentication() throws Exception {
            mockMvc.perform(post("/api/wishlist/{courseId}", 101L))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(wishlistService);
        }

        // TC_ES_06: 403 Forbidden on missing authority/scope (if present)
        // Add this test if there is scope/authority restriction in config,
        // Example:
        // @Test
        // void addWishlist_ForbiddenWithoutScope() throws Exception {
        //      mockMvc.perform(post("/api/wishlist/{courseId}", 123L)
        //              .with(jwt()))
        //              .andExpect(status().isForbidden());
        // }

    }
}
