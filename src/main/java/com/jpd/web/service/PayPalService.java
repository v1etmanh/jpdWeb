package com.jpd.web.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.EnrollmentExistException;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.CustomerTransaction;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.PaymentTracking;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.CustomerTransactionRepository;
import com.jpd.web.repository.EnrollmentRepository;
import com.jpd.web.repository.PaymentTrackingRepository;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.AmountWithBreakdown;
import com.paypal.orders.ApplicationContext;
import com.paypal.orders.Order;
import com.paypal.orders.OrderRequest;
import com.paypal.orders.OrdersCaptureRequest;
import com.paypal.orders.OrdersCreateRequest;
import com.paypal.orders.OrdersGetRequest;
import com.paypal.orders.PurchaseUnitRequest;

@Service
public class PayPalService {

    // ==================== CONSTANTS ====================
    private static final String DEFAULT_CURRENCY = "USD";
    private static final String RETURN_URL = "http://localhost:9090/api/paypal/success";
    private static final String CANCEL_URL = "http://localhost:9090/api/paypal/cancel";
    private static final double CREATOR_COMMISSION_RATE = 0.8;
    private static final double ADMIN_COMMISSION_RATE = 0.2;

    // ==================== DEPENDENCIES ====================
    @Autowired
    private PayPalHttpClient payPalHttpClient;

    @Autowired
    private PaymentTrackingRepository paymentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CreatorRepository creatorRepository;

    @Autowired
    private CustomerTransactionRepository customerTransactionRepository;

    // ==================== PUBLIC API METHODS ====================

    /**
     * Tạo PayPal order và lưu tracking vào database
     *
     * @param amount Số tiền thanh toán
     * @param courseId ID của khóa học
     * @param customerId ID của khách hàng
     * @return Order PayPal đã tạo
     * @throws EnrollmentExistException nếu khách hàng đã đăng ký khóa học
     */
    public Order createOrderWithTracking(double amount, long courseId, long customerId) throws Exception {
        validateEnrollment(courseId, customerId);
        Optional<Course>c=  this.courseRepository.findById(courseId);
        if(c.isEmpty()||!c.get().isPublic()) {
            throw new CourseNotFoundException(courseId);
        }
        String description = generateDescription(courseId, customerId);
        Order order = createPayPalOrder(amount, DEFAULT_CURRENCY, description);

        savePaymentTracking(order.id(), amount, description, courseId, customerId);

        return order;
    }

    /**
     * Capture order PayPal và tạo enrollment cho khách hàng
     *
     * @param orderId ID của order PayPal
     * @return Order đã được capture
     */
    @Transactional
    public Order captureOrderAndEnroll(String orderId) throws Exception {
        Order order = capturePayPalOrder(orderId);

        PaymentTracking tracking = updatePaymentTrackingStatus(orderId, "COMPLETED");

        createEnrollmentAndTransaction(tracking);
        updateCreatorBalance(tracking.getCourseId());

        return order;
    }

    /**
     * Hủy order và cập nhật trạng thái
     *
     * @param orderId ID của order cần hủy
     */
    public void cancelOrder(String orderId) {
        updatePaymentTrackingStatus(orderId, "CANCELLED");
    }

    /**
     * Lấy thông tin order từ PayPal
     *
     * @param orderId ID của order
     * @return Order từ PayPal
     */
    public Order getOrder(String orderId) throws Exception {
        OrdersGetRequest request = new OrdersGetRequest(orderId);
        HttpResponse<Order> response = payPalHttpClient.execute(request);
        return response.result();
    }

    // ==================== PAYPAL INTEGRATION METHODS ====================

    /**
     * Tạo order trên PayPal
     */
    private Order createPayPalOrder(double total, String currency, String description) throws Exception {
        OrderRequest orderRequest = buildOrderRequest(total, currency, description);

        OrdersCreateRequest request = new OrdersCreateRequest();
        request.requestBody(orderRequest);

        HttpResponse<Order> response = payPalHttpClient.execute(request);
        return response.result();
    }

    /**
     * Capture payment từ PayPal
     */
    private Order capturePayPalOrder(String orderId) throws Exception {
        OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
        HttpResponse<Order> response = payPalHttpClient.execute(request);
        return response.result();
    }

    /**
     * Build PayPal OrderRequest
     */
    private OrderRequest buildOrderRequest(double total, String currency, String description) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        ApplicationContext applicationContext = new ApplicationContext()
                .returnUrl(RETURN_URL)
                .cancelUrl(CANCEL_URL);
        orderRequest.applicationContext(applicationContext);

        PurchaseUnitRequest purchaseUnitRequest = new PurchaseUnitRequest()
                .description(description)
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode(currency)
                        .value(String.format("%.2f", total)));

        List<PurchaseUnitRequest> purchaseUnitRequests = new ArrayList<>();
        purchaseUnitRequests.add(purchaseUnitRequest);
        orderRequest.purchaseUnits(purchaseUnitRequests);

        return orderRequest;
    }

    // ==================== VALIDATION METHODS ====================

    /**
     * Kiểm tra xem khách hàng đã đăng ký khóa học chưa
     */
    public void validateEnrollment(long courseId, long customerId) throws EnrollmentExistException {
        Optional<Enrollment> existingEnrollment = enrollmentRepository
                .findByCourse_CourseIdAndCustomer_CustomerId(courseId, customerId);

        if (existingEnrollment.isPresent()) {
            throw new EnrollmentExistException("You have already enrolled in this course");
        }
    }

    // ==================== PAYMENT TRACKING METHODS ====================

    /**
     * Lưu thông tin payment tracking
     */
    public void savePaymentTracking(String paymentId, double amount, String description,
                                    long courseId, long customerId) {
        PaymentTracking tracking = PaymentTracking.builder()
                .paymentId(paymentId)
                .status("CREATED")
                .amount(amount)
                .currency(DEFAULT_CURRENCY)
                .description(description)
                .createdAt(LocalDateTime.now())
                .courseId(courseId)
                .customerId(customerId)
                .build();

        paymentRepository.save(tracking);
    }

    /**
     * Cập nhật trạng thái payment tracking
     */
    public PaymentTracking updatePaymentTrackingStatus(String orderId, String status) {
        PaymentTracking tracking = paymentRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Payment tracking not found: " + orderId));

        tracking.setStatus(status);
        tracking.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(tracking);
    }

    // ==================== ENROLLMENT & TRANSACTION METHODS ====================

    /**
     * Tạo enrollment và transaction cho khách hàng
     */
    public void createEnrollmentAndTransaction(PaymentTracking tracking) {
        Customer customer = customerRepository.findById(tracking.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found: " + tracking.getCustomerId()));

        Course course = courseRepository.findById(tracking.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found: " + tracking.getCourseId()));

        Enrollment enrollment = Enrollment.builder()
                .course(course)
                .customer(customer)
                .build();

        enrollmentRepository.save(enrollment);

        createCustomerTransaction(tracking, enrollment);
    }

    /**
     * Tạo customer transaction
     */
    private void createCustomerTransaction(PaymentTracking tracking, Enrollment enrollment) {
        double amount = tracking.getAmount();

        CustomerTransaction transaction = CustomerTransaction.builder()
                .amount(amount)
                .content(tracking.getDescription())
                .creatorGet(amount * CREATOR_COMMISSION_RATE)
                .adminGet(amount * ADMIN_COMMISSION_RATE)
                .currency(DEFAULT_CURRENCY)
                .enrollment(enrollment)
                .status("SUCCESS")
                .build();

        customerTransactionRepository.save(transaction);
    }

    /**
     * Cập nhật balance của creator (hiện tại chưa thực sự cộng tiền)
     */
    public void updateCreatorBalance(long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        Creator creator = course.getCreator();
        // TODO: Cập nhật logic cộng tiền vào balance
        double currentBalance = creator.getBalance()+course.getPrice();
        creator.setBalance(currentBalance);

        creatorRepository.save(creator);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Tạo mô tả cho payment
     */
    public String generateDescription(long courseId, long customerId) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("Payment-%s-Course%d-Customer%d", timestamp, courseId, customerId);
    }
}