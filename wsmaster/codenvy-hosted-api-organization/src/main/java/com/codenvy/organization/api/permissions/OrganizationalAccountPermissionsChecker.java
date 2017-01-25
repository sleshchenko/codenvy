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
package com.codenvy.organization.api.permissions;

import com.codenvy.api.workspace.server.filters.AccountAction;
import com.codenvy.api.workspace.server.filters.AccountPermissionsChecker;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;

import javax.inject.Singleton;

import static com.codenvy.organization.api.permissions.OrganizationDomain.CREATE_WORKSPACES;
import static com.codenvy.organization.api.permissions.OrganizationDomain.DOMAIN_ID;
import static com.codenvy.organization.api.permissions.OrganizationDomain.MANAGE_RESOURCES;
import static com.codenvy.organization.api.permissions.OrganizationDomain.MANAGE_WORKSPACES;

@Singleton
public class OrganizationalAccountPermissionsChecker implements AccountPermissionsChecker {
    @Override
    public void checkPermissions(String accountId, AccountAction action) throws ForbiddenException {
        Subject subject = EnvironmentContext.getCurrent().getSubject();
        switch (action) {
            case CREATE_WORKSPACE:
                if (!subject.hasPermission(OrganizationDomain.DOMAIN_ID, accountId, OrganizationDomain.CREATE_WORKSPACES)) {
                    throw new ForbiddenException("User is not authorized to create workspaces in specified namespace.");
                }
                break;
            case SEE_RESOURCE:
                if (subject.hasPermission(DOMAIN_ID, accountId, CREATE_WORKSPACES)
                    || subject.hasPermission(DOMAIN_ID, accountId, MANAGE_RESOURCES)
                    || subject.hasPermission(DOMAIN_ID, accountId, MANAGE_WORKSPACES)) {

                    // user is able to see resources usage information
                    return;
                }

                throw new ForbiddenException("User is not authorized to see resources information of requested organization.");
            case MANAGE_WORKSPACES:
                if (!subject.hasPermission(OrganizationDomain.DOMAIN_ID, accountId, OrganizationDomain.MANAGE_WORKSPACES)) {
                    throw new ForbiddenException("User is not authorized to use specified namespace.");
                }
                break;
            default:
                throw new ForbiddenException("User is not authorized to use specified namespace.");
        }
    }
}
