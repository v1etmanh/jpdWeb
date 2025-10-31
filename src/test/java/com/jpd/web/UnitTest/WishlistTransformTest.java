package com.jpd.web.UnitTest;


import com.jpd.web.dto.WishlistDto;
import com.jpd.web.model.Course;
import com.jpd.web.model.Wishlist;
import com.jpd.web.transform.WishlistTransform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.NullSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WishlistTransformTest {

    @ParameterizedTest
    @CsvFileSource(resources = "/wishlist-transform-test-data.csv", numLinesToSkip = 1)
    @DisplayName("Should transform Wishlist to WishlistDto with various course data")
    void testTransformToWishlistDto(Long courseId, String courseName, String urlImg, Double price) {
        // Arrange
        Course mockCourse = mock(Course.class);
        when(mockCourse.getCourseId()).thenReturn(courseId);
        when(mockCourse.getName()).thenReturn(courseName);
        when(mockCourse.getUrlImg()).thenReturn(urlImg);
        when(mockCourse.getPrice()).thenReturn(price);

        Wishlist mockWishlist = mock(Wishlist.class);
        when(mockWishlist.getCourse()).thenReturn(mockCourse);

        // Act
        WishlistDto result = WishlistTransform.transformToWishlistDto(mockWishlist);

        // Assert
        assertNotNull(result);
        assertEquals(courseId, result.getCourseId());
        assertEquals(courseName, result.getCourse_name());
        assertEquals(urlImg, result.getCourse_img());
        assertEquals(price, result.getCourse_price());

        verify(mockWishlist, times(1)).getCourse();
        verify(mockCourse, times(1)).getCourseId();
        verify(mockCourse, times(1)).getName();
        verify(mockCourse, times(1)).getUrlImg();
        verify(mockCourse, times(1)).getPrice();
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw NullPointerException when wishlist is null")
    void testTransformToWishlistDto_NullWishlist(Wishlist wishlist) {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            WishlistTransform.transformToWishlistDto(wishlist);
        });
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/wishlist-transform-edge-cases.csv", numLinesToSkip = 1)
    @DisplayName("Should handle edge cases with boundary values")
    void testTransformToWishlistDto_EdgeCases(Long courseId, String courseName, String urlImg, Double price) {
        // Arrange
        Course mockCourse = mock(Course.class);
        when(mockCourse.getCourseId()).thenReturn(courseId);
        when(mockCourse.getName()).thenReturn(courseName);
        when(mockCourse.getUrlImg()).thenReturn(urlImg);
        when(mockCourse.getPrice()).thenReturn(price);

        Wishlist mockWishlist = mock(Wishlist.class);
        when(mockWishlist.getCourse()).thenReturn(mockCourse);

        // Act
        WishlistDto result = WishlistTransform.transformToWishlistDto(mockWishlist);

        // Assert
        assertNotNull(result);
        assertEquals(courseId, result.getCourseId());
        assertEquals(courseName, result.getCourse_name());
        assertEquals(urlImg, result.getCourse_img());
        assertEquals(price, result.getCourse_price());
    }
}