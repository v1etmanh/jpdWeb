package com.jpd.web.service;


import com.jpd.web.config.VNPayConfig;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.EnrollmentExistException;
import com.jpd.web.model.Course;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.PaymentTracking;
import com.jpd.web.repository.*;
import com.paypal.orders.Order;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
@Slf4j
@Service
public class VNPayService {
    // ==================== CONSTANTS ====================
    private static final String DEFAULT_CURRENCY = "VND";
    private static final String RETURN_URL = "http://localhost:9090/api/vnpay/vnpay-payment";

    // ==================== DEPENDENCIES ====================
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
    @Autowired
    PayPalService payPalService;
    /**
     * Tạo PayPal order và lưu tracking vào database
     *
     * @param amount Số tiền thanh toán
     * @param courseId ID của khóa học
     * @param customerId ID của khách hàng
     * @return String URL thanh toán VNPay
     * @throws EnrollmentExistException nếu khách hàng đã đăng ký khóa học
     */
        public String createOrderWithTracking(long amount, long courseId, long customerId) throws Exception {
            payPalService.validateEnrollment(courseId, customerId);
            Optional<Course>c=  this.courseRepository.findById(courseId);
            if(c.isEmpty()||!c.get().isPublic()) {
                throw new CourseNotFoundException(courseId);
            }
            String description = payPalService.generateDescription(courseId, customerId);
            String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
            String vnpayUrl = createOrder(amount, description, vnp_TxnRef);
            payPalService.savePaymentTracking(vnp_TxnRef, amount, description, courseId, customerId);
            log.info("Payment Tracking Created Successfully");
            return vnpayUrl;
        }


    @Transactional
    public int captureOrderAndEnroll(String orderId,HttpServletRequest request) throws Exception {

        int status= orderReturn(request);

        if(status!=1){
            payPalService.cancelOrder(orderId);
        }
        PaymentTracking tracking = payPalService.updatePaymentTrackingStatus(orderId, "COMPLETED");
        log.info("Payment Tracking Updated Successfully");
        payPalService.createEnrollmentAndTransaction(tracking);
        log.info("Enrollment and transaction Created Successfully with {}",tracking);
        payPalService.updateCreatorBalance(tracking.getCourseId());
        log.info(" Updated balance creator Successfully");
        return status;
    }

    public String createOrder(long amount, String orderInfor, String vnp_TxnRef){

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_IpAddr = "127.0.0.1";
        String vnp_TmnCode = VNPayConfig.vnp_TmnCode;
        String orderType = "order-type";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount*100));
        vnp_Params.put("vnp_CurrCode", DEFAULT_CURRENCY);

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfor);
        vnp_Params.put("vnp_OrderType", orderType);

        String locate = "vn";
        vnp_Params.put("vnp_Locale", locate);

        vnp_Params.put("vnp_ReturnUrl", RETURN_URL);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List fieldNames = new ArrayList(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    //Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + queryUrl;

        return paymentUrl;
    }

    public int orderReturn(HttpServletRequest request){
        Map fields = new HashMap();
        for (Enumeration params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = null;
            String fieldValue = null;
            try {
                fieldName = URLEncoder.encode((String) params.nextElement(), StandardCharsets.US_ASCII.toString());
                fieldValue = URLEncoder.encode(request.getParameter(fieldName), StandardCharsets.US_ASCII.toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }
        String signValue = VNPayConfig.hashAllFields(fields);
        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(request.getParameter("vnp_TransactionStatus"))) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return -1;
        }


    }




}