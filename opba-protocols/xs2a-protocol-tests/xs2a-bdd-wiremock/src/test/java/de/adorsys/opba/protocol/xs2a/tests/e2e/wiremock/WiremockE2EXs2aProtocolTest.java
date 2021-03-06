package de.adorsys.opba.protocol.xs2a.tests.e2e.wiremock;

import com.tngtech.jgiven.integration.spring.junit5.SpringScenarioTest;
import de.adorsys.opba.db.domain.Approach;
import de.adorsys.opba.protocol.xs2a.config.protocol.ProtocolConfiguration;
import de.adorsys.opba.protocol.xs2a.tests.e2e.JGivenConfig;
import de.adorsys.opba.protocol.xs2a.tests.e2e.stages.AccountInformationResult;
import de.adorsys.opba.protocol.xs2a.tests.e2e.wiremock.mocks.MockServers;
import de.adorsys.opba.protocol.xs2a.tests.e2e.wiremock.mocks.WiremockAccountInformationRequest;
import de.adorsys.opba.protocol.xs2a.tests.e2e.wiremock.mocks.WiremockConst;
import de.adorsys.opba.protocol.xs2a.tests.e2e.wiremock.mocks.Xs2aProtocolApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static de.adorsys.opba.protocol.xs2a.tests.TestProfiles.MOCKED_SANDBOX;
import static de.adorsys.opba.protocol.xs2a.tests.TestProfiles.ONE_TIME_POSTGRES_RAMFS;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Happy-path test that uses wiremock-stubbed request-responses to drive banking-protocol.
 */
/*
As we redefine list accounts for adorsys-sandbox bank to sandbox customary one
(and it doesn't make sense to import sandbox module here as it is XS2A test) moving it back to plain xs2a bean:
 */
@Sql(statements = "UPDATE opb_bank_protocol SET protocol_bean_name = 'xs2aListTransactions' WHERE protocol_bean_name = 'xs2aSandboxListTransactions'")

@Transactional(propagation = Propagation.NOT_SUPPORTED)
@SpringBootTest(classes = {Xs2aProtocolApplication.class, JGivenConfig.class}, webEnvironment = RANDOM_PORT)
@ActiveProfiles(profiles = {ONE_TIME_POSTGRES_RAMFS, MOCKED_SANDBOX})
class WiremockE2EXs2aProtocolTest extends SpringScenarioTest<MockServers, WiremockAccountInformationRequest<? extends WiremockAccountInformationRequest<?>>, AccountInformationResult> {

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
    void testAccountsListWithConsentUsingRedirect(Approach approach) {
        given()
                .redirect_mock_of_sandbox_for_anton_brueckner_accounts_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(approach)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_accounts_for_anton_brueckner()
                .and()
                .user_anton_brueckner_provided_initial_parameters_to_list_accounts_with_all_accounts_consent()
                .and()
                .user_anton_brueckner_sees_that_he_needs_to_be_redirected_to_aspsp_and_redirects_to_aspsp()
                .and()
                .open_banking_redirect_from_aspsp_ok_webhook_called();
        then()
                .open_banking_has_consent_for_anton_brueckner_account_list()
                .open_banking_can_read_anton_brueckner_account_data_using_consent_bound_to_service_session();
    }

    @ParameterizedTest
    @EnumSource(Approach.class)
    void testTransactionsListWithConsentUsingRedirect(Approach approach) {
        given()
                .redirect_mock_of_sandbox_for_anton_brueckner_transactions_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(approach)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_transactions_for_anton_brueckner(WiremockConst.ANTON_BRUECKNER_RESOURCE_ID)
                .and()
                .user_anton_brueckner_provided_initial_parameters_to_list_transactions_with_single_account_consent()
                .and()
                .user_anton_brueckner_sees_that_he_needs_to_be_redirected_to_aspsp_and_redirects_to_aspsp()
                .and()
                .open_banking_redirect_from_aspsp_ok_webhook_called();
        then()
                .open_banking_has_consent_for_anton_brueckner_transaction_list()
                .open_banking_can_read_anton_brueckner_transactions_data_using_consent_bound_to_service_session(
                    WiremockConst.ANTON_BRUECKNER_RESOURCE_ID, WiremockConst.DATE_FROM, WiremockConst.DATE_TO, WiremockConst.BOTH_BOOKING
                );
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
    void testAccountsListWithConsentUsingEmbedded(Approach approach) {
        given()
                .embedded_mock_of_sandbox_for_max_musterman_accounts_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(approach)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_accounts_for_max_musterman()
                .and()
                .user_max_musterman_provided_initial_parameters_to_list_accounts_all_accounts_consent()
                .and()
                .user_max_musterman_provided_password_to_embedded_authorization()
                .and()
                .user_max_musterman_selected_sca_challenge_type_email2_to_embedded_authorization()
                .and()
                .user_max_musterman_provided_sca_challenge_result_to_embedded_authorization_and_sees_redirect_to_fintech_ok();
        then()
                .open_banking_has_consent_for_max_musterman_account_list()
                .open_banking_can_read_max_musterman_account_data_using_consent_bound_to_service_session();
    }

    @ParameterizedTest
    @EnumSource(Approach.class)
    void testTransactionsListWithConsentUsingEmbedded(Approach approach) {
        given()
                .embedded_mock_of_sandbox_for_max_musterman_transactions_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(approach)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_transactions_for_max_musterman()
                .and()
                .user_max_musterman_provided_initial_parameters_to_list_transactions_with_single_account_consent()
                .and()
                .user_max_musterman_provided_password_to_embedded_authorization()
                .and()
                .user_max_musterman_selected_sca_challenge_type_email1_to_embedded_authorization()
                .and()
                .user_max_musterman_provided_sca_challenge_result_to_embedded_authorization_and_sees_redirect_to_fintech_ok();
        then()
                .open_banking_has_consent_for_max_musterman_transaction_list()
                .open_banking_can_read_max_musterman_transactions_data_using_consent_bound_to_service_session(
                        WiremockConst.MAX_MUSTERMAN_RESOURCE_ID, WiremockConst.DATE_FROM, WiremockConst.DATE_TO, WiremockConst.BOTH_BOOKING
                );
    }

    @ParameterizedTest
    @EnumSource(Approach.class)
    void testAccountsListWithOnceWrongPasswordThenOkWithConsentUsingEmbedded(Approach approach) {
        given()
                .embedded_mock_of_sandbox_for_max_musterman_accounts_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(approach)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_accounts_for_max_musterman()
                .and()
                .user_max_musterman_provided_initial_parameters_to_list_accounts_all_accounts_consent()
                .and()
                .user_max_musterman_provided_wrong_password_to_embedded_authorization_and_stays_on_password_page()
                .and()
                .user_max_musterman_provided_correct_password_after_wrong_to_embedded_authorization()
                .and()
                .user_max_musterman_selected_sca_challenge_type_email2_to_embedded_authorization()
                .and()
                .user_max_musterman_provided_sca_challenge_result_to_embedded_authorization_and_sees_redirect_to_fintech_ok();
        then()
                .open_banking_has_consent_for_max_musterman_account_list()
                .open_banking_can_read_max_musterman_account_data_using_consent_bound_to_service_session();
    }

    @ParameterizedTest
    @EnumSource(Approach.class)
    void testTransactionsListWithOnceWrongPasswordThenOkWithConsentUsingEmbedded(Approach approach) {
        given()
                .embedded_mock_of_sandbox_for_max_musterman_transactions_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(approach)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_transactions_for_max_musterman()
                .and()
                .user_max_musterman_provided_initial_parameters_to_list_transactions_with_single_account_consent()
                .and()
                .user_max_musterman_provided_wrong_password_to_embedded_authorization_and_stays_on_password_page()
                .and()
                .user_max_musterman_provided_correct_password_after_wrong_to_embedded_authorization()
                .and()
                .user_max_musterman_selected_sca_challenge_type_email1_to_embedded_authorization()
                .and()
                .user_max_musterman_provided_sca_challenge_result_to_embedded_authorization_and_sees_redirect_to_fintech_ok();
        then()
                .open_banking_has_consent_for_max_musterman_transaction_list()
                .open_banking_can_read_max_musterman_transactions_data_using_consent_bound_to_service_session(
                        WiremockConst.MAX_MUSTERMAN_RESOURCE_ID, WiremockConst.DATE_FROM, WiremockConst.DATE_TO, WiremockConst.BOTH_BOOKING
                );
    }

    @ParameterizedTest
    @EnumSource(Approach.class)
    void testTransactionsListWithOnceWrongIbanThenOkWithConsentUsingEmbedded(Approach approach) {
        given()
                .embedded_mock_of_sandbox_for_max_musterman_transactions_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(approach)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_transactions_for_max_musterman()
                .and()
                .user_max_musterman_provided_initial_parameters_with_wrong_iban_to_list_transactions_with_single_account_consent()
                .and()
                .user_max_musterman_provided_initial_parameters_with_correct_iban_to_list_transactions_with_single_account_consent()
                .and()
                .user_max_musterman_provided_password_to_embedded_authorization()
                .and()
                .user_max_musterman_selected_sca_challenge_type_email1_to_embedded_authorization()
                .and()
                .user_max_musterman_provided_sca_challenge_result_to_embedded_authorization_and_sees_redirect_to_fintech_ok();
        then()
                .open_banking_has_consent_for_max_musterman_transaction_list()
                .open_banking_can_read_max_musterman_transactions_data_using_consent_bound_to_service_session(
                        WiremockConst.MAX_MUSTERMAN_RESOURCE_ID, WiremockConst.DATE_FROM, WiremockConst.DATE_TO, WiremockConst.BOTH_BOOKING
                );
    }

    @ParameterizedTest
    @EnumSource(Approach.class)
    void testAccountsListWithOnceWrongScaChallengeThenOkWithConsentUsingEmbedded(Approach approach) {
        given()
                .embedded_mock_of_sandbox_for_max_musterman_accounts_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(approach)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_accounts_for_max_musterman()
                .and()
                .user_max_musterman_provided_initial_parameters_to_list_accounts_all_accounts_consent()
                .and()
                .user_max_musterman_provided_password_to_embedded_authorization()
                .and()
                .user_max_musterman_selected_sca_challenge_type_email2_to_embedded_authorization()
                .and()
                .user_max_musterman_provided_wrong_sca_challenge_result_to_embedded_authorization_and_stays_on_sca_page()
                .and()
                .user_max_musterman_provided_correct_sca_challenge_result_after_wrong_to_embedded_authorization_and_sees_redirect_to_fintech_ok();
        then()
                .open_banking_has_consent_for_max_musterman_account_list()
                .open_banking_can_read_max_musterman_account_data_using_consent_bound_to_service_session();
    }

    @ParameterizedTest
    @EnumSource(Approach.class)
    void testTransactionsListWithOnceWrongScaChallengeThenOkWithConsentUsingEmbedded(Approach approach) {
        given()
                .embedded_mock_of_sandbox_for_max_musterman_transactions_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(approach)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_transactions_for_max_musterman()
                .and()
                .user_max_musterman_provided_initial_parameters_to_list_transactions_with_single_account_consent()
                .and()
                .user_max_musterman_provided_password_to_embedded_authorization()
                .and()
                .user_max_musterman_selected_sca_challenge_type_email1_to_embedded_authorization()
                .and()
                .user_max_musterman_provided_wrong_sca_challenge_result_to_embedded_authorization_and_stays_on_sca_page()
                .and()
                .user_max_musterman_provided_correct_sca_challenge_result_after_wrong_to_embedded_authorization_and_sees_redirect_to_fintech_ok();
        then()
                .open_banking_has_consent_for_max_musterman_transaction_list()
                .open_banking_can_read_max_musterman_transactions_data_using_consent_bound_to_service_session(
                        WiremockConst.MAX_MUSTERMAN_RESOURCE_ID, WiremockConst.DATE_FROM, WiremockConst.DATE_TO, WiremockConst.BOTH_BOOKING
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

    @Test
    void testAccountsListWithConsentUsingRedirectWithIpAddress() {
        given()
                .redirect_mock_of_sandbox_for_anton_brueckner_accounts_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(Approach.REDIRECT)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_accounts_for_anton_brueckner()
                .and()
                .user_anton_brueckner_provided_initial_parameters_to_list_accounts_with_all_accounts_consent_with_ip_address_check()
                .and()
                .open_banking_redirect_from_aspsp_ok_webhook_called();
        then()
                .open_banking_has_consent_for_anton_brueckner_account_list()
                .open_banking_can_read_anton_brueckner_account_data_using_consent_bound_to_service_session();
    }

    @Test
    void testAccountsListWithConsentUsingRedirectWithComputedIpAddress() {
        given()
                .redirect_mock_of_sandbox_for_anton_brueckner_accounts_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(Approach.REDIRECT)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_accounts_for_anton_brueckner_ip_address_compute()
                .and()
                .user_anton_brueckner_provided_initial_parameters_to_list_accounts_with_all_accounts_consent_with_ip_address_check()
                .and()
                .open_banking_redirect_from_aspsp_ok_webhook_called();
        then()
                .open_banking_has_consent_for_anton_brueckner_account_list()
                .open_banking_can_read_anton_brueckner_account_data_using_consent_bound_to_service_session();
    }

    @Test
    void testAccountsListWithDedicatedConsentUsingRedirectWithComputedIpAddress() {
        given()
                .redirect_mock_of_sandbox_for_anton_brueckner_accounts_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(Approach.REDIRECT)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_accounts_for_anton_brueckner_ip_address_compute()
                .and()
                .user_anton_brueckner_provided_initial_parameters_to_list_accounts_with_dedicated_consent()
                .and()
                .open_banking_redirect_from_aspsp_ok_webhook_called();
        then()
                .open_banking_has_consent_for_anton_brueckner_account_list()
                .open_banking_can_read_anton_brueckner_account_data_using_consent_bound_to_service_session();
    }

    @Test
    void testAccountsListWithConsentUsingRedirectWithComputedIpAddressForUnknownUser() {
        given()
                .redirect_mock_of_sandbox_for_anton_brueckner_accounts_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(Approach.REDIRECT)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_accounts_for_anton_brueckner()
                .and()
                .user_anton_brueckner_provided_initial_parameters_but_without_psu_id_to_list_accounts_with_all_accounts_consent()
                .and()
                .user_anton_brueckner_provided_psu_id_parameter_to_list_accounts_with_all_accounts_consent_with_ip_address_check()
                .and()
                .open_banking_redirect_from_aspsp_ok_webhook_called();
        then()
                .open_banking_has_consent_for_anton_brueckner_account_list()
                .open_banking_can_read_anton_brueckner_account_data_using_consent_bound_to_service_session();
    }

    @Test
    void testAccountsListWithConsentUsingRedirectWithWrongIbans() {
        given()
                .redirect_mock_of_sandbox_for_anton_brueckner_accounts_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(Approach.REDIRECT)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_accounts_for_anton_brueckner()
                .and()
                .user_anton_brueckner_provided_initial_parameters_to_list_accounts_with_wrong_ibans()
                .and()
                .got_503_http_error();
    }

    @ParameterizedTest
    @EnumSource(Approach.class)
    void testTransactionsListWithConsentUsingEmbeddedForUnknownUser(Approach approach) {
        given()
                .embedded_mock_of_sandbox_for_max_musterman_transactions_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(approach)
                .rest_assured_points_to_opba_server();

        when()
                .fintech_calls_list_transactions_for_max_musterman()
                .and()
                .user_max_musterman_provided_initial_parameters_to_list_transactions_but_without_psu_id_with_single_accounts_consent()
                .and()
                .user_max_musterman_provided_psu_id_parameter_to_list_transactions_with_single_account_consent()
                .and()
                .user_max_musterman_provided_password_to_embedded_authorization()
                .and()
                .user_max_musterman_selected_sca_challenge_type_email1_to_embedded_authorization()
                .and()
                .user_max_musterman_provided_sca_challenge_result_to_embedded_authorization_and_sees_redirect_to_fintech_ok();
        then()
                .open_banking_has_consent_for_max_musterman_transaction_list()
                .open_banking_can_read_max_musterman_transactions_data_using_consent_bound_to_service_session(
                        WiremockConst.MAX_MUSTERMAN_RESOURCE_ID, WiremockConst.DATE_FROM, WiremockConst.DATE_TO, WiremockConst.BOTH_BOOKING
                );
    }

    @Test
    void testAccountsListWithConsentUsingRedirectWithoutIpAddress() {
        given()
                .redirect_mock_of_sandbox_for_anton_brueckner_accounts_running()
                .preferred_sca_approach_selected_for_all_banks_in_opba(Approach.REDIRECT)
                .rest_assured_points_to_opba_server();
        when()
                .fintech_calls_list_accounts_for_anton_brueckner_no_ip_address();

        then()
                .user_anton_brueckner_provided_initial_parameters_to_list_accounts_with_all_accounts_consent_and_gets_202();
    }

}
