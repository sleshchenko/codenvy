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
package com.codenvy.resources.api.provider;

import com.codenvy.organization.spi.impl.OrganizationImpl;
import com.codenvy.resources.api.ram.RamResource;
import com.codenvy.resources.spi.impl.ProvidedResourceImpl;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.commons.lang.Size;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * Provides free resources for account usage
 *
 * @author Sergii Leschenko
 */
public class FreeResourcesProvider implements ResourcesProvider {
    public static final String FREE_RESOURCES_PROVIDER = "free";

    private final AccountManager accountManager;
    private final long           ramPerUser;
    private final long           ramPerOrganization;

    @Inject
    public FreeResourcesProvider(AccountManager accountManager,
                                 @Named("limits.user.workspaces.ram") String ramPerUser,
                                 @Named("limits.organization.workspaces.ram") String ramPerOrganization) {
        this.accountManager = accountManager;
        this.ramPerUser = "-1".equals(ramPerUser) ? -1 : Size.parseSizeToMegabytes(ramPerUser);
        this.ramPerOrganization = "-1".equals(ramPerOrganization) ? -1 : Size.parseSizeToMegabytes(ramPerOrganization);
    }

    @Override
    public List<ProvidedResourceImpl> getResources(String accountId) throws ServerException, NotFoundException {
        final Account account = accountManager.getById(accountId);
        if (UserImpl.PERSONAL_ACCOUNT.equals(account.getType())) {
            return singletonList(new ProvidedResourceImpl(FREE_RESOURCES_PROVIDER,
                                                          null,
                                                          accountId,
                                                          -1L,
                                                          -1L,
                                                          singletonList(new RamResource(ramPerUser))));
        } else if (OrganizationImpl.ORGANIZATIONAL_ACCOUNT.equals(account.getType())) {
            return singletonList(new ProvidedResourceImpl(FREE_RESOURCES_PROVIDER,
                                                          null,
                                                          accountId,
                                                          -1L,
                                                          -1L,
                                                          singletonList(new RamResource(ramPerOrganization))));
        }
        //resources for other types of accounts are not specified
        return Collections.emptyList();
    }
}
