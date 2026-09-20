package org.smart.erp.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.smart.erp.eip.dto.SystemNotificationPublishDTO;
import org.smart.erp.eip.service.SystemNotificationService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies authorization at the Service proxy, independent of MVC controllers. */
class ServiceMethodSecurityProxyTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void clearSecurity() {
		SecurityContextHolder.clearContext();
		if (context != null) {
			context.close();
		}
	}

	@Test
	void authorizedServiceCallerCanPublish() {
		context = new AnnotationConfigApplicationContext(TestConfig.class);
		SecurityContextHolder.getContext().setAuthentication(
				new TestingAuthenticationToken("user", "n", "eip:notification:publish"));

		SystemNotificationService service = context.getBean(SystemNotificationService.class);
		assertThat(service.publish(new SystemNotificationPublishDTO())).isEqualTo("published");
	}

	@Test
	void unauthorizedServiceCallerIsRejectedBeforeImplementation() {
		context = new AnnotationConfigApplicationContext(TestConfig.class);
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "n"));

		SystemNotificationService service = context.getBean(SystemNotificationService.class);
		assertThatThrownBy(() -> service.publish(new SystemNotificationPublishDTO()))
				.isInstanceOf(AccessDeniedException.class);
		assertThat(context.getBean(AtomicInteger.class).get()).isZero();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableMethodSecurity
	static class TestConfig {
		@Bean
		AtomicInteger invocations() {
			return new AtomicInteger();
		}

		@Bean
		SystemNotificationService notificationService(AtomicInteger invocations) {
			return new TestNotificationService(invocations);
		}
	}

	static class TestNotificationService implements SystemNotificationService {
		private final AtomicInteger invocations;

		TestNotificationService(AtomicInteger invocations) {
			this.invocations = invocations;
		}

		@Override
		public String publish(SystemNotificationPublishDTO dto) {
			invocations.incrementAndGet();
			return "published";
		}
	}
}
