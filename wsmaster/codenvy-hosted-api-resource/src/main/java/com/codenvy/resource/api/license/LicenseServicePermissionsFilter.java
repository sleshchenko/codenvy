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
package com.codenvy.resource.api.license;

import com.codenvy.api.permission.server.account.AccountAction;
import com.codenvy.api.permission.server.account.AccountPermissionsChecker;

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

/**
 * Restricts access to methods of {@link AccountLicenseService} by users' permissions.
 *
 * <p>Filter contains rules for protecting of all methods of {@link AccountLicenseService}.<br>
 * In case when requested method is unknown filter throws {@link ForbiddenException}
 *
 * @author Sergii Leschenko
 */
@Filter
@Path("/license/account{path:(/.*)?}")
public class LicenseServicePermissionsFilter extends CheMethodInvokerFilter {
    public static final String GET_LICENSE_METHOD = "getLicense";

    private final AccountManager                         accountManager;
    private final Map<String, AccountPermissionsChecker> permissionsCheckers;

    @Inject
    public LicenseServicePermissionsFilter(AccountManager accountManager,
                                           Map<String, AccountPermissionsChecker> permissionsCheckers) {
        this.accountManager = accountManager;
        this.permissionsCheckers = permissionsCheckers;
    }

    @Override
    protected void filter(GenericResourceMethod genericMethodResource, Object[] arguments) throws ApiException {
        String accountId;
        switch (genericMethodResource.getMethod().getName()) {
            case GET_LICENSE_METHOD:
                accountId = ((String)arguments[0]);
                break;

            default:
                throw new ForbiddenException("The user does not have permission to perform this operation");
        }

        final Account account = accountManager.getById(accountId);
        final AccountPermissionsChecker permissionsChecker = permissionsCheckers.get(account.getType());
        if (permissionsChecker != null) {
            permissionsChecker.checkPermissions(accountId, AccountAction.SEE_RESOURCE_INFORMATION);
        } else {
            throw new ForbiddenException("User is not authorized to perform given operation");
        }
    }
}
