package com.jpd.web.nguyen.unit.service.dictionary;

import com.jpd.web.model.RememberWord;
import com.jpd.web.repository.RememberWordRepository;
import com.jpd.web.service.DictionaryService;
import com.jpd.web.dto.RememberWordDto;           // chỉnh lại package theo dự án thực tế
import com.jpd.web.transform.RememberTransform;          // chỉnh lại package theo dự án thực tế
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho DictionaryService.getDictionary(email)
 * <p>
 * Bao phủ toàn bộ scenario đã phân tích:
 * <p>
 * HAPPY PATH:
 * - shouldReturnListOfDtos_whenRepositoryReturnsEntities
 * <p>
 * EDGE CASE:
 * - shouldReturnEmptyList_whenRepositoryReturnsEmptyList
 * <p>
 * ERROR SCENARIO (tuỳ chọn, nhưng rất hữu ích):
 * - shouldPropagateException_whenRepositoryThrows
 * <p>
 * Ghi chú kỹ thuật:
 * - repository.findAllByCustomer_Email(email) được mock
 * - RememberTransform.toRememberWordDto(...) là static -> mock bằng Mockito.mockStatic
 * - Service không có dependency khác
 */
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class DictionaryServiceGetDictionaryTest {

    @Mock
    private RememberWordRepository repository;

    @InjectMocks
    private DictionaryService dictionaryService;

    /**
     * TC_HP_01:
     * shouldReturnListOfDtos_whenRepositoryReturnsEntities
     * <p>
     * // Given:
     * - email = "user@example.com"
     * - repository.findAllByCustomer_Email(email) -> [rw1, rw2]
     * - RememberTransform.toRememberWordDto(rw1) -> dto1
     * - RememberTransform.toRememberWordDto(rw2) -> dto2
     * <p>
     * // When:
     * - gọi dictionaryService.getDictionary(email)
     * <p>
     * // Then:
     * - trả về list [dto1, dto2] đúng thứ tự
     * - size = 2
     * - không null
     * - repository được gọi đúng với email
     * - mapper static được gọi đúng cho từng phần tử
     */
    @Test
    void shouldReturnListOfDtos_whenRepositoryReturnsEntities() {
        // =====================
        // Given
        // =====================
        String email = "user@example.com";

        RememberWord rw1 = new RememberWord();
        rw1.setId(1L);
        rw1.setWord("test1");
        RememberWord rw2 = new RememberWord(); 
        rw2.setId(2L);
        rw2.setWord("test2");
        List<RememberWord> entities = List.of(rw1, rw2);

        when(repository.findAllByCustomer_Email(email)).thenReturn(entities);

        // Tạo DTOs với data khác nhau để test chính xác
        RememberWordDto dto1 = RememberWordDto.builder()
                .rwId(1L)
                .word("test1")
                .meaning("meaning1")
                .build();
        RememberWordDto dto2 = RememberWordDto.builder()
                .rwId(2L)
                .word("test2") 
                .meaning("meaning2")
                .build();

        try (MockedStatic<RememberTransform> mockedStatic = Mockito.mockStatic(RememberTransform.class)) {
            // map entity -> dto
            mockedStatic.when(() -> RememberTransform.toRememberWordDto(rw1)).thenReturn(dto1);
            mockedStatic.when(() -> RememberTransform.toRememberWordDto(rw2)).thenReturn(dto2);

            // =====================
            // When
            // =====================
            List<RememberWordDto> result = dictionaryService.getDictionary(email);

            // =====================
            // Then
            // =====================
            assertNotNull(result, "Kết quả không được null");
            assertEquals(2, result.size(), "Kết quả phải có đúng 2 phần tử");
            
            // NOTE: Implementation sử dụng stream.map().toList() tạo ra new instances
            // nên không thể dùng assertSame, phải dùng assertEquals để check content
            assertEquals(dto1, result.get(0),
                    "Phần tử đầu tiên trong result phải match dto1 map từ rw1");
            assertEquals(dto2, result.get(1),
                    "Phần tử thứ hai phải match dto2 map từ rw2");

            // verify repository được gọi đúng
            verify(repository, times(1)).findAllByCustomer_Email(email);

            // verify static mapper được gọi đúng
            mockedStatic.verify(
                    () -> RememberTransform.toRememberWordDto(rw1),
                    times(1)
            );
            mockedStatic.verify(
                    () -> RememberTransform.toRememberWordDto(rw2),
                    times(1)
            );
        }
    }

    /**
     * TC_EC_01:
     * shouldReturnEmptyList_whenRepositoryReturnsEmptyList
     * <p>
     * // Given:
     * - repository.findAllByCustomer_Email(email) -> Collections.emptyList()
     * <p>
     * // When:
     * - gọi dictionaryService.getDictionary(email)
     * <p>
     * // Then:
     * - trả về list rỗng, không null
     * - size = 0
     * - KHÔNG gọi RememberTransform.toRememberWordDto(...)
     */
    @Test
    void shouldReturnEmptyList_whenRepositoryReturnsEmptyList() {
        // =====================
        // Given
        // =====================
        String email = "empty@example.com";

        when(repository.findAllByCustomer_Email(email))
                .thenReturn(Collections.emptyList());

        // =====================
        // When
        // =====================
        List<RememberWordDto> result;
        // Vì ở đây không có entity nào để map, ta không cần mở mockStatic
        result = dictionaryService.getDictionary(email);

        // =====================
        // Then
        // =====================
        assertNotNull(result, "Service phải trả về list rỗng, không được trả về null");
        assertTrue(result.isEmpty(),
                "Nếu user chưa lưu từ nào thì kết quả phải là list rỗng");

        verify(repository, times(1)).findAllByCustomer_Email(email);
        // Không có entity => mapper static không được gọi nên không cần verify mockStatic ở đây
    }

    /**
     * TC_ES_01:
     * shouldPropagateException_whenRepositoryThrows
     * <p>
     * (Scenario lỗi phụ, giúp chứng minh hàm không swallow exception)
     * <p>
     * // Given:
     * - repository.findAllByCustomer_Email(email) -> ném RuntimeException("DB down")
     * <p>
     * // When / Then:
     * - gọi dictionaryService.getDictionary(email) sẽ ném RuntimeException bubble lên
     * - mapper static KHÔNG được gọi vì fail trước
     */
    @Test
    void shouldPropagateException_whenRepositoryThrows() {
        // =====================
        // Given
        // =====================
        String email = "boom@example.com";

        when(repository.findAllByCustomer_Email(email))
                .thenThrow(new RuntimeException("DB down"));

        // =====================
        // When / Then
        // =====================
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> dictionaryService.getDictionary(email),
                "Nếu repository ném lỗi DB thì service phải bubble RuntimeException"
        );

        assertTrue(
                ex.getMessage().contains("DB down"),
                "Thông báo exception phải phản ánh lỗi từ repository"
        );

        // Vì đã nổ ngay trong repository, sẽ KHÔNG có map -> không cần mở mockStatic
        verify(repository, times(1)).findAllByCustomer_Email(email);
    }
}
