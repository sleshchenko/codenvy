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
package com.codenvy.api.workspace.server.filters;

import com.codenvy.api.workspace.server.WorkspaceAction;
import com.codenvy.api.workspace.server.WorkspaceDomain;

import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.WorkspaceService;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.everrest.CheMethodInvokerFilter;
import org.everrest.core.Filter;
import org.everrest.core.GenericContainerResponse;
import org.everrest.core.ResponseFilter;
import org.everrest.core.resource.GenericMethodResource;

import javax.inject.Inject;
import javax.ws.rs.Path;

import static com.google.api.client.repackaged.com.google.common.base.Strings.isNullOrEmpty;

/**
 * Restricts access to methods of {@link WorkspaceService} by users' permissions
 *
 * <p>Filter contains rules for protecting of all methods of {@link WorkspaceService}.<br>
 * In case when requested method is unknown filter throws {@link ForbiddenException}
 *
 * @author Sergii Leschenko
 */
@Filter
@Path("/workspace{path:(/.*)?}")
public class WorkspacePermissionsFilter extends CheMethodInvokerFilter implements ResponseFilter {
    private final AccountDao       accountDao;
    private final WorkspaceManager workspaceManager;
    private final UserManager      userManager;

    @Inject
    public WorkspacePermissionsFilter(AccountDao accountDao,
                                      WorkspaceManager workspaceManager,
                                      UserManager userManager) {
        this.accountDao = accountDao;
        this.workspaceManager = workspaceManager;
        this.userManager = userManager;
    }

    @Override
    public void filter(GenericMethodResource genericMethodResource, Object[] arguments) throws ForbiddenException, ServerException {
        final String methodName = genericMethodResource.getMethod().getName();

        final User currentUser = EnvironmentContext.getCurrent().getUser();
        WorkspaceAction action;
        String workspaceId;

        switch (methodName) {
            case "create": {
                final String accountId = ((String)arguments[3]);
                checkPermissionsToCreateWorkspaces(currentUser, accountId);
                return;
            }

            case "startFromConfig":
                String accountId = ((String)arguments[2]);
                checkPermissionsToCreateWorkspaces(currentUser, accountId);
                return;

            case "delete":
                workspaceId = ((String)arguments[0]);
                action = WorkspaceAction.DELETE;

                try {
                    final Account account = accountDao.getByWorkspace(workspaceId);
                    if (currentUser.hasPermission("account", account.getId(), "deleteWorkspaces")) {
                        //user has permission for removing workspace on account domain level
                        return;
                    }
                } catch (NotFoundException | ServerException e) {
                    //do nothing
                }
                break;

            case "recoverWorkspace":
            case "createMachine":
            case "stop":
            case "startById":
            case "createSnapshot":
                workspaceId = ((String)arguments[0]);
                action = WorkspaceAction.RUN;
                break;

            case "getSnapshot":
                workspaceId = ((String)arguments[0]);
                action = WorkspaceAction.READ;
                break;

            case "getByKey":
                try {
                    workspaceId = getWorkspaceFromKey(((String)arguments[0]));
                } catch (NotFoundException e) {
                    //Can't authorize operation
                    throw new ServerException(e);
                }
                action = WorkspaceAction.READ;
                break;

            case "update":
            case "addProject":
            case "deleteProject":
            case "updateProject":
            case "addEnvironment":
            case "deleteEnvironment":
            case "updateEnvironment":
            case "addCommand":
            case "deleteCommand":
            case "updateCommand":
                workspaceId = ((String)arguments[0]);
                action = WorkspaceAction.CONFIGURE;
                break;

            case "getWorkspaces": //method accessible to every user
                return;

            default:
                throw new ForbiddenException("The user does not have permission to perform this operation");
        }

        if (!currentUser.hasPermission(WorkspaceDomain.DOMAIN_ID, workspaceId, action.toString())) {
            throw new ForbiddenException("The user does not have permission to " + action.toString());
        }
    }

    private void checkPermissionsToCreateWorkspaces(User user, String accountId) throws ForbiddenException {
        if (!isNullOrEmpty(accountId)) {
            if (!user.hasPermission("account", accountId, "createWorkspaces")) {
                throw new ForbiddenException("The user does not have permission to create workspace in given account");
            }
        }
    }

    /**
     * Get workspace id from composite key.
     */
    private String getWorkspaceFromKey(String key) throws NotFoundException, ServerException {
        String[] parts = key.split(":", -1); // -1 is to prevent skipping trailing part
        if (parts.length == 1) {
            return key;
        }
        final String userName = parts[0];
        final String wsName = parts[1];
        final String ownerId = userName.isEmpty() ? EnvironmentContext.getCurrent().getUser().getId()
                                                  : userManager.getByName(userName).getId();
        return workspaceManager.getWorkspace(wsName, ownerId).getId();
    }

    @Override
    public void doFilter(GenericContainerResponse response) {
        //TODO Remove implementing of ResponseFilter when we will use 1.12.2 everrest with changes https://github.com/codenvy/everrest/pull/22
    }
}
