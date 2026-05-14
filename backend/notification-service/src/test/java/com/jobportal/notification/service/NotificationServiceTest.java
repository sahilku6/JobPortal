package com.jobportal.notification.service;

import com.jobportal.notification.dto.NotificationRequest;
import com.jobportal.notification.model.Notification;
import com.jobportal.notification.repository.NotificationRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private NotificationRepository notificationRepository;

    @InjectMocks private NotificationService notificationService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(notificationService, "frontendBaseUrl", "http://localhost:3000");
    }

    @Test
    void sendNotification_marksAsSentOnSuccess() throws Exception {
        NotificationRequest request = NotificationRequest.builder()
                .to("user@test.com").subject("Hi").body("Body").type("JOB_APPLIED").build();

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.sendNotification(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(captor.capture());
        Notification finalSave = captor.getAllValues().get(2);
        assertThat(finalSave.getStatus()).isEqualTo(Notification.NotificationStatus.SENT);
        assertThat(finalSave.getSentAt()).isNotNull();
    }

    @Test
    void sendNotification_marksFailedWhenEmailThrows() throws Exception {
        NotificationRequest request = NotificationRequest.builder()
                .to("user@test.com").subject("Hi").body("Body").type("JOB_APPLIED").build();

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.sendNotification(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        Notification finalSave = captor.getAllValues().get(1);
        assertThat(finalSave.getStatus()).isEqualTo(Notification.NotificationStatus.FAILED);
        assertThat(finalSave.getErrorMessage()).contains("smtp down");
    }

    @Test
    void buildHtml_usesBlockLayoutAndCenteredButton() throws Exception {
        NotificationRequest request = NotificationRequest.builder()
                .to("user@test.com")
                .subject("Update on your application")
                .body("Please review the latest details.")
                .type("STATUS_CHANGED")
                .jobTitle("Human Resources Manager")
                .companyName("Rohit Pvt LTD.")
                .status("UNDER_REVIEW")
                .applicationId(4L)
                .ctaLabel("View Status")
                .ctaUrl("https://careerbridge.local/applications")
                .build();

        Method buildHtml = NotificationService.class.getDeclaredMethod("buildHtml", NotificationRequest.class);
        buildHtml.setAccessible(true);

        String html = (String) buildHtml.invoke(notificationService, request);

        assertThat(html).contains(".detailRow{display:block");
        assertThat(html).contains(".detailValue{display:block");
        assertThat(html).contains(".snapshotGrid{display:block");
        assertThat(html).contains("display:block;max-width:240px;margin:0 auto;background:");
        assertThat(html).contains("color:#ffffff !important");
        assertThat(html).contains("text-align:center");
    }
}
