package com.jpd.web.UnitTest;


import com.jpd.web.dto.UserInfoDto;
import com.jpd.web.model.Customer;
import com.jpd.web.transform.CustomerTransform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.NullSource;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerTransformTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @ParameterizedTest
    @CsvFileSource(resources = "/customer-transform-test-data.csv", numLinesToSkip = 1)
    @DisplayName("Should transform Customer to UserInfoDto with various data and isCreator flag")
    void testTransToUserInfor(String username, String email, String familyName, String givenName,
                              String role, String createDateStr, boolean isCreator) {
        LocalDate localDate = LocalDate.parse(createDateStr, FORMATTER);
        Date createDate = Date.valueOf(localDate);

        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getUsername()).thenReturn(username);
        when(mockCustomer.getEmail()).thenReturn(email);
        when(mockCustomer.getFamilyName()).thenReturn(familyName);
        when(mockCustomer.getGivenName()).thenReturn(givenName);
        when(mockCustomer.getRole()).thenReturn(role);
        when(mockCustomer.getCreateDate()).thenReturn(createDate);

        UserInfoDto result = CustomerTransform.transToUserInfor(mockCustomer, isCreator);

        assertNotNull(result);
        assertEquals(username, result.getUserName());
        assertEquals(email, result.getEmail());
        assertEquals(familyName, result.getFamilyName());
        assertNull(result.getGivenName());
        assertEquals(role, result.getRole());
        assertEquals(createDate, result.getCreateDate());
        assertEquals(isCreator, result.isCreator());

        verify(mockCustomer, times(1)).getUsername();
        verify(mockCustomer, times(1)).getEmail();
        verify(mockCustomer, times(1)).getFamilyName();
        verify(mockCustomer, never()).getGivenName();
        verify(mockCustomer, times(1)).getRole();
        verify(mockCustomer, times(1)).getCreateDate();
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw NullPointerException when Customer is null")
    void testTransToUserInfor_NullCustomer(Customer customer) {
        assertThrows(NullPointerException.class, () -> {
            CustomerTransform.transToUserInfor(customer, true);
        });
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/customer-transform-creator-flag-test-data.csv", numLinesToSkip = 1)
    @DisplayName("Should correctly set isCreator flag for both true and false scenarios")
    void testTransToUserInfor_CreatorFlag(String username, String email, String familyName, String givenName,
                                          String role, String createDateStr, boolean isCreator) {
        LocalDate localDate = LocalDate.parse(createDateStr, FORMATTER);
        Date createDate = Date.valueOf(localDate);

        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getUsername()).thenReturn(username);
        when(mockCustomer.getEmail()).thenReturn(email);
        when(mockCustomer.getFamilyName()).thenReturn(familyName);
        when(mockCustomer.getGivenName()).thenReturn(givenName);
        when(mockCustomer.getRole()).thenReturn(role);
        when(mockCustomer.getCreateDate()).thenReturn(createDate);

        UserInfoDto result = CustomerTransform.transToUserInfor(mockCustomer, isCreator);

        assertNotNull(result);
        assertEquals(isCreator, result.isCreator());
        assertEquals(username, result.getUserName());
        assertEquals(email, result.getEmail());
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/customer-transform-edge-cases-test-data.csv", numLinesToSkip = 1)
    @DisplayName("Should handle edge cases with boundary and special values")
    void testTransToUserInfor_EdgeCases(String username, String email, String familyName, String givenName,
                                        String role, String createDateStr, boolean isCreator) {
        LocalDate localDate = LocalDate.parse(createDateStr, FORMATTER);
        Date createDate = Date.valueOf(localDate);

        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getUsername()).thenReturn(username);
        when(mockCustomer.getEmail()).thenReturn(email);
        when(mockCustomer.getFamilyName()).thenReturn(familyName);
        when(mockCustomer.getGivenName()).thenReturn(givenName);
        when(mockCustomer.getRole()).thenReturn(role);
        when(mockCustomer.getCreateDate()).thenReturn(createDate);

        UserInfoDto result = CustomerTransform.transToUserInfor(mockCustomer, isCreator);

        assertNotNull(result);
        assertEquals(username, result.getUserName());
        assertEquals(email, result.getEmail());
        assertEquals(familyName, result.getFamilyName());
        assertEquals(role, result.getRole());
        assertEquals(createDate, result.getCreateDate());
        assertEquals(isCreator, result.isCreator());
    }
}