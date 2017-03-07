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

import com.codenvy.api.dao.authentication.TokenGenerator;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.commons.schedule.ScheduleRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Provides ability to create temporary one-time token that can be used for authentication.
 *
 * Workflow is following: some part of our code recognize user and want to authenticate it.
 * It generate one bearer token and give user for one time authentication.
 *
 * @author Sergii Kabashniuk
 */
@Singleton
public class BearerTokens {
    private static final Logger LOG = LoggerFactory.getLogger(BearerTokens.class);

    private final int                                                      ticketLifeTimeSeconds;
    private final TokenGenerator                                           tokenGenerator;
    private final ConcurrentMap<String, ConcurrentHashMap<String, String>> tokenMap;

    @Inject
    public BearerTokens(@Named("auth.sso.bearer_ticket_lifetime_seconds") int ticketLifeTimeSeconds,
                        TokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
        this.ticketLifeTimeSeconds = ticketLifeTimeSeconds;
        this.tokenMap = new ConcurrentHashMap<>();
    }

    @ScheduleRate(initialDelay = 50,
                  period = 60)
    public void cleanup() {
        for (String token : tokenMap.keySet()) {
            if (!isValid(token)) {
                LOG.debug("Ticket {} invalidated ", token);
                tokenMap.remove(token);
            }
        }
    }

    /**
     * Generate new bearer token for specified user.
     *
     * @param email
     *         email of the user
     * @param username
     *         username of the user
     * @param payload
     *         map for storing additional information
     * @return token for one time authentication
     */
    public String generate(String email, String username, Map<String, String> payload) {
        String token = tokenGenerator.generate();
        ConcurrentHashMap<String, String> payloadCopy = payload == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(payload);
        payloadCopy.put("email", email);
        payloadCopy.put("username", username);
        payloadCopy.put("creation.time", Long.toString(System.currentTimeMillis()));
        tokenMap.put(token, payloadCopy);
        return token;
    }

    /**
     * Get token payload.
     *
     * @param bearerToken
     *         bearer token that associated with payload.
     * @return map with payload
     */
    public Map<String, String> getPayload(String bearerToken) {
        Map<String, String> payload = tokenMap.get(bearerToken);

        return payload == null ? Collections.emptyMap() : new HashMap<>(payload);
    }

    /**
     * Add some payload to existed token.
     *
     * @param bearerToken
     *         bearer token is going to be associated with payload
     */
    public void addPayload(String bearerToken, Map<String, String> addPayload) {
        Map<String, String> payload = tokenMap.get(bearerToken);
        if (payload != null) {
            payload.putAll(addPayload);
        } else {
            throw new IllegalStateException("Token " + bearerToken + " is not found");
        }
    }

    /**
     * Invalidates specified bearer token.
     *
     * @param bearerToken
     *         bearer token to invalidate
     */
    public void invalidate(String bearerToken) {
        tokenMap.remove(bearerToken);
    }

    /**
     * Validates bearer token.
     *
     * Does nothing if token is valid or throws exception otherwise.
     *
     * @throws ForbiddenException
     *         when token is not valid
     */
    public void validate(String bearerToken) throws ForbiddenException {
        if (isValid(bearerToken)) {
            tokenMap.remove(bearerToken);
        } else {
            throw new ForbiddenException("Authentication of user failed. Token " + bearerToken + " not found or expired.");
        }
    }

    /**
     * Check is bearer token still valid.
     *
     * @param token
     *         bearer token
     * @return true if it is valid, false otherwise
     */
    public boolean isValid(String token) {
        Map<String, String> payload = tokenMap.get(token);
        if (payload != null) {
            // verify token's age
            long creationTime = Long.valueOf(payload.get("creation.time"));
            long currentTime = System.currentTimeMillis();

            return (creationTime + ticketLifeTimeSeconds * 1000) > currentTime;
        }
        return false;
    }
}
