package com.jpd.web.UnitTest;

import com.jpd.web.dto.CreatorDto;
import com.jpd.web.exception.*;
import com.jpd.web.model.*;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.WithdrawRepository;
import com.jpd.web.service.*;
import com.jpd.web.service.utils.ValidationResources;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho CreatorService - SỬ DỤNG DDT VỚI FILE CSV
 */
@ExtendWith(MockitoExtension.class)
class CreatorServiceTest {

    @InjectMocks
    private CreatorService creatorService;

    @Mock private ValidationResources validationResources;
    @Mock private PayPalPayoutServiceV2 palPayoutServiceV2;
    @Mock private FireBaseService fireBaseService;
    @Mock private WithdrawRepository withdrawRepository;
    @Mock private CreatorRepository creatorRepository;

    private Creator sampleCreator;

    @BeforeEach
    void setup() {
        sampleCreator = Creator.builder()
                .creatorId(1L)
                .fullName("John Doe")
                .paymentEmail("john@paypal.com")
                .balance(200)
                .status(Status.SUCCESS)
                .certificateUrl(new ArrayList<>(List.of("cert.pdf")))
                .build();
    }

    // ===================================================================
    // TEST getAccount() - DDT
    // ===================================================================

    @ParameterizedTest(name = "[{index}] ID={0} → {1}")
    @CsvFileSource(resources = "/data/getAccount_invalid_ids.csv", numLinesToSkip = 1)
    void testGetAccount_InvalidIds(Long creatorId, String exceptionClassName) throws Exception {
        // Chuyển String → Class<? extends Throwable>
        Class<? extends Throwable> expectedException = Class.forName(exceptionClassName)
                .asSubclass(Throwable.class);

        // Mock hành vi validateCreatorExists
        if (creatorId == null) {
            when(validationResources.validateCreatorExists(null))
                    .thenThrow(new IllegalArgumentException("Creator ID cannot be null"));
        } else {
            when(validationResources.validateCreatorExists(creatorId))
                    .thenThrow(new CreatorNotFoundException(creatorId));
        }

        // Kiểm tra exception
        assertThrows(expectedException, () -> creatorService.getAccount(creatorId));
    }

    @Test
    @DisplayName("getAccount() → trả về CreatorDto khi creator tồn tại")
    void testGetAccount_Success() {
        when(validationResources.validateCreatorExists(1L)).thenReturn(sampleCreator);

        CreatorDto dto = creatorService.getAccount(1L);

        assertEquals("John Doe", dto.getFullName());
        verify(validationResources).validateCreatorExists(1L);
    }

    @Test
    @DisplayName("getAccount() → trả về đúng creator khác")
    void testGetAccount_DifferentCreator() {
        Creator c2 = Creator.builder().creatorId(2L).fullName("Jane").build();
        when(validationResources.validateCreatorExists(2L)).thenReturn(c2);

        CreatorDto dto = creatorService.getAccount(2L);
        assertEquals("Jane", dto.getFullName());
    }

    // ===================================================================
    // TEST createWithdraw() - DDT
    // ===================================================================

    @ParameterizedTest(name = "[{index}] creatorId={0}, amount={1} → WithdrawException")
    @CsvFileSource(resources = "/data/withdraw_invalid_amounts.csv", numLinesToSkip = 1)
    void testCreateWithdraw_InvalidAmount(Long creatorId, double amount) {
        when(validationResources.validateCreatorExists(creatorId)).thenReturn(sampleCreator);

        assertThrows(WithdrawException.class, () -> creatorService.createWithdraw(creatorId, amount));
    }

    @Test
    @DisplayName("createWithdraw() → thành công khi hợp lệ")
    void testCreateWithdraw_Success() throws Exception {
        when(validationResources.validateCreatorExists(1L)).thenReturn(sampleCreator);
        when(palPayoutServiceV2.createSinglePayout(any(), anyDouble(), anyString(), anyString(), any(), any()))
                .thenReturn(new PayoutTracking("batch", "test@paypal.com", 100.0, "USD", "note"));
        when(withdrawRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Withdraw result = creatorService.createWithdraw(1L, 50);

        assertEquals(Status.PENDING, result.getStatus());
        assertEquals(150, sampleCreator.getBalance());
        verify(withdrawRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("createWithdraw() → ném khi thiếu paymentEmail")
    void testCreateWithdraw_MissingPaymentEmail() {
        sampleCreator.setPaymentEmail(null);
        when(validationResources.validateCreatorExists(1L)).thenReturn(sampleCreator);

        assertThrows(WithdrawException.class, () -> creatorService.createWithdraw(1L, 50));
    }

    @Test
    @DisplayName("createWithdraw() → ném khi PayPal lỗi")
    void testCreateWithdraw_PayoutFailure() throws Exception {
        when(validationResources.validateCreatorExists(1L)).thenReturn(sampleCreator);
        when(palPayoutServiceV2.createSinglePayout(any(), anyDouble(), anyString(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("PayPal error"));

        assertThrows(WithdrawException.class, () -> creatorService.createWithdraw(1L, 50));
    }

    @Test
    @DisplayName("createWithdraw() → giảm balance đúng")
    void testCreateWithdraw_BalanceDecreased() throws Exception {
        when(validationResources.validateCreatorExists(1L)).thenReturn(sampleCreator);
        when(palPayoutServiceV2.createSinglePayout(any(), anyDouble(), anyString(), anyString(), any(), any()))
                .thenReturn(new PayoutTracking("batch", "test@paypal.com", 100.0, "USD", "note"));
        when(withdrawRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        creatorService.createWithdraw(1L, 100);
        assertEquals(100, sampleCreator.getBalance());
    }

    // ===================================================================
    // TEST uploadCertificate() - DDT
    // ===================================================================

    @ParameterizedTest(name = "[{index}] creatorId={0}, succeed={1}")
    @CsvFileSource(resources = "/data/certificate_upload_cases.csv", numLinesToSkip = 1)
    void testUploadCertificate(Long creatorId, boolean shouldSucceed, String expectedUrl) throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(validationResources.validateCreatorExists(creatorId)).thenReturn(sampleCreator);

        if (shouldSucceed) {
            // expectedUrl luôn có giá trị hợp lệ (ví dụ "firebase-url")
            when(fireBaseService.uploadFile(any(), any())).thenReturn(expectedUrl);
            when(creatorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            creatorService.upLoadCertificate(creatorId, file);   // <-- ĐÃ SỬA tên method

            assertTrue(sampleCreator.getCertificateUrl().contains(expectedUrl));
            verify(creatorRepository, times(1)).save(any());
        } else {
            // expectedUrl = "-" → không dùng, chỉ cần ném exception
            when(fireBaseService.uploadFile(any(), any()))
                    .thenThrow(new IOException("upload error"));

            assertThrows(IOException.class, () -> creatorService.upLoadCertificate(creatorId, file));
        }
    }
}