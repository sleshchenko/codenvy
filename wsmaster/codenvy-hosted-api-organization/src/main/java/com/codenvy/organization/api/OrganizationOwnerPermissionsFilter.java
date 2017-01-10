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
package com.codenvy.organization.api;

import com.codenvy.api.permission.shared.dto.PermissionsDto;
import com.codenvy.organization.api.permissions.OrganizationDomain;
import com.codenvy.organization.shared.model.Organization;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.user.User;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.everrest.CheMethodInvokerFilter;
import org.everrest.core.Filter;
import org.everrest.core.resource.GenericResourceMethod;

import javax.inject.Inject;
import javax.ws.rs.Path;

/**
 * Restricts access to setting permissions for owner of organization
 *
 * @author Sergii Leschenko
 */
@Filter
@Path("/permissions/{path:(/.*)?}")
public class OrganizationOwnerPermissionsFilter extends CheMethodInvokerFilter {
    private final OrganizationManager organizationManager;
    private final UserManager         userManager;

    @Inject
    public OrganizationOwnerPermissionsFilter(OrganizationManager organizationManager,
                                              UserManager userManager) {
        this.organizationManager = organizationManager;
        this.userManager = userManager;
    }

    @Override
    public void filter(GenericResourceMethod genericResourceMethod, Object[] arguments) throws ForbiddenException,
                                                                                               NotFoundException,
                                                                                               ServerException {
        final String methodName = genericResourceMethod.getMethod().getName();

        final String domainId;
        final String instanceId;
        final String userId;

        switch (methodName) {
            case "storePermissions":
                final PermissionsDto permissions = (PermissionsDto)arguments[0];
                domainId = permissions.getDomainId();
                userId = permissions.getUserId();
                instanceId = organizationManager.getById(permissions.getInstanceId()).getId();
                break;
            case "removePermissions":
                domainId = ((String)arguments[0]);
                instanceId = ((String)arguments[1]);
                userId = ((String)arguments[2]);
                break;
            default:
                return;
        }

        if (!OrganizationDomain.DOMAIN_ID.equals(domainId)) {
            //filter only setting permissions on organization level
            return;
        }

        User user = userManager.getById(userId);
        Organization organization = organizationManager.getById(instanceId);

        if (organization.getName().equals(user.getName())) {
            throw new ForbiddenException("It is not allowed to edit organization owner's permissions");
        }
    }
}
