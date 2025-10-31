package com.jpd.web.nguyen.unit.service.dictionary;

import com.jpd.web.model.Customer;
import com.jpd.web.model.RememberWord;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.repository.RememberWordRepository;
import com.jpd.web.service.DictionaryService;
import com.jpd.web.service.utils.ValidationResources; // chỉnh lại package nếu khác
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho DictionaryService.deleteRememberWord(email, id)
 *
 * Bao phủ các scenario:
 *
 *  HAPPY PATH:
 *    - shouldDeleteRememberWord_whenUserOwnsWord
 *
 *  ERROR SCENARIOS:
 *    - shouldThrowRuntimeException_whenWordDoesNotExist
 *    - shouldThrowUnauthorizedException_whenWordBelongsToDifferentUser
 *
 * Ghi chú:
 *  - deleteRememberWord() gọi validate(email, id) trước,
 *    validate() sẽ dùng validationResources và repository.findById(id).
 *  - Nếu validate pass -> service gọi repository.deleteById(id).
 *  - Nếu validate ném exception -> KHÔNG được gọi deleteById.
 */
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class DictionaryServiceDeleteRememberWordTest {

    @Mock
    private ValidationResources validationResources;

    @Mock
    private RememberWordRepository repository;

    @InjectMocks
    private DictionaryService dictionaryService;

    /**
     * TC_HP_01:
     * shouldDeleteRememberWord_whenUserOwnsWord
     *
     * // Given:
     *   - email = "user@example.com"
     *   - id = 10
     *   - validationResources.validateCustomerExist(email) -> Customer {customerId=123}
     *   - repository.findById(10) -> Optional.of(rememberWord) với rememberWord.customer.customerId=123
     *
     * // When:
     *   - gọi dictionaryService.deleteRememberWord(email, id)
     *
     * // Then:
     *   - Không ném exception
     *   - repository.deleteById(id) được gọi đúng 1 lần
     *   - validationResources.validateCustomerExist(email) được gọi đúng 1 lần
     *   - repository.findById(id) được gọi đúng 1 lần (trong validate)
     */
    @Test
    void shouldDeleteRememberWord_whenUserOwnsWord() {
        // =====================
        // Given
        // =====================
        String email = "user@example.com";
        long id = 10L;

        // Customer hiện tại (đang đăng nhập)
        Customer currentCustomer = Customer.builder()
                .customerId(123L)
                .email(email)
                .build();

        // RememberWord trong DB, thuộc về currentCustomer
        RememberWord ownedWord = new RememberWord();
        ownedWord.setId(id);
        ownedWord.setCustomer(currentCustomer);

        // Stub validateCustomerExist(email)
        when(validationResources.validateCustomerExist(email))
                .thenReturn(currentCustomer);

        // validate() sẽ gọi repository.findById(id)
        when(repository.findById(id))
                .thenReturn(Optional.of(ownedWord));

        // =====================
        // When
        // =====================
        assertDoesNotThrow(
                () -> dictionaryService.deleteRememberWord(email, id),
                "Khi user sở hữu từ đó, xóa phải không ném exception"
        );

        // =====================
        // Then
        // =====================
        // Sau khi validate pass, service phải gọi deleteById(id)
        verify(repository, times(1)).deleteById(id);

        // validateCustomerExist phải được gọi đúng email truyền vào
        verify(validationResources, times(1))
                .validateCustomerExist(email);

        // findById được gọi 1 lần (trong validate)
        verify(repository, times(1)).findById(id);

        // Không có thêm tương tác bất ngờ
        verifyNoMoreInteractions(repository, validationResources);
    }

    /**
     * TC_ES_01:
     * shouldThrowRuntimeException_whenWordDoesNotExist
     *
     * // Given:
     *   - email = "user@example.com"
     *   - id = 99
     *   - validationResources.validateCustomerExist(email) -> Customer {customerId=123}
     *   - repository.findById(99) -> Optional.empty()
     *     => validate() ném RuntimeException("this id is not exist")
     *
     * // When / Then:
     *   - Gọi deleteRememberWord(email, id) phải ném RuntimeException
     *   - KHÔNG gọi repository.deleteById(id)
     */
    @Test
    void shouldThrowRuntimeException_whenWordDoesNotExist() {
        // =====================
        // Given
        // =====================
        String email = "user@example.com";
        long id = 99L;

        Customer currentCustomer = Customer.builder()
                .customerId(123L)
                .email(email)
                .build();

        when(validationResources.validateCustomerExist(email))
                .thenReturn(currentCustomer);

        // findById() trả về empty => validate sẽ ném RuntimeException("this id is not exist")
        when(repository.findById(id))
                .thenReturn(Optional.empty());

        // =====================
        // When / Then
        // =====================
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> dictionaryService.deleteRememberWord(email, id),
                "Nếu từ không tồn tại, service phải ném RuntimeException('this id is not exist')"
        );

        // (Tuỳ chọn) kiểm tra message
        assertTrue(
                ex.getMessage().toLowerCase().contains("this id is not exist"),
                "Message exception phải mô tả rằng id không tồn tại"
        );

        // Vì fail trong validate => KHÔNG được phép gọi deleteById
        verify(repository, never()).deleteById(anyLong());

        verify(validationResources, times(1))
                .validateCustomerExist(email);

        // findById được gọi đúng 1 lần trong validate
        verify(repository, times(1)).findById(id);

        verifyNoMoreInteractions(repository, validationResources);
    }

    /**
     * TC_ES_02:
     * shouldThrowUnauthorizedException_whenWordBelongsToDifferentUser
     *
     * // Given:
     *   - email = "userA@example.com"
     *   - id = 7
     *   - validationResources.validateCustomerExist(email) -> Customer {customerId=111}
     *   - repository.findById(7) -> RememberWord {customerId=222} // thuộc user khác
     *     => validate() ném UnauthorizedException("you do not own this word")
     *
     * // When / Then:
     *   - deleteRememberWord(email, id) phải ném UnauthorizedException
     *   - KHÔNG gọi repository.deleteById(id)
     */
    @Test
    void shouldThrowUnauthorizedException_whenWordBelongsToDifferentUser() {
        // =====================
        // Given
        // =====================
        String email = "userA@example.com";
        long id = 7L;

        // Customer hiện tại
        Customer currentCustomer = Customer.builder()
                .customerId(111L)
                .email(email)
                .build();

        // Word trong DB nhưng thuộc user khác (customerId=222)
        Customer otherCustomer = Customer.builder()
                .customerId(222L)
                .email("other@example.com")
                .build();

        RememberWord someoneElsesWord = new RememberWord();
        someoneElsesWord.setId(id);
        someoneElsesWord.setCustomer(otherCustomer);

        // Stub validateCustomerExist để trả về currentCustomer
        when(validationResources.validateCustomerExist(email))
                .thenReturn(currentCustomer);

        // validate() gọi repository.findById(id) -> trả về word thuộc người khác
        when(repository.findById(id))
                .thenReturn(Optional.of(someoneElsesWord));

        // =====================
        // When / Then
        // =====================
        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> dictionaryService.deleteRememberWord(email, id),
                "Nếu từ thuộc user khác, service phải ném UnauthorizedException"
        );

        assertTrue(
                ex.getMessage().toLowerCase().contains("you do not own this word"),
                "Thông báo lỗi phải thể hiện rằng user không sở hữu từ này"
        );

        // Không được xóa DB vì không có quyền
        verify(repository, never()).deleteById(anyLong());

        // Cả hai dependency đã được gọi đúng trong validate
        verify(validationResources, times(1))
                .validateCustomerExist(email);
        verify(repository, times(1)).findById(id);

        verifyNoMoreInteractions(repository, validationResources);
    }
}
