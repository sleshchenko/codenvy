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

import com.codenvy.api.license.server.SystemLicenseManager;
import com.codenvy.auth.sso.server.organization.UserCreator;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.user.User;
import org.eclipse.che.api.user.server.PreferenceManager;
import org.eclipse.che.api.user.server.ProfileManager;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.api.user.server.model.impl.ProfileImpl;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.commons.lang.NameGenerator;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.codenvy.api.license.shared.model.Constants.FAIR_SOURCE_LICENSE_IS_NOT_ACCEPTED_MESSAGE;

/**
 * @author Sergii Kabashniuk
 */
public class OrgServiceUserCreator implements UserCreator {
    private final UserManager          userManager;
    private final ProfileManager       profileManager;
    private final PreferenceManager    preferenceManager;
    private final SystemLicenseManager licenseManager;
    private final boolean              userSelfCreationAllowed;

    @Inject
    public OrgServiceUserCreator(UserManager userManager,
                                 ProfileManager profileManager,
                                 PreferenceManager preferenceManager,
                                 SystemLicenseManager licenseManager,
                                 @Named("che.auth.user_self_creation") boolean userSelfCreationAllowed) {
        this.userManager = userManager;
        this.profileManager = profileManager;
        this.preferenceManager = preferenceManager;
        this.licenseManager = licenseManager;
        this.userSelfCreationAllowed = userSelfCreationAllowed;
    }

    @Override
    public User createUser(String email, String userName, String firstName, String lastName) throws IOException {
        //TODO check this method should only call if user is not exists.
        try {
            return userManager.getByEmail(email);
        } catch (NotFoundException e) {
            try {
                if (!licenseManager.isFairSourceLicenseAccepted()) {
                    throw new ForbiddenException(FAIR_SOURCE_LICENSE_IS_NOT_ACCEPTED_MESSAGE);
                }

                if (!licenseManager.canUserBeAdded()) {
                    throw new ForbiddenException("Unable to add your account. The Codenvy license has reached its user limit.");
                }
            } catch (Exception ex) {
                throw new IOException(ex.getLocalizedMessage(), ex);
            }

            if (!userSelfCreationAllowed) {
                throw new IOException("Currently only admins can create accounts. Please contact our Admin Team for further info.");
            }

            final Map<String, String> attributes = new HashMap<>();
            if (firstName != null) {
                attributes.put("firstName", firstName);
            }
            if (lastName != null) {
                attributes.put("lastName", lastName);
            }
            attributes.put("email", email);


            try {
                User user = createNonReservedUser(userName, email);
                while (user == null) {
                    user = createNonReservedUser(NameGenerator.generate(userName, 4), email);
                }

                ProfileImpl profile = new ProfileImpl(profileManager.getById(user.getId()));
                profile.getAttributes().putAll(attributes);
                profileManager.update(profile);

                final Map<String, String> preferences = new HashMap<>();
                preferences.put("codenvy:created", Long.toString(System.currentTimeMillis()));
                preferences.put("resetPassword", "true");
                preferenceManager.save(user.getId(), preferences);

                return user;
            } catch (NotFoundException | ServerException ex) {
                throw new IOException(ex.getLocalizedMessage(), ex);
            }
        } catch (ServerException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Create user via user manager, ensuring the name is not reserved and conflicting.
     *
     * @param username
     *         user name
     * @param email
     *         user email
     * @return created user if successfully created, null otherwise
     * @throws ServerException
     *         when exception occurs during user creation
     */
    private User createNonReservedUser(String username, String email) throws ServerException {
        try {
            userManager.create(new UserImpl(null, email, username), false);
            return userManager.getByName(username);
        } catch (ServerException | NotFoundException e) {
            throw new ServerException(e);
        } catch (ConflictException e) {
            return null;
        }
    }
}
