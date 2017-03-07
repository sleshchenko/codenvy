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
package com.codenvy.auth.sso.server;

import com.codenvy.auth.sso.bearer.server.BearerTokens;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.model.user.User;
import org.eclipse.che.api.user.server.TokenValidator;
import org.eclipse.che.api.user.server.model.impl.UserImpl;

import javax.inject.Inject;
import java.util.Map;

/**
 * Token validator implementation.
 */
public class BearerTokenValidator implements TokenValidator {

    @Inject
    private BearerTokens bearerTokens;

    @Override
    public User validateToken(String token) throws ConflictException {
        Map<String, String> payload = bearerTokens.getPayload(token);
        if (!bearerTokens.isValid(token)) {
            throw new ConflictException("Cannot create user - authentication token is invalid. Request a new one.");
        }

        String username = payload.get("username");
        String email = payload.get("email");
        if (username == null || email == null) {
            throw new ConflictException("Cannot create user - authentication token is invalid. Request a new one.");
        }

        return new UserImpl(null, email, username);
    }
}
