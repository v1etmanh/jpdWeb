package com.jpd.web.nguyen.unit.service.customerService;

import com.jpd.web.model.Customer;
import com.jpd.web.model.Creator;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.service.CustomerService;
import com.jpd.web.dto.UserInfoDto;
import com.jpd.web.transform.CustomerTransform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho CustomerService.getOrCreateAccount(Jwt jwt)
 * <p>
 * Bao phủ các scenario:
 * <p>
 * HAPPY PATH:
 * - shouldReturnExistingCustomerInfo_whenCustomerAlreadyExists_andIsNotCreator
 * - shouldCreateNewCustomerAndReturnInfo_whenCustomerDoesNotExist_andIsNotCreator
 * - shouldReturnExistingCustomerInfoWithCreatorFlag_whenCustomerExistsAndIsCreator
 * <p>
 * EDGE CASE:
 * - shouldCreateNewCustomerWithEmailAsUsername_whenJwtDoesNotContainNameClaim
 * <p>
 * ERROR SCENARIO (contract violation / technical debt):
 * - shouldThrowNullPointerException_whenJwtDoesNotHaveEmailClaim
 * <p>
 * Kỹ thuật:
 * - Mock các repository và Jwt
 * - Mock static CustomerTransform.transToUserInfor(...)
 * - Dùng ArgumentCaptor để kiểm tra dữ liệu truyền vào save()
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceGetOrCreateAccountTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CreatorRepository creatorRepository;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        // MockitoExtension sẽ tự động init mocks, nên không cần openMocks()
    }

    /**
     * TC_HP_01:
     * shouldReturnExistingCustomerInfo_whenCustomerAlreadyExists_andIsNotCreator
     * <p>
     * Mô tả:
     * - Customer đã tồn tại trong DB (findByEmail() trả về Optional.of(existingCustomer)).
     * - Người dùng KHÔNG phải creator (creatorRepository.findByCustomer() trả về Optional.empty()).
     * - Service phải KHÔNG tạo mới customer, KHÔNG gọi save().
     * - Service phải gọi CustomerTransform.transToUserInfor(existingCustomer, false)
     * và trả đúng UserInfoDto.
     */
    @Test
    void shouldReturnExistingCustomerInfo_whenCustomerAlreadyExists_andIsNotCreator() {
        // ======================
        // Given
        // ======================
        String email = "user@example.com";

        when(jwt.getClaimAsString("email")).thenReturn(email);

        Customer existingCustomer = Customer.builder()
                .customerId(1L)
                .email(email)
                .username("Existing User")
                .givenName("Ex")
                .familyName("User")
                .role("USER")
                .build();

        when(customerRepository.findByEmail(email))
                .thenReturn(Optional.of(existingCustomer));

        // user không phải creator
        when(creatorRepository.findByCustomer(existingCustomer))
                .thenReturn(Optional.empty());

        // mock static mapper
        UserInfoDto dtoMock = mock(UserInfoDto.class);
        try (MockedStatic<CustomerTransform> mockedStatic =
                     Mockito.mockStatic(CustomerTransform.class)) {

            mockedStatic.when(() ->
                    CustomerTransform.transToUserInfor(existingCustomer, false)
            ).thenReturn(dtoMock);

            // ======================
            // When
            // ======================
            UserInfoDto result = customerService.getOrCreateAccount(jwt);

            // ======================
            // Then
            // ======================
            assertNotNull(result, "Service phải trả về UserInfoDto, không phải null");
            assertSame(dtoMock, result,
                    "Kết quả phải chính là dtoMock được trả ra từ CustomerTransform");

            // Không được tạo mới hay lưu lại customer
            verify(customerRepository, never()).save(any(Customer.class));

            // isCreator=false => gọi findByCustomer 1 lần
            verify(creatorRepository, times(1))
                    .findByCustomer(existingCustomer);

            // Mapper static gọi đúng tham số
            mockedStatic.verify(
                    () -> CustomerTransform.transToUserInfor(existingCustomer, false),
                    times(1)
            );
        }
    }

    /**
     * TC_HP_02:
     * shouldCreateNewCustomerAndReturnInfo_whenCustomerDoesNotExist_andIsNotCreator
     * <p>
     * Mô tả:
     * - Customer CHƯA tồn tại trong DB (findByEmail() -> Optional.empty()).
     * - Service phải gọi createNewCustomer(jwt), tức là:
     * + Đọc claim từ jwt
     * + Build Customer mới (role="USER", username lấy từ 'name')
     * + customerRepository.save(newCustomer) -> savedCustomer
     * - Sau đó check creatorRepository (isCreator = false)
     * - Cuối cùng gọi CustomerTransform.transToUserInfor(savedCustomer, false)
     * và trả về dtoMock.
     */
    @Test
    void shouldCreateNewCustomerAndReturnInfo_whenCustomerDoesNotExist_andIsNotCreator() {
        // ======================
        // Given
        // ======================
        String email = "newuser@example.com";
        String name = "New User";
        String givenName = "New";
        String familyName = "User";

        when(jwt.getClaimAsString("email")).thenReturn(email);
        when(jwt.getClaimAsString("name")).thenReturn(name);
        when(jwt.getClaimAsString("given_name")).thenReturn(givenName);
        when(jwt.getClaimAsString("family_name")).thenReturn(familyName);

        // Chưa có user này
        when(customerRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        // savedCustomer giả lập DB đã lưu và gán ID
        Customer savedCustomer = Customer.builder()
                .customerId(123L)
                .email(email)
                .username(name)        // username = name vì name != null
                .givenName(givenName)
                .familyName(familyName)
                .role("USER")
                .build();

        // Khi service gọi save(...), ta trả về savedCustomer
        when(customerRepository.save(any(Customer.class)))
                .thenReturn(savedCustomer);

        // User này KHÔNG phải creator
        when(creatorRepository.findByCustomer(savedCustomer))
                .thenReturn(Optional.empty());

        UserInfoDto dtoMock = mock(UserInfoDto.class);

        try (MockedStatic<CustomerTransform> mockedStatic =
                     Mockito.mockStatic(CustomerTransform.class)) {

            mockedStatic.when(() ->
                    CustomerTransform.transToUserInfor(savedCustomer, false)
            ).thenReturn(dtoMock);

            // ======================
            // When
            // ======================
            UserInfoDto result = customerService.getOrCreateAccount(jwt);

            // ======================
            // Then
            // ======================
            assertNotNull(result, "Phải trả về dto sau khi tạo mới");
            assertSame(dtoMock, result,
                    "Phải return dtoMock được mapper static build");

            // verify rằng service đã save 1 lần
            verify(customerRepository, times(1))
                    .save(any(Customer.class));

            // verify check creator
            verify(creatorRepository, times(1))
                    .findByCustomer(savedCustomer);

            mockedStatic.verify(
                    () -> CustomerTransform.transToUserInfor(savedCustomer, false),
                    times(1)
            );
        }
    }

    /**
     * TC_HP_03:
     * shouldReturnExistingCustomerInfoWithCreatorFlag_whenCustomerExistsAndIsCreator
     * <p>
     * Mô tả:
     * - Customer đã tồn tại trong DB
     * - creatorRepository.findByCustomer(...) trả về Optional.of(creator)
     * => isCreator = true
     * - Mapper được gọi với tham số isCreator = true
     */
    @Test
    void shouldReturnExistingCustomerInfoWithCreatorFlag_whenCustomerExistsAndIsCreator() {
        // ======================
        // Given
        // ======================
        String email = "pro@example.com";
        when(jwt.getClaimAsString("email")).thenReturn(email);

        Customer existingCustomer = Customer.builder()
                .customerId(7L)
                .email(email)
                .username("Pro User")
                .givenName("Pro")
                .familyName("User")
                .role("USER")
                .build();

        when(customerRepository.findByEmail(email))
                .thenReturn(Optional.of(existingCustomer));

        // Mock creator tồn tại -> isCreator = true
        Creator creatorMock = mock(Creator.class);
        when(creatorRepository.findByCustomer(existingCustomer))
                .thenReturn(Optional.of(creatorMock));

        UserInfoDto dtoMock = mock(UserInfoDto.class);

        try (MockedStatic<CustomerTransform> mockedStatic =
                     Mockito.mockStatic(CustomerTransform.class)) {

            mockedStatic.when(() ->
                    CustomerTransform.transToUserInfor(existingCustomer, true)
            ).thenReturn(dtoMock);

            // ======================
            // When
            // ======================
            UserInfoDto result = customerService.getOrCreateAccount(jwt);

            // ======================
            // Then
            // ======================
            assertNotNull(result);
            assertSame(dtoMock, result,
                    "Khi user là creator, mapper phải được gọi với isCreator=true");

            verify(customerRepository, never()).save(any(Customer.class));
            verify(creatorRepository).findByCustomer(existingCustomer);

            mockedStatic.verify(
                    () -> CustomerTransform.transToUserInfor(existingCustomer, true),
                    times(1)
            );
        }
    }

    /**
     * TC_EC_01:
     * shouldCreateNewCustomerWithEmailAsUsername_whenJwtDoesNotContainNameClaim
     * <p>
     * Edge case:
     * - Jwt không có claim 'name' -> getClaimAsString("name") trả về null
     * - Logic trong createNewCustomer():
     * username = (name != null ? name : email)
     * => phải fallback username = email
     * <p>
     * Ta cần:
     * - customerRepository.findByEmail(email) -> Optional.empty()  (chưa tồn tại)
     * - save(...) được gọi với customer.username == email
     * - Sau đó creatorRepository trả Optional.empty(), isCreator=false
     * - Mapper static trả về dtoMock
     */
    @Test
    void shouldCreateNewCustomerWithEmailAsUsername_whenJwtDoesNotContainNameClaim() {
        // ======================
        // Given
        // ======================
        String email = "no-name@example.com";
        String givenName = "GivenX";
        String familyName = "FamilyY";

        when(jwt.getClaimAsString("email")).thenReturn(email);
        when(jwt.getClaimAsString("name")).thenReturn(null); // <-- name missing
        when(jwt.getClaimAsString("given_name")).thenReturn(givenName);
        when(jwt.getClaimAsString("family_name")).thenReturn(familyName);

        // Chưa có account trong DB
        when(customerRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        // Chuẩn bị "savedCustomer" mà repo sẽ trả về sau save()
        Customer savedCustomer = Customer.builder()
                .customerId(999L)
                .email(email)
                .username(email)          // fallback name -> email
                .givenName(givenName)
                .familyName(familyName)
                .role("USER")
                .build();

        // Ta muốn vừa capture tham số truyền vào save(), vừa return savedCustomer
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        when(customerRepository.save(customerCaptor.capture()))
                .thenReturn(savedCustomer);

        // Người dùng KHÔNG phải creator
        when(creatorRepository.findByCustomer(savedCustomer))
                .thenReturn(Optional.empty());

        UserInfoDto dtoMock = mock(UserInfoDto.class);

        try (MockedStatic<CustomerTransform> mockedStatic =
                     Mockito.mockStatic(CustomerTransform.class)) {

            mockedStatic.when(() ->
                    CustomerTransform.transToUserInfor(savedCustomer, false)
            ).thenReturn(dtoMock);

            // ======================
            // When
            // ======================
            UserInfoDto result = customerService.getOrCreateAccount(jwt);

            // ======================
            // Then
            // Kiểm tra DTO trả về
            assertNotNull(result);
            assertSame(dtoMock, result,
                    "Service phải trả về dto từ mapper static");

            // Kiểm tra đối tượng được truyền vào save() trước khi DB 'gắn id'
            Customer newCustomerBeforeSave = customerCaptor.getValue();
            assertNotNull(newCustomerBeforeSave, "Phải tạo Customer mới để lưu");

            // Fallback username logic
            assertEquals(email, newCustomerBeforeSave.getEmail(),
                    "Email trong customer mới phải bằng email từ JWT");
            assertEquals(email, newCustomerBeforeSave.getUsername(),
                    "Vì JWT không có 'name', username phải fallback = email");
            assertEquals(givenName, newCustomerBeforeSave.getGivenName(),
                    "givenName phải lấy từ JWT");
            assertEquals(familyName, newCustomerBeforeSave.getFamilyName(),
                    "familyName phải lấy từ JWT");
            assertEquals("USER", newCustomerBeforeSave.getRole(),
                    "Role mặc định phải là USER");

            // verify creator check gọi đúng savedCustomer (sau save)
            verify(creatorRepository, times(1))
                    .findByCustomer(savedCustomer);

            mockedStatic.verify(
                    () -> CustomerTransform.transToUserInfor(savedCustomer, false),
                    times(1)
            );
        }
    }

    /**
     * TC_ES_01 (Technical debt scenario):
     * shouldThrowNullPointerException_whenJwtDoesNotHaveEmailClaim
     * <p>
     * Mô tả:
     * - jwt.getClaimAsString("email") trả về null.
     * - Code gọi customerRepository.findByEmail(email) với email=null.
     * - Vì đây là mock, nếu không stub, mặc định trả về null thay vì Optional.
     * - Sau đó code cố gọi .orElseGet(...) trên null => NullPointerException.
     * <p>
     * Đây là một case "input contract violated".
     * Thường trong thực tế, hệ thống auth bảo đảm email luôn có.
     * Test này nhằm highlight việc service KHÔNG handle trường hợp thiếu email.
     */
//    @Test
//    void shouldThrowNullPointerException_whenJwtDoesNotHaveEmailClaim() {
//        // ======================
//        // Given
//        // ======================
//        when(jwt.getClaimAsString("email")).thenReturn(null);
//
//        // KHÔNG stub customerRepository.findByEmail(null)
//        // -> Mockito default trả về null (không phải Optional)
//        // -> sẽ gây NullPointerException khi .orElseGet(...) được gọi
//
//        // ======================
//        // When / Then
//        // ======================
//        assertThrows(
//                NullPointerException.class,
//                () -> customerService.getOrCreateAccount(jwt),
//                "Thiếu email trong JWT sẽ gây NullPointerException do code không bảo vệ trường hợp này"
//        );
//
//        // save() chắc chắn chưa được gọi
//        verify(customerRepository, never()).save(any(Customer.class));
//        // creatorRepository cũng chưa được gọi vì fail quá sớm
//        verifyNoInteractions(creatorRepository);
//    }
}
