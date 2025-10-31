package com.jpd.web.service;

import com.jpd.web.dto.WishlistDto;
import com.jpd.web.exception.WishlistExistException;
import com.jpd.web.model.Course;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Wishlist;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.WishlistRepository;
import com.jpd.web.transform.WishlistTransform;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.springframework.dao.DataAccessException;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishListServiceTest {

    @Mock
    WishlistRepository wishlistRepository;
    @Mock
    CustomerRepository customerRepository;
    @Mock
    CourseRepository courseRepository;

    @InjectMocks
    WishlistService wishListService;

    Customer cus;
    Course course;
    Wishlist wishlist, wishlist2;

    @BeforeEach
    void setup() {
        cus = Customer.builder().customerId(1L).email("emma@email.com").build();
        course = Course.builder().courseId(99L).name("Algebra").build();
        wishlist = Wishlist.builder().id(10L).customer(cus).course(course).build();

        // For EC_04, wishlist2 for non-duplicate course
        Course anotherCourse = Course.builder().courseId(98L).name("Geometry").build();
        wishlist2 = Wishlist.builder().id(11L).customer(cus).course(anotherCourse).build();
    }


//    SỬ DỤNG CSV
@Test
@DisplayName("TC_HP_01_addWishlist_happy_path_should_save_successfully")
void addWishlist_happy_path() {
    when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
    when(courseRepository.findById(99L)).thenReturn(Optional.of(course));
    when(wishlistRepository.findByCourse_CourseIdAndCustomer_CustomerId(99L, 1L)).thenReturn(Optional.empty());

    wishListService.addWishlist("emma@email.com", 99L);

    verify(wishlistRepository, times(1)).save(argThat(w ->
            w.getCustomer() == cus && w.getCourse() == course
    ));
    verifyNoMoreInteractions(wishlistRepository);
}

    // TC_HP_02: retrieveYourWishlist happy path with 2 items, test transform/order
    @Test
    @DisplayName("TC_HP_02_retrieveYourWishlist_happy_path_should_return_dto_list_exact_order")
    void retrieveYourWishlist_happy_path() {
        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
        List<Wishlist> wishlists = List.of(wishlist, wishlist2);
        when(wishlistRepository.findByCustomer(cus)).thenReturn(wishlists);

        WishlistDto dto1 = WishlistDto.builder().courseId(99L).course_name("Algebra").build();
        WishlistDto dto2 = WishlistDto.builder().courseId(98L).course_name("Geometry").build();

        try (MockedStatic<WishlistTransform> mockedTransform = mockStatic(WishlistTransform.class)) {
            mockedTransform.when(() -> WishlistTransform.transformToWishlistDto(wishlist)).thenReturn(dto1);
            mockedTransform.when(() -> WishlistTransform.transformToWishlistDto(wishlist2)).thenReturn(dto2);

            List<WishlistDto> result = wishListService.retrieveYourWishlist("emma@email.com");

            assertThat(result).containsExactly(dto1, dto2);
            mockedTransform.verify(() -> WishlistTransform.transformToWishlistDto(wishlist), times(1));
            mockedTransform.verify(() -> WishlistTransform.transformToWishlistDto(wishlist2), times(1));
            mockedTransform.verifyNoMoreInteractions();
        }
    }

    // TC_EC_01: addWishlist with invalid courseId (đọc từ CSV)
    @ParameterizedTest
    @CsvFileSource(resources = "/wishlist_invalid_courseId.csv", numLinesToSkip = 1)
    @DisplayName("TC_EC_01_addWishlist_invalid_courseId_not_found_should_throw_CourseNotFoundException (CSV)")
    void addWishlist_invalidCourseId_shouldThrow(long cid) {
        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
        when(courseRepository.findById(cid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishListService.addWishlist("emma@email.com", cid))
                .isInstanceOf(com.jpd.web.exception.CourseNotFoundException.class)
                .hasMessageContaining(String.valueOf(cid));

        verify(wishlistRepository, never()).save(any());
    }

    // TC_EC_02: retrieveYourWishlist returns empty; no transform called
    @Test
    @DisplayName("TC_EC_02_retrieveYourWishlist_no_wishlist_return_empty_list")
    void retrieveYourWishlist_empty() {
        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
        when(wishlistRepository.findByCustomer(cus)).thenReturn(Collections.emptyList());

        try (MockedStatic<WishlistTransform> mockedTransform = mockStatic(WishlistTransform.class)) {
            List<WishlistDto> result = wishListService.retrieveYourWishlist("emma@email.com");
            assertThat(result).isEmpty();
            mockedTransform.verifyNoInteractions();
        }
    }

    // TC_EC_03: retrieveYourWishlist order preserved
    @Test
    @DisplayName("TC_EC_03_retrieveYourWishlist_should_preserve_order")
    void retrieveYourWishlist_orderPreserved() {
        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
        // Deliberate out-of-natural-order
        List<Wishlist> wishlists = Arrays.asList(wishlist2, wishlist);
        when(wishlistRepository.findByCustomer(cus)).thenReturn(wishlists);

        WishlistDto dto2 = WishlistDto.builder().courseId(98L).course_name("Geometry").build();
        WishlistDto dto1 = WishlistDto.builder().courseId(99L).course_name("Algebra").build();

        try (MockedStatic<WishlistTransform> mockedTransform = mockStatic(WishlistTransform.class)) {
            mockedTransform.when(() -> WishlistTransform.transformToWishlistDto(wishlist2)).thenReturn(dto2);
            mockedTransform.when(() -> WishlistTransform.transformToWishlistDto(wishlist)).thenReturn(dto1);

            List<WishlistDto> result = wishListService.retrieveYourWishlist("emma@email.com");
            assertThat(result).containsExactly(dto2, dto1);
        }
    }

    // TC_EC_04: addWishlist - customer has other wishlists but not this courseId; should save
    @Test
    @DisplayName("TC_EC_04_addWishlist_with_existing_non_matching_wishlists_should_save")
    void addWishlist_existingNonMatchingWishlistsShouldSave() {
        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
        when(courseRepository.findById(99L)).thenReturn(Optional.of(course));
        when(wishlistRepository.findByCourse_CourseIdAndCustomer_CustomerId(99L, 1L)).thenReturn(Optional.empty());

        wishListService.addWishlist("emma@email.com", 99L);

        verify(wishlistRepository, times(1)).save(any());
    }

    // TC_ES_01: addWishlist - customer not found, expect NoSuchElement
    @Test
    @DisplayName("TC_ES_01_addWishlist_customer_not_found_should_throw_NoSuchElementException")
    void addWishlist_customerNotFound_shouldThrow() {
        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishListService.addWishlist("emma@email.com", 99L))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(courseRepository, wishlistRepository);
    }

    // TC_ES_02: addWishlist - course not found
    @Test
    @DisplayName("TC_ES_02_addWishlist_course_not_found_should_throw_CourseNotFoundException")
    void addWishlist_courseNotFound_shouldThrow() {
        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishListService.addWishlist("emma@email.com", 99L))
                .isInstanceOf(com.jpd.web.exception.CourseNotFoundException.class)
                .hasMessageContaining("99");

        verify(wishlistRepository, never()).save(any());
    }

    // TC_ES_03: addWishlist - wishlist exists, throw WishlistExistException
    @Test
    @DisplayName("TC_ES_03_addWishlist_existing_wishlist_should_throw_WishlistExistException")
    void addWishlist_existingWishlist_shouldThrow() {
        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
        when(courseRepository.findById(99L)).thenReturn(Optional.of(course));
        when(wishlistRepository.findByCourse_CourseIdAndCustomer_CustomerId(99L, 1L))
                .thenReturn(Optional.of(wishlist));

        assertThatThrownBy(() -> wishListService.addWishlist("emma@email.com", 99L))
                .isInstanceOf(WishlistExistException.class)
                .hasMessageContaining("you have added this course in your wishlish");

        verify(wishlistRepository, never()).save(any());
    }

    // TC_ES_04: retrieveYourWishlist - customer not found
    @Test
    @DisplayName("TC_ES_04_retrieveYourWishlist_customer_not_found_should_throw_NoSuchElementException")
    void retrieveYourWishlist_customerNotFound_shouldThrow() {
        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishListService.retrieveYourWishlist("emma@email.com"))
                .isInstanceOf(NoSuchElementException.class);
    }

    // TC_ES_05: addWishlist - save throws DataAccessException, do not swallow
    @Test
    @DisplayName("TC_ES_05_addWishlist_save_throws_should_propagate")
    void addWishlist_saveThrows_shouldPropagate() {
        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
        when(courseRepository.findById(99L)).thenReturn(Optional.of(course));
        when(wishlistRepository.findByCourse_CourseIdAndCustomer_CustomerId(99L, 1L)).thenReturn(Optional.empty());
        when(wishlistRepository.save(any())).thenThrow(new DataAccessException("db error") {});

        assertThatThrownBy(() -> wishListService.addWishlist("emma@email.com", 99L))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("db error");
    }

    // TC_ES_06: retrieveYourWishlist - transform throws
    @Test
    @DisplayName("TC_ES_06_retrieveYourWishlist_transform_throws_should_propagate")
    void retrieveYourWishlist_transformThrows_shouldPropagate() {
        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
        List<Wishlist> wishlists = List.of(wishlist);
        when(wishlistRepository.findByCustomer(cus)).thenReturn(wishlists);

        try (MockedStatic<WishlistTransform> mockedTransform = mockStatic(WishlistTransform.class)) {
            RuntimeException ex = new RuntimeException("transform err");
            mockedTransform.when(() -> WishlistTransform.transformToWishlistDto(wishlist)).thenThrow(ex);
            assertThatThrownBy(() -> wishListService.retrieveYourWishlist("emma@email.com"))
                    .isSameAs(ex);
        }
    }

    // TC_ES_07: addWishlist_invalid_email_should_throw_NoSuchElementException (đọc từ CSV)
    @ParameterizedTest
    @CsvFileSource(resources = "/invalid_emails.csv", numLinesToSkip = 1, nullValues = {"NULL"})
    @DisplayName("TC_ES_07_addWishlist_invalid_email_should_throw_NoSuchElementException (CSV)")
    void addWishlist_invalidEmail_shouldThrow(String badEmail) {
        when(customerRepository.findByEmail(badEmail)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishListService.addWishlist(badEmail, 123L))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(courseRepository, wishlistRepository);
    }

    // TC_ES_07: retrieveYourWishlist_invalid_email_should_throw_NoSuchElementException (đọc từ CSV)
    @ParameterizedTest
    @CsvFileSource(resources = "/invalid_emails.csv", numLinesToSkip = 1, nullValues = {"NULL"})
    @DisplayName("TC_ES_07_retrieveYourWishlist_invalid_email_should_throw_NoSuchElementException (CSV)")
    void retrieveYourWishlist_invalidEmail_shouldThrow(String badEmail) {
        when(customerRepository.findByEmail(badEmail)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishListService.retrieveYourWishlist(badEmail))
                .isInstanceOf(NoSuchElementException.class);

        verifyNoInteractions(wishlistRepository);
    }
    // TC_HP_01: addWishlist happy path
//    @Test
//    @DisplayName("TC_HP_01_addWishlist_happy_path_should_save_successfully")
//    void addWishlist_happy_path() {
//        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
//        when(courseRepository.findById(99L)).thenReturn(Optional.of(course));
//        when(wishlistRepository.findByCourse_CourseIdAndCustomer_CustomerId(99L, 1L)).thenReturn(Optional.empty());
//
//        wishListService.addWishlist("emma@email.com", 99L);
//
//        verify(wishlistRepository, times(1)).save(argThat(w ->
//            w.getCustomer() == cus && w.getCourse() == course
//        ));
//        verifyNoMoreInteractions(wishlistRepository);
//    }
//
//    // TC_HP_02: retrieveYourWishlist happy path with 2 items, test transform/order
//    @Test
//    @DisplayName("TC_HP_02_retrieveYourWishlist_happy_path_should_return_dto_list_exact_order")
//    void retrieveYourWishlist_happy_path() {
//        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
//        List<Wishlist> wishlists = List.of(wishlist, wishlist2);
//        when(wishlistRepository.findByCustomer(cus)).thenReturn(wishlists);
//
//        WishlistDto dto1 = WishlistDto.builder().courseId(99L).course_name("Algebra").build();
//        WishlistDto dto2 = WishlistDto.builder().courseId(98L).course_name("Geometry").build();
//
//        try (MockedStatic<WishlistTransform> mockedTransform = mockStatic(WishlistTransform.class)) {
//            mockedTransform.when(() -> WishlistTransform.transformToWishlistDto(wishlist)).thenReturn(dto1);
//            mockedTransform.when(() -> WishlistTransform.transformToWishlistDto(wishlist2)).thenReturn(dto2);
//
//            List<WishlistDto> result = wishListService.retrieveYourWishlist("emma@email.com");
//
//            assertThat(result).containsExactly(dto1, dto2);
//            mockedTransform.verify(() -> WishlistTransform.transformToWishlistDto(wishlist), times(1));
//            mockedTransform.verify(() -> WishlistTransform.transformToWishlistDto(wishlist2), times(1));
//            mockedTransform.verifyNoMoreInteractions();
//        }
//    }
//
//    // TC_EC_01: addWishlist with invalid courseId (negative/zero/huge but not found)
//    @ParameterizedTest
//    @ValueSource(longs = {-123, 0, Long.MAX_VALUE})
//    @DisplayName("TC_EC_01_addWishlist_invalid_courseId_not_found_should_throw_CourseNotFoundException")
//    void addWishlist_invalidCourseId_shouldThrow(long cid) {
//        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
//        when(courseRepository.findById(cid)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> wishListService.addWishlist("emma@email.com", cid))
//                .isInstanceOf(com.jpd.web.exception.CourseNotFoundException.class)
//                .hasMessageContaining(String.valueOf(cid));
//
//        verify(wishlistRepository, never()).save(any());
//    }
//
//    // TC_EC_02: retrieveYourWishlist returns empty; no transform called
//    @Test
//    @DisplayName("TC_EC_02_retrieveYourWishlist_no_wishlist_return_empty_list")
//    void retrieveYourWishlist_empty() {
//        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
//        when(wishlistRepository.findByCustomer(cus)).thenReturn(Collections.emptyList());
//
//        try (MockedStatic<WishlistTransform> mockedTransform = mockStatic(WishlistTransform.class)) {
//            List<WishlistDto> result = wishListService.retrieveYourWishlist("emma@email.com");
//            assertThat(result).isEmpty();
//            // Transform not called
//            mockedTransform.verifyNoInteractions();
//        }
//    }
//
//    // TC_EC_03: retrieveYourWishlist order preserved
//    @Test
//    @DisplayName("TC_EC_03_retrieveYourWishlist_should_preserve_order")
//    void retrieveYourWishlist_orderPreserved() {
//        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
//        // Deliberate out-of-natural-order
//        List<Wishlist> wishlists = Arrays.asList(wishlist2, wishlist); // order: wishlist2, wishlist
//        when(wishlistRepository.findByCustomer(cus)).thenReturn(wishlists);
//
//        WishlistDto dto2 = WishlistDto.builder().courseId(98L).course_name("Geometry").build();
//        WishlistDto dto1 = WishlistDto.builder().courseId(99L).course_name("Algebra").build();
//
//        try (MockedStatic<WishlistTransform> mockedTransform = mockStatic(WishlistTransform.class)) {
//            mockedTransform.when(() -> WishlistTransform.transformToWishlistDto(wishlist2)).thenReturn(dto2);
//            mockedTransform.when(() -> WishlistTransform.transformToWishlistDto(wishlist)).thenReturn(dto1);
//
//            List<WishlistDto> result = wishListService.retrieveYourWishlist("emma@email.com");
//            assertThat(result).containsExactly(dto2, dto1);
//        }
//    }
//
//    // TC_EC_04: addWishlist - customer has other wishlists but not this courseId; should save
//    @Test
//    @DisplayName("TC_EC_04_addWishlist_with_existing_non_matching_wishlists_should_save")
//    void addWishlist_existingNonMatchingWishlistsShouldSave() {
//        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
//        when(courseRepository.findById(99L)).thenReturn(Optional.of(course));
//        // wishlist for another course exists, but target (99L) does not
//        when(wishlistRepository.findByCourse_CourseIdAndCustomer_CustomerId(99L, 1L)).thenReturn(Optional.empty());
//        // could also: when(wishlistRepository.findByCustomer(cus)).thenReturn(List.of(wishlist2));
//
//        wishListService.addWishlist("emma@email.com", 99L);
//
//        verify(wishlistRepository, times(1)).save(any());
//    }
//
//    // TC_ES_01: addWishlist - customer not found, expect NoSuchElement
//    @Test
//    @DisplayName("TC_ES_01_addWishlist_customer_not_found_should_throw_NoSuchElementException")
//    void addWishlist_customerNotFound_shouldThrow() {
//        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> wishListService.addWishlist("emma@email.com", 99L))
//                .isInstanceOf(NoSuchElementException.class);
//
//        verifyNoInteractions(courseRepository, wishlistRepository);
//    }
//
//    // TC_ES_02: addWishlist - course not found
//    @Test
//    @DisplayName("TC_ES_02_addWishlist_course_not_found_should_throw_CourseNotFoundException")
//    void addWishlist_courseNotFound_shouldThrow() {
//        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
//        when(courseRepository.findById(99L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> wishListService.addWishlist("emma@email.com", 99L))
//                .isInstanceOf(com.jpd.web.exception.CourseNotFoundException.class)
//                .hasMessageContaining("99");
//
//        verify(wishlistRepository, never()).save(any());
//    }
//
//    // TC_ES_03: addWishlist - wishlist exists, throw WishlistExistException
//    @Test
//    @DisplayName("TC_ES_03_addWishlist_existing_wishlist_should_throw_WishlistExistException")
//    void addWishlist_existingWishlist_shouldThrow() {
//        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
//        when(courseRepository.findById(99L)).thenReturn(Optional.of(course));
//        when(wishlistRepository.findByCourse_CourseIdAndCustomer_CustomerId(99L, 1L))
//                .thenReturn(Optional.of(wishlist));
//
//        assertThatThrownBy(() -> wishListService.addWishlist("emma@email.com", 99L))
//                .isInstanceOf(WishlistExistException.class)
//                .hasMessageContaining("you have added this course in your wishlish");
//
//        verify(wishlistRepository, never()).save(any());
//    }
//
//    // TC_ES_04: retrieveYourWishlist - customer not found
//    @Test
//    @DisplayName("TC_ES_04_retrieveYourWishlist_customer_not_found_should_throw_NoSuchElementException")
//    void retrieveYourWishlist_customerNotFound_shouldThrow() {
//        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> wishListService.retrieveYourWishlist("emma@email.com"))
//                .isInstanceOf(NoSuchElementException.class);
//    }
//
//    // TC_ES_05: addWishlist - save throws DataAccessException, do not swallow
//    @Test
//    @DisplayName("TC_ES_05_addWishlist_save_throws_should_propagate")
//    void addWishlist_saveThrows_shouldPropagate() {
//        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
//        when(courseRepository.findById(99L)).thenReturn(Optional.of(course));
//        when(wishlistRepository.findByCourse_CourseIdAndCustomer_CustomerId(99L, 1L)).thenReturn(Optional.empty());
//        when(wishlistRepository.save(any())).thenThrow(new DataAccessException("db error") {});
//
//        assertThatThrownBy(() -> wishListService.addWishlist("emma@email.com", 99L))
//                .isInstanceOf(DataAccessException.class)
//                .hasMessageContaining("db error");
//    }
//
//    // TC_ES_06: retrieveYourWishlist - transform throws
//    @Test
//    @DisplayName("TC_ES_06_retrieveYourWishlist_transform_throws_should_propagate")
//    void retrieveYourWishlist_transformThrows_shouldPropagate() {
//        when(customerRepository.findByEmail("emma@email.com")).thenReturn(Optional.of(cus));
//        List<Wishlist> wishlists = List.of(wishlist);
//        when(wishlistRepository.findByCustomer(cus)).thenReturn(wishlists);
//
//        try (MockedStatic<WishlistTransform> mockedTransform = mockStatic(WishlistTransform.class)) {
//            RuntimeException ex = new RuntimeException("transform err");
//            mockedTransform.when(() -> WishlistTransform.transformToWishlistDto(wishlist)).thenThrow(ex);
//            assertThatThrownBy(() -> wishListService.retrieveYourWishlist("emma@email.com"))
//                    .isSameAs(ex);
//        }
//    }
//
//    // TC_ES_07: input email null/empty, expect NoSuchElementException
//    @ParameterizedTest
//    @NullAndEmptySource
//    @DisplayName("TC_ES_07_addWishlist_invalid_email_should_throw_NoSuchElementException")
//    void addWishlist_invalidEmail_shouldThrow(String badEmail) {
//        when(customerRepository.findByEmail(badEmail)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> wishListService.addWishlist(badEmail, 123L))
//                .isInstanceOf(NoSuchElementException.class);
//
//        verifyNoInteractions(courseRepository, wishlistRepository);
//    }
//
//    @ParameterizedTest
//    @NullAndEmptySource
//    @DisplayName("TC_ES_07_retrieveYourWishlist_invalid_email_should_throw_NoSuchElementException")
//    void retrieveYourWishlist_invalidEmail_shouldThrow(String badEmail) {
//        when(customerRepository.findByEmail(badEmail)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> wishListService.retrieveYourWishlist(badEmail))
//                .isInstanceOf(NoSuchElementException.class);
//
//        verifyNoInteractions(wishlistRepository);
//    }
}
