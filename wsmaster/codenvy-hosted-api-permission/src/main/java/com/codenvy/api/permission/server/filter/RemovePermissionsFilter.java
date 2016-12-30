/*
 *  [2012] - [2016] Codenvy, S.A.
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
package com.codenvy.api.permission.server.filter;

import com.codenvy.api.permission.server.SuperPrivilegesChecker;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.everrest.CheMethodInvokerFilter;
import org.everrest.core.Filter;
import org.everrest.core.resource.GenericResourceMethod;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import static com.codenvy.api.permission.server.AbstractPermissionsDomain.SET_PERMISSIONS;

/**
 * Restricts access to removing permissions of instance by users' setPermissions permission
 *
 * @author Sergii Leschenko
 */
@Filter
@Path("/permissions/{domain}")
public class RemovePermissionsFilter extends CheMethodInvokerFilter {
    @PathParam("domain")
    private String domain;

    @QueryParam("instance")
    private String instance;

    @Inject
    private SuperPrivilegesChecker superPrivilegesChecker;

    @Override
    public void filter(GenericResourceMethod genericResourceMethod, Object[] arguments) throws ForbiddenException, ServerException {
        final String methodName = genericResourceMethod.getMethod().getName();
        if (methodName.equals("removePermissions")) {
            if (superPrivilegesChecker.isPrivilegedToManagePermissions(domain)) {
                return;
            }

            if (!EnvironmentContext.getCurrent().getSubject().hasPermission(domain, instance, SET_PERMISSIONS)) {
                throw new ForbiddenException("User can't edit permissions for this instance");
            }
        }
    }
}
