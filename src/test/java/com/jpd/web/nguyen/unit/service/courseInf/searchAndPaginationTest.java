package com.jpd.web.nguyen.unit.service.courseInf;

import com.jpd.web.repository.CourseRepository;
import com.jpd.web.service.CourseInfService;
import com.jpd.web.dto.CourseSearchDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho hàm:
 *
 *     public Page<CourseSearchDto> searchAndPagination(String searchKey, Pageable pageable)
 *
 * Các kịch bản bao gồm:
 *
 *  - Happy Path:
 *      TC_HP_01: searchKey hợp lệ, pageable hợp lệ, repo trả về Page có data
 *
 *  - Edge Cases:
 *      TC_EC_01: searchKey chỉ chứa dấu cách → trim() = "", repo được gọi với ""
 *      TC_EC_02: pageable = null → service không validate pageable, pass thẳng xuống repo
 *
 *  - Error Scenarios:
 *      TC_ES_01: searchKey = null → NullPointerException vì searchKey.trim()
 *
 * Lưu ý quan trọng:
 *  Code hiện tại:
 *
 *      if (searchKey.trim() == null)
 *          return null;
 *
 *  - Nếu searchKey = null ⇒ gọi null.trim() ⇒ NullPointerException
 *  - Nếu searchKey = "   " ⇒ searchKey.trim() = "" (không phải null) ⇒ vẫn gọi repo
 */
class searchAndPaginationTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseInfService courseInfService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * ======================
     * TC_HP_01 (Happy Path)
     * ======================
     *
     * shouldReturnPagedResult_whenRepositoryReturnsPage
     *
     * // Given:
     * - searchKey = "java"
     * - pageable = PageRequest.of(0, 10)
     * - courseRepository.searchAndCalculate("java", pageable)
     *     trả về Page chứa 2 phần tử dto1, dto2
     *
     * // When:
     * - gọi courseInfService.searchAndPagination("java", pageable)
     *
     * // Then:
     * - Kết quả không null
     * - Kết quả chính là Page mock từ repository (service không sửa đổi)
     * - Số phần tử trong page.getContent() == 2
     * - Repo được gọi đúng 1 lần với "java" (là searchKey.trim()) và pageable
     */
    @Test
    void shouldReturnPagedResult_whenRepositoryReturnsPage() {
        // =========================
        // Given
        // =========================
        String searchKey = "java";
        Pageable pageable = PageRequest.of(0, 10);

        CourseSearchDto dto1 = new CourseSearchDto();
        CourseSearchDto dto2 = new CourseSearchDto();

        Page<CourseSearchDto> pageFromRepo =
                new PageImpl<>(List.of(dto1, dto2), pageable, 2);

        when(courseRepository.searchAndCalculate(searchKey, pageable))
                .thenReturn(pageFromRepo);

        // =========================
        // When
        // =========================
        Page<CourseSearchDto> result =
                courseInfService.searchAndPagination(searchKey, pageable);

        // =========================
        // Then
        // =========================
        assertNotNull(result, "Service không được trả về null trong happy path");
        assertSame(pageFromRepo, result,
                "Service phải trả thẳng Page từ repository, không được tạo bản sao khác");
        assertEquals(2, result.getContent().size(),
                "Page trả về phải có đúng 2 phần tử như mock");

        verify(courseRepository, times(1))
                .searchAndCalculate("java", pageable);
    }

    /**
     * ======================
     * TC_EC_01 (Edge Case)
     * ======================
     *
     * shouldReturnNull_whenSearchKeyIsSpacesOnly
     *
     * // Given:
     * - searchKey = "   " (toàn dấu cách)
     * - searchKey.trim() = "" (chuỗi rỗng)
     * - Implementation check: if (searchKey == null || searchKey.trim().isEmpty()) return null;
     *
     * // When:
     * - gọi courseInfService.searchAndPagination("   ", pageable)
     *
     * // Then:
     * - Kết quả == null (vì searchKey.trim().isEmpty() == true)
     * - Repository KHÔNG được gọi
     */
    @Test
    void shouldReturnNull_whenSearchKeyIsSpacesOnly() {
        // =========================
        // Given
        // =========================
        String rawKey = "   ";
        Pageable pageable = PageRequest.of(1, 5);

        // =========================
        // When
        // =========================
        Page<CourseSearchDto> result =
                courseInfService.searchAndPagination(rawKey, pageable);

        // =========================
        // Then
        // =========================
        assertNull(result,
                "searchKey chỉ chứa khoảng trắng → trim().isEmpty() == true → return null");

        // Verify repository KHÔNG được gọi vì đã return null sớm
        verifyNoInteractions(courseRepository);
    }

    /**
     * ======================
     * TC_EC_02 (Edge Case)
     * ======================
     *
     * shouldReturnPageEvenIfPageableIsNull_whenRepositoryHandlesNullPageable
     *
     * // Given:
     * - searchKey = "spring"
     * - pageable = null
     * - courseRepository.searchAndCalculate("spring", null)
     *     trả về 1 Page có 1 phần tử
     *
     * // When:
     * - gọi courseInfService.searchAndPagination("spring", null)
     *
     * // Then:
     * - Kết quả không null
     * - Kết quả có đúng 1 phần tử
     * - Repo được gọi với pageable=null (service không validate pageable)
     *
     * Giải thích:
     * Code service KHÔNG kiểm tra pageable,
     * nên nó pass thẳng null xuống repository.
     * Test này thể hiện rõ behavior hiện tại.
     */
    @Test
    void shouldReturnPageEvenIfPageableIsNull_whenRepositoryHandlesNullPageable() {
        // =========================
        // Given
        // =========================
        String searchKey = "spring";
        Pageable pageable = null; // intentionally null

        CourseSearchDto dto = new CourseSearchDto();
        Page<CourseSearchDto> pageFromRepo =
                // PageImpl(List<T>) sẽ tự dùng Pageable.unpaged() bên trong
                new PageImpl<>(List.of(dto));

        when(courseRepository.searchAndCalculate(searchKey, pageable))
                .thenReturn(pageFromRepo);

        // =========================
        // When
        // =========================
        Page<CourseSearchDto> result =
                courseInfService.searchAndPagination(searchKey, pageable);

        // =========================
        // Then
        // =========================
        assertNotNull(result,
                "Service vẫn phải trả về Page ngay cả khi pageable=null (repo chịu trách nhiệm)");
        assertEquals(1, result.getContent().size(),
                "Page mock trả về có 1 phần tử phải giữ nguyên");
        assertSame(pageFromRepo, result,
                "Service không được thay đổi Page do repo trả về");

        // verify repo được gọi với pageable=null
        verify(courseRepository, times(1))
                .searchAndCalculate("spring", null);
    }

    /**
     * ==========================
     * TC_ES_01 (Error Scenario) 
     * ==========================
     *
     * shouldReturnNull_whenSearchKeyIsNull
     *
     * // Given:
     * - searchKey = null
     * - pageable = PageRequest.of(0, 10) (bất kỳ)
     *
     * // When:
     * - Gọi courseInfService.searchAndPagination(null, pageable)
     *   → Implementation: if (searchKey == null || searchKey.trim().isEmpty()) return null;
     *   → searchKey == null → return null (không NPE)
     *
     * // Then:
     * - Kết quả == null
     * - Repository KHÔNG bị gọi
     *
     * Lưu ý:
     * Implementation đã được sửa để handle null safely.
     */
    @Test
    void shouldReturnNull_whenSearchKeyIsNull() {
        // =========================
        // Given
        // =========================
        String searchKey = null;
        Pageable pageable = PageRequest.of(0, 10);

        // =========================
        // When
        // =========================
        Page<CourseSearchDto> result = courseInfService.searchAndPagination(searchKey, pageable);

        // =========================
        // Then
        // =========================
        assertNull(result,
                "searchKey == null → implementation return null (không throw NPE)");

        // Repository không nên bị gọi vì đã return null sớm
        verifyNoInteractions(courseRepository);
    }

    // ==========================================================
    // ============== ADDITIONAL TEST CASES ====================
    // ==========================================================

    /**
     * TC_EC_03: shouldReturnNull_whenSearchKeyIsEmptyString
     *
     * Test với searchKey = "" (empty string)
     */
    @Test
    void shouldReturnNull_whenSearchKeyIsEmptyString() {
        // =========================
        // Given
        // =========================
        String searchKey = "";
        Pageable pageable = PageRequest.of(0, 5);

        // =========================
        // When
        // =========================
        Page<CourseSearchDto> result = courseInfService.searchAndPagination(searchKey, pageable);

        // =========================
        // Then
        // =========================
        assertNull(result,
                "searchKey = \"\" → trim().isEmpty() == true → return null");

        verifyNoInteractions(courseRepository);
    }

    /**
     * TC_HP_02: shouldTrimSearchKeyBeforeCallingRepository
     *
     * Test với searchKey có whitespace ở đầu cuối nhưng có content
     */
    @Test
    void shouldTrimSearchKeyBeforeCallingRepository() {
        // =========================
        // Given
        // =========================
        String searchKeyWithSpaces = "  spring boot  ";
        String trimmedKey = "spring boot";
        Pageable pageable = PageRequest.of(0, 10);

        CourseSearchDto dto = new CourseSearchDto();
        Page<CourseSearchDto> pageFromRepo =
                new PageImpl<>(List.of(dto), pageable, 1);

        when(courseRepository.searchAndCalculate(trimmedKey, pageable))
                .thenReturn(pageFromRepo);

        // =========================
        // When
        // =========================
        Page<CourseSearchDto> result =
                courseInfService.searchAndPagination(searchKeyWithSpaces, pageable);

        // =========================
        // Then
        // =========================
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertSame(pageFromRepo, result);

        // Verify repository được gọi với key đã trim
        verify(courseRepository, times(1))
                .searchAndCalculate(trimmedKey, pageable);
    }

    /**
     * TC_EC_04: shouldHandleRepositoryReturningEmptyPage
     *
     * Test khi repository trả về empty page
     */
    @Test
    void shouldHandleRepositoryReturningEmptyPage() {
        // =========================
        // Given
        // =========================
        String searchKey = "nonexistent";
        Pageable pageable = PageRequest.of(0, 10);

        Page<CourseSearchDto> emptyPage =
                new PageImpl<>(List.of(), pageable, 0);

        when(courseRepository.searchAndCalculate(searchKey, pageable))
                .thenReturn(emptyPage);

        // =========================
        // When
        // =========================
        Page<CourseSearchDto> result =
                courseInfService.searchAndPagination(searchKey, pageable);

        // =========================
        // Then
        // =========================
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        assertSame(emptyPage, result);

        verify(courseRepository, times(1))
                .searchAndCalculate(searchKey, pageable);
    }

    /**
     * TC_EC_05: shouldHandleLargePageRequest
     *
     * Test với page request lớn
     */
    @Test
    void shouldHandleLargePageRequest() {
        // =========================
        // Given
        // =========================
        String searchKey = "popular";
        Pageable largePage = PageRequest.of(10, 100); // page 10, size 100

        CourseSearchDto dto1 = new CourseSearchDto();
        CourseSearchDto dto2 = new CourseSearchDto();
        List<CourseSearchDto> content = List.of(dto1, dto2);
        Page<CourseSearchDto> pageFromRepo = 
                new PageImpl<>(content, largePage, 1050L); // total 1050

        when(courseRepository.searchAndCalculate(searchKey, largePage))
                .thenReturn(pageFromRepo);

        // =========================
        // When
        // =========================
        Page<CourseSearchDto> result =
                courseInfService.searchAndPagination(searchKey, largePage);

        // =========================
        // Then
        // =========================
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(pageFromRepo.getTotalElements(), result.getTotalElements()); // Should match mock
        assertEquals(10, result.getNumber()); // current page
        assertEquals(100, result.getSize()); // page size
        assertSame(pageFromRepo, result);

        verify(courseRepository, times(1))
                .searchAndCalculate(searchKey, largePage);
    }
}
