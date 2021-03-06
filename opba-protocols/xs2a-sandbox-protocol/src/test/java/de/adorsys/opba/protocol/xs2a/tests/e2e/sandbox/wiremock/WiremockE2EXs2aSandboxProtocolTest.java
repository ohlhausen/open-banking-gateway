package de.adorsys.opba.protocol.xs2a.tests.e2e.sandbox.wiremock;

import com.tngtech.jgiven.integration.spring.junit5.SpringScenarioTest;
import de.adorsys.opba.db.domain.Approach;
import de.adorsys.opba.protocol.xs2a.config.protocol.ProtocolConfiguration;
import de.adorsys.opba.protocol.xs2a.tests.TestProfiles;
import de.adorsys.opba.protocol.xs2a.tests.e2e.JGivenConfig;
import de.adorsys.opba.protocol.xs2a.tests.e2e.sandbox.wiremock.config.Xs2aSandboxProtocolApplication;
import de.adorsys.opba.protocol.xs2a.tests.e2e.stages.AccountInformationResult;
import de.adorsys.opba.protocol.xs2a.tests.e2e.wiremock.mocks.MockServers;
import de.adorsys.opba.protocol.xs2a.tests.e2e.wiremock.mocks.WiremockAccountInformationRequest;
import de.adorsys.opba.protocol.xs2a.tests.e2e.wiremock.mocks.WiremockConst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Happy-path test that uses wiremock-stubbed request-responses to drive banking-protocol of Dynamic-Sandbox.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@SpringBootTest(classes = {Xs2aSandboxProtocolApplication.class, JGivenConfig.class}, webEnvironment = RANDOM_PORT)
@ActiveProfiles(profiles = {TestProfiles.ONE_TIME_POSTGRES_RAMFS, TestProfiles.MOCKED_SANDBOX})
class WiremockE2EXs2aSandboxProtocolTest extends SpringScenarioTest<MockServers, WiremockAccountInformationRequest<? extends WiremockAccountInformationRequest<?>>, AccountInformationResult> {

    @LocalServerPort
    private int port;

    @Autowired
    private ProtocolConfiguration configuration;

    // See https://github.com/spring-projects/spring-boot/issues/14879 for the 'why setting port'
    @BeforeEach
    void setBaseUrl() {
        ProtocolConfiguration.Redirect.Consent consent = configuration.getRedirect().getConsentAccounts();
        consent.setOk(consent.getOk().replaceAll("localhost:\\d+", "localhost:" + port));
        consent.setNok(consent.getNok().replaceAll("localhost:\\d+", "localhost:" + port));
    }

    @ParameterizedTest
    @EnumSource(Approach.class)
    void testAccountAndTransactionsListWithConsentForAllServicesUsingRedirect(Approach approach) {
        given()
                .redirect_mock_of_sandbox_for_anton_brueckner_transactions_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(approach)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_transactions_for_anton_brueckner(WiremockConst.ANTON_BRUECKNER_RESOURCE_ID)
                .and()
                .user_anton_brueckner_provided_initial_parameters_to_list_transactions_with_all_accounts_psd2_consent()
                .and()
                .user_anton_brueckner_sees_that_he_needs_to_be_redirected_to_aspsp_and_redirects_to_aspsp()
                .and()
                .open_banking_redirect_from_aspsp_ok_webhook_called();
        then()
                .open_banking_has_consent_for_anton_brueckner_transaction_list()
                .open_banking_can_read_anton_brueckner_account_data_using_consent_bound_to_service_session()
                .open_banking_can_read_anton_brueckner_transactions_data_using_consent_bound_to_service_session(
                        WiremockConst.ANTON_BRUECKNER_RESOURCE_ID, WiremockConst.DATE_FROM, WiremockConst.DATE_TO, WiremockConst.BOTH_BOOKING
                );
    }

    @ParameterizedTest
    @EnumSource(Approach.class)
    void testAccountAndTransactionsListWithConsentForAllServicesUsingEmbedded(Approach approach) {
        given()
                .embedded_mock_of_sandbox_for_max_musterman_transactions_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(approach)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_transactions_for_max_musterman()
                .and()
                .user_max_musterman_provided_initial_parameters_to_list_transactions_with_all_accounts_psd2_consent()
                .and()
                .user_max_musterman_provided_password_to_embedded_authorization()
                .and()
                .user_max_musterman_selected_sca_challenge_type_email1_to_embedded_authorization()
                .and()
                .user_max_musterman_provided_sca_challenge_result_to_embedded_authorization_and_sees_redirect_to_fintech_ok();
        then()
                .open_banking_has_consent_for_max_musterman_transaction_list()
                .open_banking_can_read_max_musterman_account_data_using_consent_bound_to_service_session()
                .open_banking_can_read_max_musterman_transactions_data_using_consent_bound_to_service_session(
                        WiremockConst.MAX_MUSTERMAN_RESOURCE_ID, WiremockConst.DATE_FROM, WiremockConst.DATE_TO, WiremockConst.BOTH_BOOKING
                );
    }
}
