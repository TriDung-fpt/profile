package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fptu.evstation.rental.evrentalsystem.service.AuthService;
import com.fptu.evstation.rental.evrentalsystem.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;
import vn.payos.service.blocking.webhooks.WebhooksService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentWebhookController.class)
class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PayOS payOS;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private AuthService authService; // Mock the missing dependency

    @Mock
    private WebhooksService webhooksService;

    @BeforeEach
    void setUp() {
        when(payOS.webhooks()).thenReturn(webhooksService);
    }

    @Test
    void testHandleWebhook_EmptyBody_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .content(""))
                .andExpect(status().isOk())
                .andExpect(content().string("ignored-empty-body"));

        verify(paymentService, never()).autoConfirmDeposit(anyLong());
        verify(paymentService, never()).autoConfirmRentalDeposit(anyLong());
    }

    @Test
    void testHandleWebhook_NullBody_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/payments/webhook"))
                .andExpect(status().isOk())
                .andExpect(content().string("ignored-empty-body"));
    }

    @Test
    void testHandleWebhook_VerificationFails_SignatureException() throws Exception {
        when(webhooksService.verify(anyString())).thenThrow(new RuntimeException("Invalid signature"));

        mockMvc.perform(post("/api/payments/webhook")
                        .content("{\"key\":\"value\"}")) // Nội dung bất kỳ
                .andExpect(status().isOk())
                .andExpect(content().string("ignored-verification-failed"));
    }

    @Test
    void testHandleWebhook_VerificationReturnsNull() throws Exception {
        when(webhooksService.verify(anyString())).thenReturn(null);

        mockMvc.perform(post("/api/payments/webhook")
                        .content("{\"key\":\"value\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("ignored-verification-failed"));
    }

    @Test
    void testHandleWebhook_TestPayload_OrderCodeZero() throws Exception {
        WebhookData testData = new WebhookData();
        testData.setOrderCode(0L);
        when(webhooksService.verify(anyString())).thenReturn(testData);

        mockMvc.perform(post("/api/payments/webhook")
                        .content("{\"orderCode\":0}"))
                .andExpect(status().isOk())
                .andExpect(content().string("ignored-test-payload"));
    }

    @Test
    void testHandleWebhook_FailedPayment_CodeNot00() throws Exception {
        WebhookData failedData = new WebhookData();
        failedData.setOrderCode(12345678L);
        failedData.setCode("99"); // Mã lỗi
        failedData.setDesc("Payment failed");
        when(webhooksService.verify(anyString())).thenReturn(failedData);

        mockMvc.perform(post("/api/payments/webhook")
                        .content("{\"orderCode\":12345678, \"code\":\"99\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("ignored-non-success-code"));
    }

    @Test
    void testHandleWebhook_Success_Type1_Deposit500k() throws Exception {
        WebhookData successData = new WebhookData();
        successData.setCode("00");
        successData.setOrderCode(1991234L);
        when(webhooksService.verify(anyString())).thenReturn(successData);

        doNothing().when(paymentService).autoConfirmDeposit(99L);

        mockMvc.perform(post("/api/payments/webhook")
                        .content("{\"orderCode\":1991234, \"code\":\"00\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("processed-deposit-booking-99"));

        verify(paymentService, times(1)).autoConfirmDeposit(99L);
        verify(paymentService, never()).autoConfirmRentalDeposit(anyLong());
    }

    @Test
    void testHandleWebhook_Success_Type2_Deposit2Percent() throws Exception {
        WebhookData successData = new WebhookData();
        successData.setCode("00");
        successData.setOrderCode(21005678L);
        when(webhooksService.verify(anyString())).thenReturn(successData);

        doNothing().when(paymentService).autoConfirmRentalDeposit(100L);

        mockMvc.perform(post("/api/payments/webhook")
                        .content("{\"orderCode\":21005678, \"code\":\"00\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("processed-rental-booking-100"));

        verify(paymentService, times(1)).autoConfirmRentalDeposit(100L);
        verify(paymentService, never()).autoConfirmDeposit(anyLong());
    }

    @Test
    void testHandleWebhook_Success_UnknownType() throws Exception {
        WebhookData successData = new WebhookData();
        successData.setCode("00");
        successData.setOrderCode(91005678L); // Type 9 (Unknown)
        when(webhooksService.verify(anyString())).thenReturn(successData);

        mockMvc.perform(post("/api/payments/webhook")
                        .content("{\"orderCode\":91005678, \"code\":\"00\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("ignored-unknown-type"));

        verify(paymentService, never()).autoConfirmDeposit(anyLong());
        verify(paymentService, never()).autoConfirmRentalDeposit(anyLong());
    }

    @Test
    void testHandleWebhook_Fail_OrderCodeTooShort() throws Exception {
        WebhookData successData = new WebhookData();
        successData.setCode("00");
        successData.setOrderCode(12345L); // Quá ngắn (length 5 < 6)
        when(webhooksService.verify(anyString())).thenReturn(successData);

        mockMvc.perform(post("/api/payments/webhook")
                        .content("{\"orderCode\":12345, \"code\":\"00\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("ignored-short-ordercode"));
    }

    @Test
    void testHandleWebhook_Fail_ServiceException() throws Exception {
        WebhookData successData = new WebhookData();
        successData.setCode("00");
        successData.setOrderCode(1991234L); // Type 1, Booking 99
        when(webhooksService.verify(anyString())).thenReturn(successData);

        doThrow(new RuntimeException("DB error")).when(paymentService).autoConfirmDeposit(99L);

        mockMvc.perform(post("/api/payments/webhook")
                        .content("{\"orderCode\":1991234, \"code\":\"00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("processing-error: DB error"));
    }

    @Test
    void testHandleWebhook_Fail_ParsingOrderCodeException() throws Exception {
        when(webhooksService.verify(anyString())).thenThrow(new NumberFormatException("Lỗi parse"));

        mockMvc.perform(post("/api/payments/webhook")
                        .content("{\"orderCode\":\"1A991234\", \"code\":\"00\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("ignored-verification-failed"));
    }
}