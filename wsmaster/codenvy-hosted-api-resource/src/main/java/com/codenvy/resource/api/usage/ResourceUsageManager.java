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

import com.codenvy.resource.api.exception.NoEnoughResourcesException;
import com.codenvy.resource.api.ResourceAggregator;
import com.codenvy.resource.api.ResourceLockProvider;
import com.codenvy.resource.api.ResourceUsageTracker;
import com.codenvy.resource.api.ResourcesReserveTracker;
import com.codenvy.resource.api.license.LicenseManager;
import com.codenvy.resource.model.Resource;
import com.codenvy.resource.spi.impl.ResourceImpl;
import com.google.common.util.concurrent.Striped;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * Facade for resources using related operations.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class ResourceUsageManager {
    @FunctionalInterface
    public interface ResourceUsageOperation<T, X extends Throwable> {
        T use() throws X;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ResourceUsageManager.class);

    private static final Striped<Lock> RESOURCES_LOCKS = Striped.lazyWeakLock(100);

    private final ResourceAggregator                   resourceAggregator;
    private final Set<ResourceUsageTracker>            usageTrackers;
    private final AccountManager                       accountManager;
    private final Map<String, ResourcesReserveTracker> accountTypeToReserveTracker;
    private final LicenseManager                       licenseManager;
    private final Map<String, ResourceLockProvider>    accountTypeToLockProvider;

    @Inject
    public ResourceUsageManager(ResourceAggregator resourceAggregator,
                                Set<ResourceUsageTracker> usageTrackers,
                                Set<ResourcesReserveTracker> resourcesReserveTrackers,
                                AccountManager accountManager,
                                LicenseManager licenseManager,
                                Set<ResourceLockProvider> resourceLockProviders) {
        this.resourceAggregator = resourceAggregator;
        this.usageTrackers = usageTrackers;
        this.accountManager = accountManager;
        this.licenseManager = licenseManager;
        this.accountTypeToReserveTracker = resourcesReserveTrackers.stream()
                                                                   .collect(toMap(ResourcesReserveTracker::getAccountType,
                                                                                  Function.identity()));
        this.accountTypeToLockProvider = resourceLockProviders.stream()
                                                              .collect(Collectors.toMap(ResourceLockProvider::getAccountType,
                                                                                        Function.identity()));
    }

    /**
     * Lock resources for performing some operation with them.
     *
     * <p>Note: should be invoked while consuming operation starts
     *
     * @param accountId
     *         account id which will use resources
     * @param resources
     *         resources which should be reserved
     * @param operation
     *         operation that start resources usage
     * @return result of resources usage operation
     * @throws NoEnoughResourcesException
     *         when given account doesn't have enough resources to use
     * @throws NotFoundException
     *         when {@code resources} list contains resource with not supported type
     * @throws ServerException
     *         when some exception occurred while available resources fetching
     * @throws X
     *         when some exception occurred while operation performing
     */
    public <T, X extends Throwable> T lockResources(String accountId,
                                                    List<? extends Resource> resources,
                                                    ResourceUsageOperation<T, X> operation) throws X,
                                                                                                   NotFoundException,
                                                                                                   ServerException,
                                                                                                   NoEnoughResourcesException {
        final Account account = accountManager.getById(accountId);
        final ResourceLockProvider resourceLockProvider = accountTypeToLockProvider.get(account.getType());
        String lockId;
        if (resourceLockProvider == null) {
            // this account type doesn't have custom lock provider.
            // Lock resources by current account
            lockId = accountId;
        } else {
            lockId = resourceLockProvider.getLockId(accountId);
        }

        final Lock lock = RESOURCES_LOCKS.get(lockId);
        lock.lock();
        try {
            List<? extends Resource> availableResources = getAvailableResources(accountId);
            //check resources availability
            resourceAggregator.deduct(availableResources, resources);
            return operation.use();
        } finally {
            lock.unlock();
        }
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
        final Account account = accountManager.getById(accountId);
        final ResourcesReserveTracker resourcesReserveTracker = accountTypeToReserveTracker.get(account.getType());
        final List<? extends Resource> licenseResources = licenseManager.getByAccount(accountId)
                                                                        .getTotalResources();

        if (resourcesReserveTracker == null) {
            return licenseResources;
        }

        try {
            return resourceAggregator.deduct(licenseResources,
                                             resourcesReserveTracker.getReservedResources(accountId));
        } catch (NoEnoughResourcesException e) {
            //TODO What should we do in this case
            final String variable = "Number of reserved resources is greater than resources provided by license.";
            LOG.error(variable, e);
            throw new ServerException(variable);
        }
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
        try {
            return resourceAggregator.deduct(getTotalResources(accountId),
                                             getUsedResources(accountId));
        } catch (NoEnoughResourcesException e) {
            //TODO What should we do in this case
            final String variable = "Number of used resources more than total resources";
            LOG.error(variable, e);
            throw new ServerException(variable);
        }
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
        List<ResourceImpl> usedResources = new ArrayList<>();
        for (ResourceUsageTracker usageTracker : usageTrackers) {
            usedResources.add(usageTracker.getUsedResource(accountId));
        }
        return usedResources;
    }
}
