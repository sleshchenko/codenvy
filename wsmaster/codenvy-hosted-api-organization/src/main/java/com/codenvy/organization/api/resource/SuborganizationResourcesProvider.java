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
package com.codenvy.organization.api.resource;

import com.codenvy.activity.server.TimeoutResourceType;
import com.codenvy.organization.api.OrganizationManager;
import com.codenvy.organization.spi.impl.OrganizationImpl;
import com.codenvy.resource.api.license.ResourcesProvider;
import com.codenvy.resource.api.usage.ResourceUsageManager;
import com.codenvy.resource.model.ProvidedResources;
import com.codenvy.resource.model.Resource;
import com.codenvy.resource.spi.impl.ProvidedResourcesImpl;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

/**
 * Provides resources that are shared for suborganization by its parent organization
 *
 * @author Sergii Leschenko
 */
@Singleton
public class SuborganizationResourcesProvider implements ResourcesProvider {
    public static final String PARENT_RESOURCES_PROVIDER = "parentOrganization";

    private final AccountManager                             accountManager;
    private final OrganizationManager                        organizationManager;
    private final Provider<OrganizationResourcesDistributor> distributorProvider;
    private final Provider<ResourceUsageManager>             usageManagerProvider;

    @Inject
    public SuborganizationResourcesProvider(AccountManager accountManager,
                                            OrganizationManager organizationManager,
                                            Provider<OrganizationResourcesDistributor> distributorProvider,
                                            Provider<ResourceUsageManager> usageManagerProvider) {
        this.accountManager = accountManager;
        this.organizationManager = organizationManager;
        this.distributorProvider = distributorProvider;
        this.usageManagerProvider = usageManagerProvider;
    }

    @Override
    public List<ProvidedResources> getResources(String accountId) throws ServerException,
                                                                         NotFoundException {
        final Account account = accountManager.getById(accountId);

        String parent;
        if (OrganizationImpl.ORGANIZATIONAL_ACCOUNT.equals(account.getType())
            && (parent = organizationManager.getById(accountId).getParent()) != null) {
            // given account is suborganization's account and can have resources provided by parent
            final List<Resource> sharedResources = getDistributedResources(accountId);

            Optional<? extends Resource> timeout = findTimeoutResource(sharedResources);
            if (!timeout.isPresent()) {
                List<? extends Resource> parentResources = usageManagerProvider.get().getAvailableResources(parent);
                findTimeoutResource(parentResources).ifPresent(sharedResources::add);
            }

            if (!sharedResources.isEmpty()) {
                return singletonList(new ProvidedResourcesImpl(PARENT_RESOURCES_PROVIDER,
                                                               null,
                                                               accountId,
                                                               -1L,
                                                               -1L,
                                                               sharedResources));
            }
        }

        return Collections.emptyList();
    }

    private Optional<? extends Resource> findTimeoutResource(List<? extends Resource> resources) {
        return resources.stream()
                        .filter(resource -> resource.getType().equals(TimeoutResourceType.ID))
                        .findAny();
    }

    private List<Resource> getDistributedResources(String accountId) throws ServerException {
        final List<Resource> distributedResources = new ArrayList<>();
        try {
            distributorProvider.get()
                               .get(accountId)
                               .getResources()
                               .stream()
                               .map(resource -> (Resource)resource)
                               .collect(Collectors.toCollection(() -> distributedResources));
        } catch (NotFoundException ignored) {
        }
        return distributedResources;
    }
}
