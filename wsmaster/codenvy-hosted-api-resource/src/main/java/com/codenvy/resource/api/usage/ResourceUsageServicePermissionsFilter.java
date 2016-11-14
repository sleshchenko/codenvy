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
package com.codenvy.resource.api.usage;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.everrest.CheMethodInvokerFilter;
import org.everrest.core.Filter;
import org.everrest.core.resource.GenericResourceMethod;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * Restricts access to methods of {@link ResourceUsageService} by users' permissions.
 *
 * <p>Filter contains rules for protecting of all methods of {@link ResourceUsageService}.<br>
 * In case when requested method is unknown filter throws {@link ForbiddenException}
 *
 * @author Sergii Leschenko
 */
@Filter
@Path("/resource{path:(?!/free)(/.*)?}")
public class ResourceUsageServicePermissionsFilter extends CheMethodInvokerFilter {
    static final String GET_TOTAL_RESOURCES_METHOD     = "getTotalResources";
    static final String GET_AVAILABLE_RESOURCES_METHOD = "getAvailableResources";
    static final String GET_USED_RESOURCES_METHOD      = "getUsedResources";

    private final AccountManager                                  accountManager;
    private final Map<String, ResourceServicesPermissionsChecker> permissionsCheckers;

    @Inject
    public ResourceUsageServicePermissionsFilter(AccountManager accountManager,
                                                 Set<ResourceServicesPermissionsChecker> permissionsCheckers) {
        this.accountManager = accountManager;
        this.permissionsCheckers = permissionsCheckers.stream()
                                                      .collect(toMap(ResourceServicesPermissionsChecker::getAccountType,
                                                                     Function.identity()));
    }

    @Override
    protected void filter(GenericResourceMethod genericMethodResource, Object[] arguments) throws ApiException {
        // TODO Add ability for admin see organizations' resources
        String accountId;
        switch (genericMethodResource.getMethod().getName()) {
            case GET_TOTAL_RESOURCES_METHOD:
            case GET_AVAILABLE_RESOURCES_METHOD:
            case GET_USED_RESOURCES_METHOD:
                accountId = ((String)arguments[0]);
                break;

            default:
                throw new ForbiddenException("The user does not have permission to perform this operation");
        }
        final Account account = accountManager.getById(accountId);

        final ResourceServicesPermissionsChecker resourceServicesPermissionsChecker = permissionsCheckers.get(account.getType());
        if (resourceServicesPermissionsChecker != null) {
            resourceServicesPermissionsChecker.checkPermissions(accountId);
        } else {
            throw new ForbiddenException("User is not authorized to perform given operation");
        }
    }
}
