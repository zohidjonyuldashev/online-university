package uz.pdp.online_university;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uz.pdp.online_university.repository.AuditLogRepository;
import uz.pdp.online_university.repository.NotificationRepository;
import uz.pdp.online_university.repository.NotificationTemplateRepository;
import uz.pdp.online_university.repository.OtpVerificationRepository;
import uz.pdp.online_university.repository.PermissionRepository;
import uz.pdp.online_university.repository.PolicyVersionRepository;
import uz.pdp.online_university.repository.RoleRepository;
import uz.pdp.online_university.repository.UserRepository;
import uz.pdp.online_university.service.EmailService;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootTest
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@EnableAutoConfiguration(exclude = {
		DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class,
		JpaRepositoriesAutoConfiguration.class,
		MailSenderAutoConfiguration.class
})
class OnlineUniversityApplicationTests {

	@MockitoBean
	AuditLogRepository auditLogRepository;
	@MockitoBean
	UserRepository userRepository;
	@MockitoBean
	RoleRepository roleRepository;
	@MockitoBean
	PermissionRepository permissionRepository;
	@MockitoBean
	OtpVerificationRepository otpVerificationRepository;
	@MockitoBean
	PolicyVersionRepository policyVersionRepository;
	@MockitoBean
	NotificationRepository notificationRepository;
	@MockitoBean
	NotificationTemplateRepository notificationTemplateRepository;
	@MockitoBean
	JavaMailSender javaMailSender;
	@MockitoBean
	EmailService emailService;

	@Test
	void contextLoads() {
	}

}
