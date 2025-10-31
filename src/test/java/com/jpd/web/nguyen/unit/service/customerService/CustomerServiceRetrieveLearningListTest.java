package com.jpd.web.nguyen.unit.service.customerService;

import com.jpd.web.service.CustomerService;
import com.jpd.web.service.CourseInfService;
import com.jpd.web.service.WishlistService;
import com.jpd.web.dto.CourseLearningCardDto;
import com.jpd.web.dto.WishlistDto;
import com.jpd.web.dto.LearningListDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho CustomerService.retrieveLearningList(email).
 * <p>
 * Bao phủ toàn bộ scenario đã phân tích:
 * <p>
 * HAPPY PATH:
 * - shouldReturnLearningListDto_whenBothCourseListAndWishlistHaveData
 * <p>
 * ERROR SCENARIOS:
 * - shouldPropagateException_whenRetrieveYourCourseThrows
 * - shouldPropagateException_whenRetrieveYourWishlistThrows
 * <p>
 * EDGE CASES:
 * - shouldReturnDtoWithEmptyCourseList_whenNoActiveCoursesButWishlistExists
 * - shouldReturnDtoWithEmptyWishlist_whenNoWishlistButHasCourses
 * - shouldReturnDtoWithBothListsEmpty_whenUserHasNoCoursesAndNoWishlist
 * <p>
 * Kỹ thuật:
 * - courseInfService và wishlistService đều được mock
 * - Không có static method / DB trong hàm, chỉ là orchestrator
 */
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CustomerServiceRetrieveLearningListTest {

    @Mock
    private CourseInfService courseInfService;

    @Mock
    private WishlistService wishlistService;

    @InjectMocks
    private CustomerService customerService;

    /**
     * TC_HP_01:
     * shouldReturnLearningListDto_whenBothCourseListAndWishlistHaveData
     * <p>
     * Mô tả:
     * // Given:
     * - courseInfService.retrieveYourCourse(email) -> trả về 2 khóa học đang học
     * - wishlistService.retrieveYourWishlist(email) -> trả về 1 wishlist item
     * <p>
     * // When:
     * - gọi retrieveLearningList(email)
     * <p>
     * // Then:
     * - result không null
     * - result.cardDtos = list trả về từ courseInfService
     * - result.wishlistDtos = list trả về từ wishlistService
     * - cả 2 service đều được gọi đúng với email
     */
    @Test
    void shouldReturnLearningListDto_whenBothCourseListAndWishlistHaveData() {
        // =====================
        // Given
        // =====================
        String email = "user@example.com";

        CourseLearningCardDto c1 = new CourseLearningCardDto();
        CourseLearningCardDto c2 = new CourseLearningCardDto();
        List<CourseLearningCardDto> learningCourses = List.of(c1, c2);

        WishlistDto w1 = new WishlistDto();
        List<WishlistDto> wishlist = List.of(w1);

        when(courseInfService.retrieveYourCourse(email))
                .thenReturn(learningCourses);

        when(wishlistService.retrieveYourWishlist(email))
                .thenReturn(wishlist);

        // =====================
        // When
        // =====================
        LearningListDto result = customerService.retrieveLearningList(email);

        // =====================
        // Then
        // =====================
        assertNotNull(result, "Service phải trả về LearningListDto, không được null");
        assertSame(learningCourses, result.getCardDtos(),
                "cardDtos trong DTO phải chính là list trả về từ courseInfService");
        assertSame(wishlist, result.getWishlistDtos(),
                "wishlistDtos trong DTO phải chính là list trả về từ wishlistService");

        // verify gọi dependency với đúng email
        verify(courseInfService, times(1)).retrieveYourCourse(email);
        verify(wishlistService, times(1)).retrieveYourWishlist(email);
    }

    /**
     * TC_ES_01:
     * shouldPropagateException_whenRetrieveYourCourseThrows
     * <p>
     * Mô tả:
     * // Given:
     * - courseInfService.retrieveYourCourse(email) ném RuntimeException("DB down")
     * - wishlistService không (và sẽ không) được gọi
     * <p>
     * // Then:
     * - retrieveLearningList(email) ném RuntimeException bubble lên
     * - wishlistService không bị gọi
     */
    @Test
    void shouldPropagateException_whenRetrieveYourCourseThrows() {
        // =====================
        // Given
        // =====================
        String email = "err-course@example.com";

        when(courseInfService.retrieveYourCourse(email))
                .thenThrow(new RuntimeException("DB down"));

        // =====================
        // When / Then
        // =====================
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> customerService.retrieveLearningList(email),
                "Nếu courseInfService ném lỗi thì method phải bubble RuntimeException lên"
        );

        assertTrue(
                ex.getMessage().contains("DB down"),
                "Thông báo lỗi phải chứa nội dung từ exception gốc"
        );

        // Vì lỗi đã xảy ra trước khi gọi wishlistService
        verify(wishlistService, never()).retrieveYourWishlist(anyString());
    }

    /**
     * TC_ES_02:
     * shouldPropagateException_whenRetrieveYourWishlistThrows
     * <p>
     * Mô tả:
     * // Given:
     * - courseInfService.retrieveYourCourse(email) -> list hợp lệ
     * - wishlistService.retrieveYourWishlist(email) -> ném RuntimeException("Redis timeout")
     * <p>
     * // Then:
     * - retrieveLearningList(email) ném RuntimeException bubble lên
     * - Đảm bảo courseInfService đã được gọi trước khi lỗi từ wishlistService xảy ra
     */
    @Test
    void shouldPropagateException_whenRetrieveYourWishlistThrows() {
        // =====================
        // Given
        // =====================
        String email = "err-wishlist@example.com";

        CourseLearningCardDto c1 = new CourseLearningCardDto();
        List<CourseLearningCardDto> learningCourses = List.of(c1);

        when(courseInfService.retrieveYourCourse(email))
                .thenReturn(learningCourses);

        when(wishlistService.retrieveYourWishlist(email))
                .thenThrow(new RuntimeException("Redis timeout"));

        // =====================
        // When / Then
        // =====================
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> customerService.retrieveLearningList(email),
                "Nếu wishlistService ném lỗi thì method phải bubble RuntimeException lên"
        );

        assertTrue(
                ex.getMessage().contains("Redis timeout"),
                "Thông báo lỗi phải chứa nội dung từ exception gốc"
        );

        // courseInfService phải được gọi trước khi sập
        verify(courseInfService, times(1)).retrieveYourCourse(email);
        // wishlistService được gọi và ném lỗi
        verify(wishlistService, times(1)).retrieveYourWishlist(email);
    }

    /**
     * TC_EC_01:
     * shouldReturnDtoWithEmptyCourseList_whenNoActiveCoursesButWishlistExists
     * <p>
     * Mô tả:
     * // Given:
     * - courseInfService.retrieveYourCourse(email) -> Collections.emptyList()
     * - wishlistService.retrieveYourWishlist(email) -> list có phần tử
     * <p>
     * // Then:
     * - DTO trả về có cardDtos rỗng, wishlistDtos chứa item
     * - Không ném exception
     */
    @Test
    void shouldReturnDtoWithEmptyCourseList_whenNoActiveCoursesButWishlistExists() {
        // =====================
        // Given
        // =====================
        String email = "fresh@example.com";

        List<CourseLearningCardDto> emptyCourses = Collections.emptyList();

        WishlistDto w1 = new WishlistDto();
        WishlistDto w2 = new WishlistDto();
        List<WishlistDto> wishlist = List.of(w1, w2);

        when(courseInfService.retrieveYourCourse(email))
                .thenReturn(emptyCourses);

        when(wishlistService.retrieveYourWishlist(email))
                .thenReturn(wishlist);

        // =====================
        // When
        // =====================
        LearningListDto result = customerService.retrieveLearningList(email);

        // =====================
        // Then
        // =====================
        assertNotNull(result, "DTO trả về không được null ngay cả khi list rỗng");
        assertNotNull(result.getCardDtos(), "cardDtos không được null");
        assertTrue(result.getCardDtos().isEmpty(),
                "cardDtos phải rỗng khi user chưa có khóa học đang học");

        assertNotNull(result.getWishlistDtos(), "wishlistDtos không được null");
        assertEquals(2, result.getWishlistDtos().size(),
                "wishlistDtos phải giữ nguyên các phần tử từ wishlistService");

        verify(courseInfService, times(1)).retrieveYourCourse(email);
        verify(wishlistService, times(1)).retrieveYourWishlist(email);
    }

    /**
     * TC_EC_02:
     * shouldReturnDtoWithEmptyWishlist_whenNoWishlistButHasCourses
     * <p>
     * Mô tả:
     * // Given:
     * - courseInfService.retrieveYourCourse(email) -> list có phần tử
     * - wishlistService.retrieveYourWishlist(email) -> Collections.emptyList()
     * <p>
     * // Then:
     * - DTO trả về có cardDtos != empty, wishlistDtos rỗng
     * - Không ném exception
     */
    @Test
    void shouldReturnDtoWithEmptyWishlist_whenNoWishlistButHasCourses() {
        // =====================
        // Given
        // =====================
        String email = "active-no-wishlist@example.com";

        CourseLearningCardDto c1 = new CourseLearningCardDto();
        CourseLearningCardDto c2 = new CourseLearningCardDto();
        List<CourseLearningCardDto> learningCourses = List.of(c1, c2);

        List<WishlistDto> emptyWishlist = Collections.emptyList();

        when(courseInfService.retrieveYourCourse(email))
                .thenReturn(learningCourses);

        when(wishlistService.retrieveYourWishlist(email))
                .thenReturn(emptyWishlist);

        // =====================
        // When
        // =====================
        LearningListDto result = customerService.retrieveLearningList(email);

        // =====================
        // Then
        // =====================
        assertNotNull(result);
        assertNotNull(result.getCardDtos());
        assertEquals(2, result.getCardDtos().size(),
                "cardDtos phải giữ nguyên số lượng từ courseInfService");

        assertNotNull(result.getWishlistDtos());
        assertTrue(result.getWishlistDtos().isEmpty(),
                "wishlistDtos phải rỗng khi wishlistService trả về empty list");

        verify(courseInfService, times(1)).retrieveYourCourse(email);
        verify(wishlistService, times(1)).retrieveYourWishlist(email);
    }

    /**
     * TC_EC_03:
     * shouldReturnDtoWithBothListsEmpty_whenUserHasNoCoursesAndNoWishlist
     * <p>
     * Mô tả:
     * // Given:
     * - courseInfService.retrieveYourCourse(email) -> Collections.emptyList()
     * - wishlistService.retrieveYourWishlist(email) -> Collections.emptyList()
     * <p>
     * // Then:
     * - DTO trả về có 2 list rỗng, không null
     * - Không ném exception
     * <p>
     * Đây là base state của user hoàn toàn mới.
     */
    @Test
    void shouldReturnDtoWithBothListsEmpty_whenUserHasNoCoursesAndNoWishlist() {
        // =====================
        // Given
        // =====================
        String email = "brandnew@example.com";

        List<CourseLearningCardDto> emptyCourses = Collections.emptyList();
        List<WishlistDto> emptyWishlist = Collections.emptyList();

        when(courseInfService.retrieveYourCourse(email))
                .thenReturn(emptyCourses);

        when(wishlistService.retrieveYourWishlist(email))
                .thenReturn(emptyWishlist);

        // =====================
        // When
        // =====================
        LearningListDto result = customerService.retrieveLearningList(email);

        // =====================
        // Then
        // =====================
        assertNotNull(result, "DTO trả về không được null ngay cả khi không có dữ liệu");
        assertNotNull(result.getCardDtos(), "cardDtos trong DTO không được null");
        assertTrue(result.getCardDtos().isEmpty(),
                "cardDtos phải rỗng khi user chưa học gì");

        assertNotNull(result.getWishlistDtos(), "wishlistDtos trong DTO không được null");
        assertTrue(result.getWishlistDtos().isEmpty(),
                "wishlistDtos phải rỗng khi user không wishlist gì");

        verify(courseInfService, times(1)).retrieveYourCourse(email);
        verify(wishlistService, times(1)).retrieveYourWishlist(email);
    }
}
