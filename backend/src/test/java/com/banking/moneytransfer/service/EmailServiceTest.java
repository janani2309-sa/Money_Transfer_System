package com.banking.moneytransfer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    public void testSendOtpEmail_Success() {
        // Arrange
        String toEmail = "test@example.com";
        String otpCode = "123456";

        // Act
        emailService.sendOtpEmail(toEmail, otpCode);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertEquals("ApexPay <no-reply@apexpay.com>", sentMessage.getFrom());
        assertArrayEquals(new String[]{toEmail}, sentMessage.getTo());
        assertEquals("ApexPay - Verification Code", sentMessage.getSubject());
        
        String text = sentMessage.getText();
        assertNotNull(text);
        assertTrue(text.contains(otpCode));
        assertTrue(text.contains("Dear Customer"));
        assertTrue(text.contains("ApexPay Security Team"));
    }

    @Test
    public void testSendOtpEmail_Failure() {
        // Arrange
        String toEmail = "fail@example.com";
        String otpCode = "654321";
        doThrow(new RuntimeException("Mail server down")).when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        // The exception should be caught internally by the service, so no exception should be thrown to the caller.
        assertDoesNotThrow(() -> emailService.sendOtpEmail(toEmail, otpCode));

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
