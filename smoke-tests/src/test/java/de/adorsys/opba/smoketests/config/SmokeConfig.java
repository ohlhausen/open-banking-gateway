package de.adorsys.opba.smoketests.config;

import de.adorsys.opba.db.repository.jpa.BankProfileJpaRepository;
import de.adorsys.opba.db.repository.jpa.ConsentRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class SmokeConfig {

    public static final LocalDate DATE_FROM = LocalDate.parse("2018-01-01");
    public static final LocalDate DATE_TO = LocalDate.parse("2020-09-30");
    public static final String BOTH_BOOKING = "BOTH";

    @Getter
    @Value("${test.smoke.opba.server-uri}")
    private String opbaServerUri;

    @Getter
    @Value("${test.smoke.aspsp-profile.server-uri}")
    private String aspspProfileServerUri;

    @MockBean
    // Stubbing out as they are not available, but currently breaking hierarchy has no sense as we can replace this with REST in future
    @SuppressWarnings("PMD.UnusedPrivateField") // Injecting into Spring context
    private BankProfileJpaRepository profiles;

    @MockBean // Stubbing out as they are not available, but currently breaking hierarchy has no sense as we can replace this with REST in future
    @SuppressWarnings("PMD.UnusedPrivateField") // Injecting into Spring context
    private ConsentRepository consents;

    @Bean
    SandboxConsentAuthApproachState authState() {
        return new SandboxConsentAuthApproachState(aspspProfileServerUri);
    }
}
