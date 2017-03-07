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

import com.google.common.collect.ImmutableSet;
import com.google.inject.name.Named;

import org.eclipse.che.api.core.BadRequestException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Collections;
import java.util.Set;

/**
 * Validates emails.
 *
 * @author Alexander Garagatyi
 * @author Sergey Kabashniuk
 */
@Singleton
public class EmailValidator {
    public static final String EMAIL_BLACKLIST = "codenvy.email.blacklist";

    private Set<String> emailBlackList = Collections.emptySet();

    @Inject
    public EmailValidator(@Named(EMAIL_BLACKLIST) String[] emailBlacklist) {
        this.emailBlackList = ImmutableSet.copyOf(emailBlacklist);
    }

    public void validateMail(String mail) throws BadRequestException {
        if (mail == null || mail.isEmpty()) {
            throw new BadRequestException("User mail can't be null or ''.");
        }

        try {
            InternetAddress address = new InternetAddress(mail);
            address.validate();
        } catch (AddressException e) {
            throw new BadRequestException("E-Mail validation failed. Please check the format of your e-mail address.");
        }

        // Check blacklist
        for (String current : emailBlackList) {
            if (mail.endsWith(current)) {
                throw new BadRequestException("User mail " + mail + " is forbidden.");
            }
        }
    }
}
