package com.jpd.web.nguyen.unit.service.dictionary;

import com.jpd.web.model.Customer;
import com.jpd.web.model.RememberWord;
import com.jpd.web.repository.RememberWordRepository;
import com.jpd.web.service.DictionaryService;
import com.jpd.web.dto.RememberWordDto;      // chỉnh lại package nếu khác
import com.jpd.web.service.utils.ValidationResources; // chỉnh lại package nếu khác
import com.jpd.web.transform.RememberTransform;       // chỉnh lại package nếu khác
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho DictionaryService.addRememberWord(email, rememberWordDto)
 *
 * Bao phủ toàn bộ scenario đã phân tích:
 *
 *  HAPPY PATH:
 *    - shouldReturnDtoAndSaveEntity_whenCustomerExists
 *
 *  ERROR SCENARIO:
 *    - shouldReturnNullAndNotSave_whenCustomerDoesNotExist
 *
 *  EDGE CASE (optional / business clarity):
 *    - shouldSaveEvenIfDtoHasMinimalFields_whenCustomerExists
 *
 * Ghi chú kỹ thuật:
 *  - validationResources.validateCustomerExist(email) => trả Customer hoặc null
 *  - RememberTransform.toRememberWord(dto) là STATIC => mock bằng mockStatic
 *  - repository.save(model) được verify qua ArgumentCaptor
 */
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class DictionaryServiceAddRememberWordTest {

    @Mock
    private ValidationResources validationResources;

    @Mock
    private RememberWordRepository repository;

    @InjectMocks
    private DictionaryService dictionaryService;

    /**
     * TC_HP_01:
     * shouldReturnDtoAndSaveEntity_whenCustomerExists
     *
     * // Given:
     *   - email = "user@example.com"
     *   - rememberWordDto có dữ liệu từ cần lưu
     *   - validationResources.validateCustomerExist(email) -> Customer hợp lệ
     *   - RememberTransform.toRememberWord(rememberWordDto) -> RememberWord entity trống ban đầu
     *
     * // When:
     *   - gọi dictionaryService.addRememberWord(email, rememberWordDto)
     *
     * // Then:
     *   - return không null và chính là cùng tham chiếu rememberWordDto truyền vào
     *   - repository.save(...) được gọi 1 lần với RememberWord đã được gán customer
     *   - RememberTransform.toRememberWord(...) được gọi đúng tham số
     *   - validateCustomerExist(...) được gọi đúng email
     */
    @Test
    void shouldReturnDtoAndSaveEntity_whenCustomerExists() {
        // =====================
        // Given
        // =====================
        String email = "user@example.com";

        // DTO đầu vào
        RememberWordDto rememberWordDto = new RememberWordDto();
        // có thể set thêm field nếu bạn muốn assert cụ thể, ví dụ:
        // rememberWordDto.setWord("こんにちは");

        // Customer giả định tồn tại
        Customer mockCustomer = Customer.builder()
                .customerId(123L)
                .email(email)
                .build();

        when(validationResources.validateCustomerExist(email))
                .thenReturn(mockCustomer);

        // Entity RememberWord ban đầu từ mapper
        RememberWord mappedEntity = new RememberWord();
        // chưa có customer gán, service sẽ gán sau

        // Khi save, repository trả về chính entity truyền vào
        when(repository.save(any(RememberWord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<RememberTransform> mockedStatic =
                     Mockito.mockStatic(RememberTransform.class)) {

            // mock static mapper
            mockedStatic.when(() -> RememberTransform.toRememberWord(rememberWordDto))
                    .thenReturn(mappedEntity);

            // =====================
            // When
            // =====================
            RememberWordDto result =
                    dictionaryService.addRememberWord(email, rememberWordDto);

            // =====================
            // Then
            // =====================
            // Kết quả phải trả về chính DTO đầu vào (theo code hiện tại)
            assertNotNull(result, "Service phải trả về DTO, không phải null");
            assertSame(rememberWordDto, result,
                    "Service phải trả về nguyên RememberWordDto truyền vào (không build DTO mới)");

            // Kiểm tra repository.save() được gọi với entity đã gắn customer
            ArgumentCaptor<RememberWord> captor =
                    ArgumentCaptor.forClass(RememberWord.class);
            verify(repository, times(1)).save(captor.capture());

            RememberWord savedEntity = captor.getValue();
            assertNotNull(savedEntity,
                    "Entity truyền vào save() không được null");
            assertEquals(mockCustomer,
                    savedEntity.getCustomer(),
                    "Service phải gán customer vào RememberWord trước khi save");

            // verify validateCustomerExist được gọi 1 lần
            verify(validationResources, times(1))
                    .validateCustomerExist(email);

            // verify mapper static được gọi đúng 1 lần với DTO đầu vào
            mockedStatic.verify(
                    () -> RememberTransform.toRememberWord(rememberWordDto),
                    times(1)
            );
        }
    }

    /**
     * TC_ES_01:
     * shouldReturnNullAndNotSave_whenCustomerDoesNotExist
     *
     * // Given:
     *   - validationResources.validateCustomerExist(email) -> null
     *
     * // When:
     *   - gọi dictionaryService.addRememberWord(email, dto)
     *
     * // Then:
     *   - return null
     *   - KHÔNG gọi RememberTransform.toRememberWord(...)
     *   - KHÔNG gọi repository.save(...)
     */
    @Test
    void shouldReturnNullAndNotSave_whenCustomerDoesNotExist() {
        // =====================
        // Given
        // =====================
        String email = "ghost@example.com";

        RememberWordDto rememberWordDto = new RememberWordDto();

        // validateCustomerExist trả về null => user không tồn tại
        when(validationResources.validateCustomerExist(email))
                .thenReturn(null);

        // =====================
        // When
        // =====================
        RememberWordDto result =
                dictionaryService.addRememberWord(email, rememberWordDto);

        // =====================
        // Then
        // =====================
        assertNull(result,
                "Nếu customer không tồn tại, service phải trả về null");

        // Vì customer == null nên logic if(customer != null) không chạy,
        // => KHÔNG được gọi mapper static và KHÔNG save
        verify(repository, never()).save(any(RememberWord.class));

        // Với static method, ta không mở mockStatic ở test này,
        // nhưng ta vẫn có thể đảm bảo bằng verifyNoInteractions gián tiếp:
        // Không có cách verify trực tiếp RememberTransform.toRememberWord(...) nếu không mockStatic,
        // nhưng việc không mở mockStatic cũng OK.
        // Điều quan trọng nhất: không crash, không save.
        verify(validationResources, times(1))
                .validateCustomerExist(email);
    }

    /**
     * TC_EC_01 (Edge Case tuỳ chọn):
     * shouldSaveEvenIfDtoHasMinimalFields_whenCustomerExists
     *
     * Ý nghĩa:
     *  - Cho thấy service KHÔNG tự validate nội dung DTO.
     *  - Dù DTO thiếu field (ví dụ không có nghĩa, phát âm...), miễn là customer tồn tại
     *    thì service vẫn convert, gán customer và save.
     *
     * // Given:
     *   - DTO có thể "thiếu dữ liệu"
     *   - validateCustomerExist(email) -> Customer
     *   - RememberTransform.toRememberWord(dto) -> RememberWord (có thể rỗng field)
     *
     * // Then:
     *   - vẫn save
     *   - vẫn trả DTO ban đầu
     *   - không ném exception
     */
    @Test
    void shouldSaveEvenIfDtoHasMinimalFields_whenCustomerExists() {
        // =====================
        // Given
        // =====================
        String email = "minimal@example.com";

        // DTO "thiếu dữ liệu" - giả lập object rỗng (tùy theo fields thật sự của RememberWordDto)
        RememberWordDto minimalDto = new RememberWordDto();
        // ví dụ nếu DTO có setter:
        // minimalDto.setWord(null);
        // minimalDto.setMeaning(null);

        Customer mockCustomer = Customer.builder()
                .customerId(999L)
                .email(email)
                .build();

        when(validationResources.validateCustomerExist(email))
                .thenReturn(mockCustomer);

        RememberWord mappedEntity = new RememberWord();
        // mappedEntity chưa có customer, service sẽ gán

        when(repository.save(any(RememberWord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<RememberTransform> mockedStatic =
                     Mockito.mockStatic(RememberTransform.class)) {

            mockedStatic.when(() -> RememberTransform.toRememberWord(minimalDto))
                    .thenReturn(mappedEntity);

            // =====================
            // When
            // =====================
            RememberWordDto result =
                    dictionaryService.addRememberWord(email, minimalDto);

            // =====================
            // Then
            // =====================
            assertNotNull(result,
                    "Ngay cả khi DTO 'trống', service vẫn phải trả về DTO (không null)");
            assertSame(minimalDto, result,
                    "Service phải trả về chính DTO truyền vào, không clone");

            // verify save được gọi
            ArgumentCaptor<RememberWord> captor =
                    ArgumentCaptor.forClass(RememberWord.class);
            verify(repository, times(1)).save(captor.capture());

            RememberWord saved = captor.getValue();
            assertNotNull(saved,
                    "Entity truyền vào save() không được null");
            assertEquals(mockCustomer, saved.getCustomer(),
                    "Customer phải được gán cho RememberWord ngay cả trong edge case");

            // verify static mapper được gọi
            mockedStatic.verify(
                    () -> RememberTransform.toRememberWord(minimalDto),
                    times(1)
            );

            // verify validation đã gọi
            verify(validationResources, times(1))
                    .validateCustomerExist(email);
        }
    }
}
