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
package com.codenvy.api.workspace.server.filters;

import com.codenvy.api.permission.server.SuperPrivilegesChecker;
import com.codenvy.organization.api.permissions.OrganizationDomain;
import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.codenvy.api.permission.server.SystemDomain;
import com.codenvy.api.permission.server.account.AccountAction;
import com.codenvy.api.permission.server.account.AccountPermissionsChecker;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.WorkspaceService;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.everrest.CheMethodInvokerFilter;
import org.everrest.core.Filter;
import org.everrest.core.resource.GenericResourceMethod;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.Map;

import static com.codenvy.api.workspace.server.WorkspaceDomain.CONFIGURE;
import static com.codenvy.api.workspace.server.WorkspaceDomain.DELETE;
import static com.codenvy.api.workspace.server.WorkspaceDomain.DOMAIN_ID;
import static com.codenvy.api.workspace.server.WorkspaceDomain.READ;
import static com.codenvy.api.workspace.server.WorkspaceDomain.RUN;
import static com.codenvy.api.workspace.server.WorkspaceDomain.USE;

/**
 * Restricts access to methods of {@link WorkspaceService} by users' permissions.
 *
 * <p>Filter contains rules for protecting of all methods of {@link WorkspaceService}.<br>
 * In case when requested method is unknown filter throws {@link ForbiddenException}
 *
 * @author Sergii Leschenko
 */
@Filter
@Path("/workspace{path:(/.*)?}")
public class WorkspacePermissionsFilter extends CheMethodInvokerFilter {
    private final WorkspaceManager       workspaceManager;
    private final AccountManager         accountManager;
    private final Map<String, AccountPermissionsChecker> accountTypeToPermissionsChecker;
    private final SuperPrivilegesChecker superPrivilegesChecker;

    @Inject
    public WorkspacePermissionsFilter(WorkspaceManager workspaceManager,
                                      AccountManager accountManager,
                                      Map<String, AccountPermissionsChecker> accountTypeToPermissionsChecker,
                                      SuperPrivilegesChecker superPrivilegesChecker) {
        this.workspaceManager = workspaceManager;
        this.accountManager = accountManager;
        this.accountTypeToPermissionsChecker = accountTypeToPermissionsChecker;
        this.superPrivilegesChecker = superPrivilegesChecker;
    }

    @Override
    public void filter(GenericResourceMethod genericResourceMethod, Object[] arguments) throws ForbiddenException,
                                                                                               ServerException,
                                                                                               NotFoundException {
        final String methodName = genericResourceMethod.getMethod().getName();

        final Subject currentSubject = EnvironmentContext.getCurrent().getSubject();
        String action;
        String key;

        switch (methodName) {
            case "getSettings":
            case "getWorkspaces":
                //methods accessible to every user
                return;

            case "getByNamespace": {
                if (superPrivilegesChecker.hasSuperPrivileges()) {
                    return;
                }
                checkAccountPermissions((String)arguments[1], AccountAction.MANAGE_WORKSPACES);
                return;
            }

            case "create": {
                checkAccountPermissions((String)arguments[3], AccountAction.CREATE_WORKSPACE);
                return;
            }

            case "startFromConfig": {
                checkAccountPermissions((String)arguments[2], AccountAction.CREATE_WORKSPACE);
                return;
            }

            case "delete":
                key = ((String)arguments[0]);
                action = DELETE;
                break;

            case "stop":
                if (superPrivilegesChecker.hasSuperPrivileges()) {
                    return;
                }
            case "startById":
            case "createSnapshot":
                key = ((String)arguments[0]);
                action = RUN;
                break;

            case "getSnapshot":
                key = ((String)arguments[0]);
                action = READ;
                break;

            case "getByKey":
                if (superPrivilegesChecker.hasSuperPrivileges()) {
                    return;
                }
            case "checkAgentHealth":
                key = ((String)arguments[0]);
                action = READ;
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
                key = ((String)arguments[0]);
                action = CONFIGURE;
                break;

            // MachineService methods
            case "startMachine":
            case "stopMachine":
                key = ((String)arguments[0]);
                action = RUN;
                break;

            case "getMachineById":
            case "getMachines":
            case "executeCommandInMachine":
            case "getProcesses":
            case "stopProcess":
            case "getProcessLogs":
                key = ((String)arguments[0]);
                action = USE;
                break;

            default:
                throw new ForbiddenException("The user does not have permission to perform this operation");
        }

        final WorkspaceImpl workspace = workspaceManager.getWorkspace(key);
        try {
            checkAccountPermissions(workspace.getNamespace(), AccountAction.MANAGE_WORKSPACES);
            // user is authorized to perform any operation if workspace belongs to account where he has the corresponding permissions
        } catch (ForbiddenException e) {
            //check permissions on workspace level
            if (!currentSubject.hasPermission(DOMAIN_ID, workspace.getId(), action)) {
                throw new ForbiddenException(
                        "The user does not have permission to " + action + " workspace with id '" + workspace.getId() + "'");
            }
        }
    }

    void checkAccountPermissions(String accountName, AccountAction action) throws ForbiddenException,
                                                                                  NotFoundException,
                                                                                  ServerException {
        if (accountName == null) {
            // default namespace will be used
            return;
        }

        final Account account = accountManager.getByName(accountName);

        AccountPermissionsChecker accountPermissionsChecker = accountTypeToPermissionsChecker.get(account.getType());

        if (accountPermissionsChecker == null) {
            throw new ForbiddenException("User is not authorized to use specified namespace");
        }

        accountPermissionsChecker.checkPermissions(account.getId(), action);
    }
}
