package com.jpd.web.service.utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.NoticeForm;
import com.jpd.web.exception.EmailSendingFailedException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SendNoticeService {
  @Autowired
  private JavaMailSender mailSender;

  public void sendNotice(NoticeForm notice, String recipientEmail) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(recipientEmail);
      message.setSubject("Chúc mừng - Bạn đã trở thành Creator");
      message.setText(buildEmailContent(notice));
      
      mailSender.send(message);
      notice.setEmailSent(true);
      
      log.info("Email sent successfully to: {}", recipientEmail);
    } catch (Exception e) {
      log.error("Failed to send email to {}: {}", recipientEmail, e.getMessage(), e);
      notice.setEmailSent(false);
      throw new EmailSendingFailedException(recipientEmail, e);
    }
  }

  private String buildEmailContent(NoticeForm notice) {
    return "Xin chúc mừng!\n\n" +
      "Tài khoản của bạn đã được đăng ký thành công để trở thành một creator.\n\n" +
      "Thông tin:\n" +
      "- Thời gian: " + notice.getCreatedAt() + "\n" +
      "- Chi tiết: " + notice.getMessage() + "\n\n" +
      "Chúng tôi mong bạn cống hiến cho cộng đồng học tập tích cực.\n\n" +
      "Trân trọng,\nĐội ngũ hỗ trợ";
  }
}