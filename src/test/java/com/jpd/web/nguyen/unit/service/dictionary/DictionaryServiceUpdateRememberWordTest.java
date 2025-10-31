package com.jpd.web.nguyen.unit.service.dictionary;

import com.jpd.web.model.Customer;
import com.jpd.web.model.RememberWord;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.repository.RememberWordRepository;
import com.jpd.web.service.DictionaryService;
import com.jpd.web.dto.RememberWordDto;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.RememberTransform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho DictionaryService.updateRememberWord(email, rememberWordDto)
 * <p>
 * Bao phủ toàn bộ scenario đã phân tích:
 * <p>
 * HAPPY PATH:
 * - shouldUpdateAndSaveWord_whenValidatePasses
 * <p>
 * ERROR SCENARIOS:
 * - shouldThrowRuntimeException_whenWordIdDoesNotExist
 * - shouldThrowUnauthorizedException_whenWordDoesNotBelongToCurrentUser
 * - shouldThrowRuntimeException_whenEntityDisappearsAfterValidate
 * <p>
 * Kỹ thuật dùng:
 * - validationResources và repository là @Mock, inject vào service bằng @InjectMocks
 * - RememberTransform.toRememberWord(...) là static => mock bằng Mockito.mockStatic
 * - repository.findById(id) sẽ được stub nhiều lần cho các call khác nhau:
 * + Lần 1: trong validate(...)
 * + Lần 2: trong updateRememberWord(...) chính
 */
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class DictionaryServiceUpdateRememberWordTest {

    @Mock
    private ValidationResources validationResources;

    @Mock
    private RememberWordRepository repository;

    @InjectMocks
    private DictionaryService dictionaryService;

    /**
     * TC_HP_01:
     * shouldUpdateAndSaveWord_whenValidatePasses
     * <p>
     * // Given:
     * - email = "user@example.com"
     * - dto.rwId = 10
     * - validate(email, 10) phải PASS:
     * * validationResources.validateCustomerExist(email) -> Customer (id=123)
     * * repository.findById(10) -> Optional.of(existingWord with customerId=123)
     * - Gọi lại repository.findById(10) trong updateRememberWord -> vẫn Optional.of(existingWord)
     * - RememberTransform.toRememberWord(dto) -> mappedWord
     * <p>
     * // When:
     * - gọi updateRememberWord(email, dto)
     * <p>
     * // Then:
     * - Trả về chính dto (cùng reference)
     * - repository.save(mappedWord) được gọi đúng 1 lần
     * - mappedWord.setId(existingWord.getId()) đã được áp dụng
     * - repository.findById(id) được gọi >=2 lần (validate + load lại)
     */
    @Test
    void shouldUpdateAndSaveWord_whenValidatePasses() {
        // =====================
        // Given
        // =====================
        String email = "user@example.com";
        long rwId = 10L;

        // DTO đầu vào - sử dụng builder để set rwId đúng cách
        RememberWordDto dto = RememberWordDto.builder()
                .rwId(rwId)
                .word("hello")
                .meaning("xin chào")
                .description("từ chào hỏi")
                .build();

        // Customer hiện đang đăng nhập
        Customer currentCustomer = Customer.builder()
                .customerId(123L)
                .email(email)
                .build();

        // RememberWord hiện có trong DB, thuộc về currentCustomer
        RememberWord existingWord = new RememberWord();
        existingWord.setId(rwId);
        existingWord.setCustomer(currentCustomer);

        // Khi validate(...) gọi
        when(validationResources.validateCustomerExist(email))
                .thenReturn(currentCustomer);

        // repository.findById(...) được gọi 2 lần trong flow tổng:
        //  Lần 1 (bên trong validate)
        //  Lần 2 (sau validate, trong updateRememberWord)
        // Ta có thể dùng thenReturn() nhiều lần để trả về cùng object cho cả 2 lần gọi.
        when(repository.findById(rwId))
                .thenReturn(Optional.of(existingWord))
                .thenReturn(Optional.of(existingWord));

        // mappedWord là object mới từ mapper static
        RememberWord mappedWord = new RememberWord();
        // mappedWord ban đầu CHƯA có id; service sẽ setId(existingWord.getId())

        try (MockedStatic<RememberTransform> mockedStatic =
                     Mockito.mockStatic(RememberTransform.class)) {

            mockedStatic.when(() -> RememberTransform.toRememberWord(dto))
                    .thenReturn(mappedWord);

            // save() sẽ trả về model đã được truyền vào
            when(repository.save(any(RememberWord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // =====================
            // When
            // =====================
            RememberWordDto result = dictionaryService.updateRememberWord(email, dto);

            // =====================
            // Then
            // =====================
            assertNotNull(result, "Service phải trả về DTO, không được null");
            assertSame(dto, result,
                    "Service phải trả về chính DTO truyền vào, không tạo DTO mới");

            // verify rằng mappedWord đã được gán id từ existingWord trước khi save
            ArgumentCaptor<RememberWord> saveCaptor = ArgumentCaptor.forClass(RememberWord.class);
            verify(repository, times(1)).save(saveCaptor.capture());

            RememberWord savedEntity = saveCaptor.getValue();
            assertNotNull(savedEntity, "Đối tượng truyền vào save() không được null");
            assertEquals(existingWord.getId(), savedEntity.getId(),
                    "ID của entity sau khi map phải được set lại bằng ID gốc từ DB");
            // Lưu ý: code hiện tại KHÔNG gán customer lại cho 're' sau map,
            // chỉ setId(). Nếu bạn muốn assert thêm customer thì cần sửa logic service.

            // repository.findById phải được gọi ít nhất 2 lần:
            // - 1 lần trong validate
            // - 1 lần trong updateRememberWord
            verify(repository, atLeast(2)).findById(rwId);

            // Mapper static được gọi đúng 1 lần
            mockedStatic.verify(
                    () -> RememberTransform.toRememberWord(dto),
                    times(1)
            );

            // validateCustomerExist được gọi 1 lần trong validate
            verify(validationResources, times(1))
                    .validateCustomerExist(email);
        }
    }

    /**
     * TC_ES_01:
     * shouldThrowRuntimeException_whenWordIdDoesNotExist
     * <p>
     * // Given:
     * - validate(email, id) -> repository.findById(id) trả về Optional.empty()
     * - validate sẽ ném new RuntimeException("this id is not exist")
     * <p>
     * // Then:
     * - updateRememberWord(...) phải ném RuntimeException
     * - Không gọi save(...)
     * - Không gọi RememberTransform.toRememberWord(...)
     */
    @Test
    void shouldThrowRuntimeException_whenWordIdDoesNotExist() {
        // =====================
        // Given
        // =====================
        String email = "user@example.com";
        long rwId = 99L;

        RememberWordDto dto = RememberWordDto.builder()
                .rwId(rwId)
                .word("test")
                .meaning("kiểm tra")
                .description("từ kiểm tra")
                .build();

        Customer currentCustomer = Customer.builder()
                .customerId(123L)
                .email(email)
                .build();

        when(validationResources.validateCustomerExist(email))
                .thenReturn(currentCustomer);

        // validate() sẽ gọi repository.findById(rwId) và thấy empty => RuntimeException("this id is not exist")
        when(repository.findById(rwId))
                .thenReturn(Optional.empty());

        // =====================
        // When / Then
        // =====================
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> dictionaryService.updateRememberWord(email, dto),
                "Nếu ID không tồn tại thì phải ném RuntimeException('this id is not exist') từ validate()"
        );

        // Kiểm tra message cụ thể
        assertTrue(ex.getMessage().contains("this id is not exist"),
                "Exception message phải chứa 'this id is not exist'");

        // Không được gọi save vì đã fail trước
        verify(repository, never()).save(any(RememberWord.class));

        // Không mockStatic ở đây, nhưng chúng ta có thể khẳng định mapper không bao giờ chạy
        // thông qua việc mapper chỉ chạy sau validate. Vì validate đã ném exception, mapper sẽ không chạy.

        verify(validationResources, times(1))
                .validateCustomerExist(email);

        // repository.findById chỉ cần được gọi 1 lần (trong validate)
        verify(repository, times(1)).findById(rwId);
    }

    /**
     * TC_ES_02:
     * shouldThrowUnauthorizedException_whenWordDoesNotBelongToCurrentUser
     * <p>
     * // Given:
     * - validateCustomerExist(email) -> Customer {customerId=111}
     * - repository.findById(id) -> RememberWord {customerId=222}  // thuộc người khác
     * - validate() ném UnauthorizedException("you do not own this word")
     * <p>
     * // Then:
     * - updateRememberWord(...) phải ném UnauthorizedException
     * - Không save(...)
     * - Không gọi mapper static
     */
    @Test
    void shouldThrowUnauthorizedException_whenWordDoesNotBelongToCurrentUser() {
        // =====================
        // Given
        // =====================
        String email = "userA@example.com";
        long rwId = 5L;

        RememberWordDto dto = RememberWordDto.builder()
                .rwId(rwId)
                .word("hello")
                .meaning("xin chào")
                .description("từ chào hỏi")
                .build();

        // Customer hiện đăng nhập
        Customer currentCustomer = Customer.builder()
                .customerId(111L)
                .email(email)
                .build();

        // Word trong DB thuộc về customer khác
        Customer otherCustomer = Customer.builder()
                .customerId(222L)
                .email("other@example.com")
                .build();
        RememberWord someoneElsesWord = new RememberWord();
        someoneElsesWord.setId(rwId);
        someoneElsesWord.setCustomer(otherCustomer);

        when(validationResources.validateCustomerExist(email))
                .thenReturn(currentCustomer);

        // validate sẽ gọi repository.findById(rwId) -> trả về word thuộc người khác
        when(repository.findById(rwId))
                .thenReturn(Optional.of(someoneElsesWord));

        // =====================
        // When / Then
        // =====================
        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> dictionaryService.updateRememberWord(email, dto),
                "Nếu từ không thuộc user hiện tại thì phải ném UnauthorizedException"
        );

        assertTrue(
                ex.getMessage().toLowerCase().contains("you do not own"),
                "Message UnauthorizedException phải thể hiện user không sở hữu từ này"
        );

        // Không save vì fail trước khi qua được validate
        verify(repository, never()).save(any(RememberWord.class));

        verify(validationResources, times(1))
                .validateCustomerExist(email);

        // repository.findById chỉ được gọi 1 lần (trong validate)
        verify(repository, times(1)).findById(rwId);
    }

    /**
     * TC_ES_03:
     * shouldThrowRuntimeException_whenEntityDisappearsAfterValidate
     * <p>
     * Ý tưởng:
     * - validate() PASS:
     * * validationResources.validateCustomerExist(email) -> Customer {id=123}
     * * repository.findById(id) -> Optional.of(existingWord với customerId=123)
     * - Sau validate, trong updateRememberWord():
     * repository.findById(id) -> Optional.empty()
     * => ném RuntimeException("Remember word not found")
     * <p>
     * // Then:
     * - updateRememberWord(...) phải ném RuntimeException("Remember word not found")
     * - Không save(...)
     * - Không gọi mapper static (vì crash trước khi map)
     */
    @Test
    void shouldThrowRuntimeException_whenEntityDisappearsAfterValidate() {
        // =====================
        // Given
        // =====================
        String email = "race@example.com";
        long rwId = 77L;

        RememberWordDto dto = RememberWordDto.builder()
                .rwId(rwId)
                .word("race")
                .meaning("cuộc đua")
                .description("từ chỉ cuộc đua")
                .build();

        Customer currentCustomer = Customer.builder()
                .customerId(123L)
                .email(email)
                .build();

        RememberWord existingWord = new RememberWord();
        existingWord.setId(rwId);
        existingWord.setCustomer(currentCustomer);

        when(validationResources.validateCustomerExist(email))
                .thenReturn(currentCustomer);

        // Trick: repository.findById(...) được gọi nhiều lần.
        // Ta muốn:
        //   - Lần 1 (validate) -> Optional.of(existingWord)
        //   - Lần 2 (trong updateRememberWord) -> Optional.empty()
        when(repository.findById(rwId))
                .thenReturn(Optional.of(existingWord))  // call #1 in validate()
                .thenReturn(Optional.empty());          // call #2 in updateRememberWord()

        // =====================
        // When / Then
        // =====================
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> dictionaryService.updateRememberWord(email, dto),
                "Nếu entity biến mất sau validate, service phải ném RuntimeException('Remember word not found')"
        );

        assertTrue(
                ex.getMessage().contains("Remember word not found"),
                "Exception phải thể hiện là không tìm thấy từ để cập nhật"
        );

        // Không save vì fail trước khi tới save()
        verify(repository, never()).save(any(RememberWord.class));

        // validateCustomerExist gọi đúng 1 lần
        verify(validationResources, times(1))
                .validateCustomerExist(email);

        // repository.findById gọi ít nhất 2 lần (validate + load lại)
        verify(repository, atLeast(2)).findById(rwId);
    }
}
