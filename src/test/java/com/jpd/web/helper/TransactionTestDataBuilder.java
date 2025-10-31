package com.jpd.web.helper;

import com.jpd.web.model.*;

import java.time.LocalDateTime;

public class TransactionTestDataBuilder {

    public static Customer mkCustomer(Long id, String given, String family, String email) {
        Customer c = new Customer();
        c.setCustomerId(id);
        c.setGivenName(given);
        c.setFamilyName(family);
        c.setEmail(email);
        return c;
    }

    public static Creator mkCreator(Long id, String fullName, boolean ban, Status status) {
        Creator cr = new Creator();
        cr.setCreatorId(id);
        cr.setFullName(fullName);
        cr.setBan(ban);
        cr.setStatus(status);
        return cr;
    }

    public static Course mkCourse(Long id, String name, double price, String url, Creator creator) {
        Course cs = new Course();
        cs.setCourseId(id);
        cs.setName(name);
        cs.setPrice(price);
        cs.setUrlImg(url);
        cs.setCreator(creator);
        return cs;
    }

    public static Enrollment mkEnrollment(Long id, Customer cust, Course course, boolean finish) {
        Enrollment e = new Enrollment();
        e.setEnrollId(id);
        e.setCustomer(cust);
        e.setCourse(course);
        e.setCreateDate(LocalDateTime.now().minusDays(5));
        e.setFinish(finish);
        return e;
    }

    public static CustomerTransaction mkTx(Long id, Enrollment enroll,
                                           double amount, double admin, double creator, String status) {
        CustomerTransaction tx = new CustomerTransaction();
        tx.setTransactionID(id);
        tx.setEnrollment(enroll);
        tx.setAmount(amount);
        tx.setAdminGet(admin);
        tx.setCreatorGet(creator);
        tx.setCurrency("VND");
        tx.setStatus(status);
        tx.setPaymentMethod(PaymentMethod.PAYPAL);
        tx.setPaymentId("TX-" + id);
        tx.setCreatedAt(LocalDateTime.now().minusDays(2));
        tx.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return tx;
    }
}
