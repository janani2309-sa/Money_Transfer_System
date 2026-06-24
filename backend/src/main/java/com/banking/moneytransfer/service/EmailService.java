package com.banking.moneytransfer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("ApexPay <no-reply@apexpay.com>");
            message.setTo(toEmail);
            message.setSubject("ApexPay - Verification Code");
            message.setText("Dear Customer,\n\n"
                    + "Thank you for choosing ApexPay.\n\n"
                    + "Your 6-digit verification code is: " + otpCode + "\n\n"
                    + "This code is valid for 5 minutes. Please do not share this code with anyone.\n\n"
                    + "Best regards,\n"
                    + "ApexPay Security Team");

            mailSender.send(message);
            System.out.println("[EMAIL SERVICE] Verification email successfully sent to: " + toEmail);
        } catch (Exception ex) {
            System.err.println("========================================================================");
            System.err.println("[EMAIL SERVICE WARNING] SMTP email delivery failed to: " + toEmail);
            System.err.println("Error details: " + ex.getMessage());
            System.err.println("FALLBACK LOG: Verification Code is: " + otpCode);
            System.err.println("========================================================================");
        }
    }
}
