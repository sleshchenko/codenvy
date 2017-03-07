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

import com.codenvy.api.dao.authentication.AccessTicket;
import com.codenvy.api.dao.authentication.CookieBuilder;
import com.codenvy.api.dao.authentication.TicketManager;
import com.codenvy.api.dao.authentication.TokenGenerator;
import com.codenvy.api.license.server.SystemLicenseManager;
import com.codenvy.auth.sso.bearer.shared.dto.CredentialsDto;
import com.codenvy.auth.sso.bearer.shared.dto.RegistrationDataDto;
import com.codenvy.auth.sso.server.organization.UserCreationValidator;
import com.codenvy.auth.sso.server.organization.UserCreator;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.user.User;
import org.eclipse.che.api.user.server.UserValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Map;

import static com.codenvy.api.license.shared.model.Constants.FAIR_SOURCE_LICENSE_IS_NOT_ACCEPTED_MESSAGE;
import static com.codenvy.api.license.shared.model.Constants.UNABLE_TO_ADD_ACCOUNT_BECAUSE_OF_LICENSE;
import static java.util.Collections.singletonMap;

/**
 * Service to validate users using bearer tokens.
 *
 * @author Alexander Garagatyi
 * @author Sergey Kabashniuk
 */
@Path("internal/token")
public class BearerTokenAuthenticationService {
    private static final Logger LOG = LoggerFactory.getLogger(BearerTokenAuthenticationService.class);

    private final TicketManager         ticketManager;
    private final TokenGenerator        tokenGenerator;
    private final BearerTokens          bearerTokens;
    private final CookieBuilder         cookieBuilder;
    private final UserCreationValidator creationValidator;
    private final UserCreator           userCreator;
    private final UserValidator         userNameValidator;
    private final SystemLicenseManager  licenseManager;
    private final VerifyEmailMailSender verifyEmailMailSender;

    @Context
    private UriInfo uriInfo;

    @Inject
    public BearerTokenAuthenticationService(TicketManager ticketManager,
                                            TokenGenerator tokenGenerator,
                                            BearerTokens bearerTokens,
                                            CookieBuilder cookieBuilder,
                                            UserCreationValidator creationValidator,
                                            UserCreator userCreator,
                                            UserValidator userNameValidator,
                                            SystemLicenseManager licenseManager,
                                            VerifyEmailMailSender verifyEmailMailSender) {
        this.ticketManager = ticketManager;
        this.tokenGenerator = tokenGenerator;
        this.bearerTokens = bearerTokens;
        this.cookieBuilder = cookieBuilder;
        this.creationValidator = creationValidator;
        this.userCreator = userCreator;
        this.userNameValidator = userNameValidator;
        this.licenseManager = licenseManager;
        this.verifyEmailMailSender = verifyEmailMailSender;
    }

    /**
     * Authenticates user by provided token, then creates the user
     * and sets the access/logged in cookies.
     *
     * @param credentialsDto
     *         user credentials
     * @return principal user principal
     */
    @POST
    @Path("authenticate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticate(@CookieParam("session-access-key") Cookie tokenAccessCookie,
                                 CredentialsDto credentialsDto) throws ForbiddenException,
                                                                       BadRequestException,
                                                                       ServerException {
        boolean isSecure = uriInfo.getRequestUri().getScheme().equals("https");

        if (!bearerTokens.isValid(credentialsDto.getToken())) {
            throw new BadRequestException("Provided token is not valid");
        }
        Map<String, String> payload = bearerTokens.getPayload(credentialsDto.getToken());
        bearerTokens.validate(credentialsDto.getToken());

        final String username = userNameValidator.normalizeUserName(payload.get("username"));
        User user;
        try {
            user = userCreator.createUser(payload.get("email"), username, payload.get("firstName"), payload.get("lastName"));
        } catch (IOException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }

        if (tokenAccessCookie != null) {
            AccessTicket accessTicket = ticketManager.getAccessTicket(tokenAccessCookie.getValue());
            if (accessTicket != null) {
                if (!user.getId().equals(accessTicket.getUserId())) {
                    // DO NOT REMOVE! This log will be used in statistic analyzing
                    LOG.info("EVENT#user-changed-name# OLD-USER#{}# NEW-USER#{}#",
                             accessTicket.getUserId(),
                             user.getId());
                    LOG.info("EVENT#user-sso-logged-out# USER#{}#", accessTicket.getUserId());
                    // DO NOT REMOVE! This log will be used in statistic analyzing
                    ticketManager.removeTicket(accessTicket.getAccessToken());
                }
            }
        }

        if (payload.containsKey("initiator")) {
            // DO NOT REMOVE! This log will be used in statistic analyzing
            LOG.info("EVENT#user-sso-logged-in# USING#{}# USER#{}#", payload.get("initiator"), username);
        }

        // If we obtained principal  - authentication is done.
        String token = tokenGenerator.generate();
        ticketManager.putAccessTicket(new AccessTicket(token, user.getId(), "bearer"));

        Response.ResponseBuilder builder = Response.ok();
        cookieBuilder.setCookies(builder, token, isSecure);

        builder.entity(singletonMap("token", token));

        bearerTokens.invalidate(credentialsDto.getToken());

        LOG.debug("Authenticate user {} with token {}", username, token);
        return builder.build();
    }

    /**
     * Validates user email and user name,
     * then sends mail with one-time token that allows to sign up.
     *
     * @param registrationDataDto
     *         email and user name for validation
     */
    @POST
    @Path("validate")
    @Consumes(MediaType.APPLICATION_JSON)
    public void validate(RegistrationDataDto registrationDataDto) throws BadRequestException,
                                                                         ForbiddenException,
                                                                         ConflictException,
                                                                         ServerException {
        String email = registrationDataDto.getEmail();
        String username = registrationDataDto.getUsername();

        creationValidator.ensureUserCreationAllowed(email, username);

        if (!licenseManager.isFairSourceLicenseAccepted()) {
            throw new ForbiddenException(FAIR_SOURCE_LICENSE_IS_NOT_ACCEPTED_MESSAGE);
        }

        if (!licenseManager.canUserBeAdded()) {
            throw new ForbiddenException(UNABLE_TO_ADD_ACCOUNT_BECAUSE_OF_LICENSE);
        }

        verifyEmailMailSender.sendConfirmationEmail(email,
                                                    bearerTokens.generate(email, username,
                                                                          singletonMap("initiator", "email")),
                                                    uriInfo.getRequestUri().getQuery(),
                                                    uriInfo.getBaseUriBuilder().replacePath(null).build().toString());
    }
}
