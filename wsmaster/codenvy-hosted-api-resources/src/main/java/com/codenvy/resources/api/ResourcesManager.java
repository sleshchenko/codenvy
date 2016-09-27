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
package com.codenvy.resources.api;

import com.codenvy.resources.model.Resource;
import com.codenvy.resources.spi.impl.AbstractResource;
import com.google.common.util.concurrent.Striped;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

/**
 * Facade for Resources related operations.
 *
 * @author gazarenkov
 * @author Sergii Leschenko
 */
@Singleton
public class ResourcesManager {
    @FunctionalInterface
    public interface ResourceUsageOperation<T, X extends Throwable> {
        T use() throws X;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ResourcesManager.class);

    private static final Striped<Lock> RESOURCES_LOCKS = Striped.lazyWeakLock(100);

    private final ResourcesAggregator       resourcesAggregator;
    private final LicenseManager            licenseManager;
    private final Set<ResourceUsageTracker> usageTrackers;

    @Inject
    public ResourcesManager(ResourcesAggregator resourcesAggregator,
                            LicenseManager licenseManager,
                            Set<ResourceUsageTracker> usageTrackers) {
        this.resourcesAggregator = resourcesAggregator;
        this.licenseManager = licenseManager;
        this.usageTrackers = usageTrackers;
    }

    /**
     * Reserve resources for usage.
     *
     * Note: should be invoked while consuming operation starts
     *
     * @param accountId
     *         account id which will use resources
     * @param resources
     *         resources which should be reserved
     * @param operation
     *         operation that start resources usage
     * @throws ConflictException
     *         when given account doesn't have enough resources to use
     * @throws NotFoundException
     *         when {@code resources} list contains resource with not supported type
     * @throws ServerException
     *         when some exception occurred while available resources fetching
     * @throws X
     *         when some exception occurred while operation performing
     */
    public <T, X extends Throwable> T reserveResources(String accountId,
                                                       List<AbstractResource> resources,
                                                       ResourceUsageOperation<T, X> operation) throws X,
                                                                                                      NotFoundException,
                                                                                                      ServerException,
                                                                                                      ConflictException {
        final Lock lock = RESOURCES_LOCKS.get(accountId);
        lock.lock();
        try {
            List<? extends Resource> availableResources = getAvailableResources(accountId);
            //check resources availability
            resourcesAggregator.deduct(availableResources, resources);
            return operation.use();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns list of resources which are available for given account
     *
     * @param accountId
     *         id of account
     * @throws NotFoundException
     *         when account with specified id was not found
     * @throws ServerException
     *         when some exception occurred while resources fetching
     */
    public List<? extends Resource> getTotalResources(String accountId) throws NotFoundException, ServerException {
        return licenseManager.getByAccount(accountId).getTotalResources();
    }

    /**
     * Returns list of resources which are available for usage by given account
     *
     * @param accountId
     *         id of account
     * @throws NotFoundException
     *         when account with specified id was not found
     * @throws ServerException
     *         when some exception occurred while resources fetching
     */
    public List<? extends Resource> getAvailableResources(String accountId) throws NotFoundException, ServerException {
        try {
            return resourcesAggregator.deduct(getTotalResources(accountId),
                                              getUsedResources(accountId));
        } catch (ConflictException e) {
            // should not happen
            LOG.error("Number of used resources more than total resources", e);
            throw new ServerException(e.getMessage(), e);
        }
    }

    /**
     * Returns list of resources which are used by given account
     *
     * @param accountId
     *         id of account
     * @throws NotFoundException
     *         when account with specified id was not found
     * @throws ServerException
     *         when some exception occurred while resources fetching
     */
    public List<? extends Resource> getUsedResources(String accountId) throws NotFoundException, ServerException {
        List<AbstractResource> usedResources = new ArrayList<>();
        for (ResourceUsageTracker usageTracker : usageTrackers) {
            usedResources.add(usageTracker.getUsedResource(accountId));
        }
        return usedResources;
    }
}
