/*
 *  [2012] - [2017] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.auth.sso.bearer.server;

import com.codenvy.api.dao.authentication.CookieBuilder;
import com.codenvy.api.license.server.SystemLicenseManager;
import com.codenvy.api.license.shared.model.Constants;
import com.codenvy.auth.sso.bearer.shared.dto.RegistrationDataDto;
import com.codenvy.auth.sso.server.organization.UserCreationValidator;
import com.codenvy.auth.sso.server.organization.UserCreator;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.assured.EverrestJetty;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Test for {@link BearerTokenAuthenticationService}
 *
 * @author Igor Vinokur
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class BearerTokenAuthenticationServiceTest {

    @SuppressWarnings("unused")
    private static ApiExceptionMapper    apiExceptionMapper;
    @Mock
    private        VerifyEmailMailSender mailSender;
    @Mock
    private        CookieBuilder         cookieBuilder;
    @Mock
    private        UserCreationValidator creationValidator;
    @Mock
    private        UserCreator           userCreator;

    @Mock
    private SystemLicenseManager licenseManager;

    @InjectMocks
    private BearerTokenAuthenticationService bearerTokenAuthenticationService;

    @Test
    public void shouldThrowAnExceptionWhenUserBeyondTheLicense() throws Exception {
        RegistrationDataDto registrationDataDto = DtoFactory.newDto(RegistrationDataDto.class)
                                                            .withEmail("Email")
                                                            .withUsername("UserName");
        when(licenseManager.isFairSourceLicenseAccepted()).thenReturn(true);
        when(licenseManager.canUserBeAdded()).thenReturn(false);

        Response response = given().contentType(ContentType.JSON).content(registrationDataDto).post("/internal/token/validate");

        assertEquals(response.getStatusCode(), 403);
        assertEquals(DtoFactory.getInstance().createDtoFromJson(response.asString(), ServiceError.class),
                     newDto(ServiceError.class).withMessage(Constants.UNABLE_TO_ADD_ACCOUNT_BECAUSE_OF_LICENSE));
        verifyZeroInteractions(mailSender);
    }

    @Test
    public void shouldThrowAnExceptionWhenFairSourceLicenseIsNotAccepted() throws Exception {
        RegistrationDataDto registrationDataDto = DtoFactory.newDto(RegistrationDataDto.class)
                                                            .withEmail("Email")
                                                            .withUsername("UserName");
        when(licenseManager.isFairSourceLicenseAccepted()).thenReturn(false);

        Response response = given().contentType(ContentType.JSON).content(registrationDataDto).post("/internal/token/validate");

        assertEquals(response.getStatusCode(), 403);
        assertEquals(DtoFactory.getInstance().createDtoFromJson(response.asString(), ServiceError.class),
                     newDto(ServiceError.class).withMessage(Constants.FAIR_SOURCE_LICENSE_IS_NOT_ACCEPTED_MESSAGE));
        verifyZeroInteractions(mailSender);
    }
}
