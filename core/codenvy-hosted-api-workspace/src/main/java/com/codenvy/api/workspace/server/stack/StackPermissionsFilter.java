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
package com.codenvy.api.workspace.server.stack;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.stack.StackService;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.everrest.CheMethodInvokerFilter;
import org.everrest.core.Filter;
import org.everrest.core.resource.GenericMethodResource;

import javax.ws.rs.Path;

/**
 * Restricts access to methods of {@link StackService} by users' permissions
 *
 * <p>Filter should contain rules for protecting of all methods of {@link StackService}.<br>
 * In case when requested method is unknown filter throws {@link ForbiddenException}
 *
 * @author Sergii Leschenko
 */
@Filter
@Path("/stack{path:(/.*)?}")
public class StackPermissionsFilter extends CheMethodInvokerFilter {
    @Override
    public void filter(GenericMethodResource genericMethodResource, Object[] arguments) throws ForbiddenException, ServerException {
        final String methodName = genericMethodResource.getMethod().getName();

        final User currentUser = EnvironmentContext.getCurrent().getUser();
        StackAction action;
        String stackId;

        switch (methodName) {
            case "getStack":
            case "getIcon":
                stackId = ((String)arguments[0]);
                action = StackAction.READ;
                break;

            case "updateStack":
            case "uploadIcon":
                stackId = ((String)arguments[1]);
                action = StackAction.UPDATE;
                break;

            case "removeIcon":
                stackId = ((String)arguments[0]);
                action = StackAction.UPDATE;
                break;

            case "removeStack":
                stackId = ((String)arguments[0]);
                action = StackAction.DELETE;
                break;

            case "createStack":
            case "searchStacks":
                //available for all
                return;
            default:
                throw new ForbiddenException("The user does not have permission to perform this operation");
        }

        if (!currentUser.hasPermission(StackDomain.DOMAIN_ID, stackId, action.toString())) {
            throw new ForbiddenException("The user does not have permission to " + action.toString());
        }
    }
}
