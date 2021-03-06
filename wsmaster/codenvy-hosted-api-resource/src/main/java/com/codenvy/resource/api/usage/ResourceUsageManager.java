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
package com.codenvy.resource.api.usage;

import com.codenvy.resource.api.AvailableResourcesProvider;
import com.codenvy.resource.api.ResourceAggregator;
import com.codenvy.resource.api.ResourceUsageTracker;
import com.codenvy.resource.api.exception.NoEnoughResourcesException;
import com.codenvy.resource.api.license.AccountLicenseManager;
import com.codenvy.resource.model.Resource;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Facade for resources using related operations.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class ResourceUsageManager {
    private final ResourceAggregator                      resourceAggregator;
    private final Set<ResourceUsageTracker>               usageTrackers;
    private final AccountManager                          accountManager;
    private final AccountLicenseManager                   accountLicenseManager;
    private final Map<String, AvailableResourcesProvider> accountTypeToAvailableResourcesProvider;
    private final DefaultAvailableResourcesProvider       defaultAvailableResourcesProvider;

    @Inject
    public ResourceUsageManager(ResourceAggregator resourceAggregator,
                                Set<ResourceUsageTracker> usageTrackers,
                                AccountManager accountManager,
                                Map<String, AvailableResourcesProvider> accountTypeToAvailableResourcesProvider,
                                AccountLicenseManager accountLicenseManager,
                                DefaultAvailableResourcesProvider defaultAvailableResourcesProvider) {
        this.resourceAggregator = resourceAggregator;
        this.usageTrackers = usageTrackers;
        this.accountManager = accountManager;
        this.accountTypeToAvailableResourcesProvider = accountTypeToAvailableResourcesProvider;
        this.accountLicenseManager = accountLicenseManager;
        this.defaultAvailableResourcesProvider = defaultAvailableResourcesProvider;
    }

    /**
     * Returns list of resources which are available for usage by given account.
     *
     * @param accountId
     *         id of account
     * @return list of resources which are available for usage by given account
     * @throws NotFoundException
     *         when account with specified id was not found
     * @throws ServerException
     *         when some exception occurred while resources fetching
     */
    public List<? extends Resource> getTotalResources(String accountId) throws NotFoundException, ServerException {
        return accountLicenseManager.getByAccount(accountId)
                                    .getTotalResources();
    }

    /**
     * Returns list of resources which are available for usage by given account.
     *
     * @param accountId
     *         id of account
     * @return list of resources which are available for usage by given account
     * @throws NotFoundException
     *         when account with specified id was not found
     * @throws ServerException
     *         when some exception occurred while resources fetching
     */
    public List<? extends Resource> getAvailableResources(String accountId) throws NotFoundException, ServerException {
        final Account account = accountManager.getById(accountId);
        final AvailableResourcesProvider availableResourcesProvider = accountTypeToAvailableResourcesProvider.get(account.getType());

        if (availableResourcesProvider == null) {
            return defaultAvailableResourcesProvider.getAvailableResources(accountId);
        }

        return availableResourcesProvider.getAvailableResources(accountId);
    }

    /**
     * Returns list of resources which are used by given account.
     *
     * @param accountId
     *         id of account
     * @return list of resources which are used by given account
     * @throws NotFoundException
     *         when account with specified id was not found
     * @throws ServerException
     *         when some exception occurred while resources fetching
     */
    public List<? extends Resource> getUsedResources(String accountId) throws NotFoundException, ServerException {
        List<Resource> usedResources = new ArrayList<>();
        for (ResourceUsageTracker usageTracker : usageTrackers) {
            Optional<Resource> usedResource = usageTracker.getUsedResource(accountId);
            usedResource.ifPresent(usedResources::add);
        }
        return usedResources;
    }

    /**
     * Checks that specified account has available resources to use
     *
     * @param accountId
     *         account id
     * @param resources
     *         resources to check availability
     * @throws NotFoundException
     *         when account with specified id was not found
     * @throws NoEnoughResourcesException
     *         when account doesn't have specified available resources
     * @throws ServerException
     *         when any other error occurs
     */
    public void checkResourcesAvailability(String accountId, List<? extends Resource> resources) throws NotFoundException,
                                                                                                        NoEnoughResourcesException,
                                                                                                        ServerException {
        List<? extends Resource> availableResources = getAvailableResources(accountId);
        //check resources availability
        resourceAggregator.deduct(availableResources, resources);
    }
}
