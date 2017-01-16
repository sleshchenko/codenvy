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
package com.codenvy.organization.api.personal;

import com.codenvy.organization.api.OrganizationManager;
import com.codenvy.organization.shared.model.Organization;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.everrest.CheMethodInvokerFilter;
import org.everrest.core.Filter;
import org.everrest.core.resource.GenericResourceMethod;

import javax.inject.Inject;
import javax.ws.rs.Path;

/**
 * Restricts access to remove/rename personal organization
 *
 * @author Sergii Leschenko
 */
@Filter
@Path("/organization{path:(?!/resource)(/.*)?}")
public class PersonalOrganizationPermissionsFilter extends CheMethodInvokerFilter {
    static final String UPDATE_METHOD = "update";
    static final String REMOVE_METHOD = "remove";

    private final OrganizationManager organizationManager;
    private final UserManager         userManager;

    @Inject
    public PersonalOrganizationPermissionsFilter(OrganizationManager organizationManager,
                                                 UserManager userManager) {
        this.organizationManager = organizationManager;
        this.userManager = userManager;
    }

    @Override
    public void filter(GenericResourceMethod genericResourceMethod, Object[] arguments) throws ForbiddenException,
                                                                                               NotFoundException,
                                                                                               ServerException {
        final String methodName = genericResourceMethod.getMethod().getName();
        switch (methodName) {
            case UPDATE_METHOD:
            case REMOVE_METHOD:
                Organization organization = organizationManager.getById((String)arguments[0]);
                try {
                    userManager.getByName(organization.getName());
                    throw new ForbiddenException("It is not allow to rename/remove personal organization.");
                } catch (NotFoundException ignored) {
                    // requested organization is not personal
                }
                break;
            default:
                //do nothing
        }
    }
}
