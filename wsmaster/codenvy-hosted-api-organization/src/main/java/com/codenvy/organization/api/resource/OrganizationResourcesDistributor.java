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

import com.codenvy.organization.api.OrganizationManager;
import com.codenvy.organization.api.event.BeforeOrganizationRemovedEvent;
import com.codenvy.organization.shared.model.OrganizationDistributedResources;
import com.codenvy.organization.spi.OrganizationDistributedResourcesDao;
import com.codenvy.organization.spi.impl.OrganizationDistributedResourcesImpl;
import com.codenvy.resource.api.ResourceAggregator;
import com.codenvy.resource.api.exception.NoEnoughResourcesException;
import com.codenvy.resource.api.usage.ResourceUsageManager;
import com.codenvy.resource.api.usage.ResourcesLocks;
import com.codenvy.resource.model.Resource;
import com.google.common.annotations.VisibleForTesting;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.commons.lang.concurrent.Unlocker;
import org.eclipse.che.core.db.cascade.CascadeEventSubscriber;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * TODO Revise docs
 *
 * Facade for organization resources operations.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class OrganizationResourcesDistributor {
    private final OrganizationDistributedResourcesDao organizationDistributedResourcesDao;
    private final OrganizationManager                 organizationManager;
    private final ResourcesLocks                      resourcesLocks;
    private final ResourceUsageManager                usageManager;
    private final ResourceAggregator                  resourceAggregator;

    @Inject
    public OrganizationResourcesDistributor(OrganizationDistributedResourcesDao organizationDistributedResourcesDao,
                                            OrganizationManager organizationManager,
                                            ResourcesLocks resourcesLocks,
                                            ResourceUsageManager usageManager,
                                            ResourceAggregator resourceAggregator) {
        this.organizationDistributedResourcesDao = organizationDistributedResourcesDao;
        this.organizationManager = organizationManager;
        this.resourcesLocks = resourcesLocks;
        this.usageManager = usageManager;
        this.resourceAggregator = resourceAggregator;
    }

    @Inject
    public void subscribe(EventService eventService) {
        eventService.subscribe(new RemoveOrganizationDistributedResourcesSubscriber(), BeforeOrganizationRemovedEvent.class);
    }

    /**
     * Cap usage of shared resources.
     *
     * <p>By default suborganization is able to use all parent organization resources
     * Cap allow to limit usage of shared resources by suborganization.
     *
     * @param suborganizationId
     *         suborganization id
     * @param resourcesCaps
     *         resources to capped
     * @throws NotFoundException
     *         when specified suborganization was not found
     * @throws ConflictException
     *         when suborganization is currently using more shared resources than should be capped
     * @throws ServerException
     *         when any other error occurs
     */
    public void cap(String suborganizationId, List<? extends Resource> resourcesCaps) throws NotFoundException,
                                                                                             ConflictException,
                                                                                             ServerException {
        requireNonNull(suborganizationId, "Required non-null suborganization id");
        requireNonNull(resourcesCaps, "Required non-null resources to capResources");
        checkIsSuborganization(suborganizationId);

        // locking resources by suborganization should lock resources whole organization tree
        // so we can check resource availability for suborganization organization
        try (@SuppressWarnings("unused") Unlocker u = resourcesLocks.lock(suborganizationId)) {
            if (resourcesCaps.isEmpty()) {
                organizationDistributedResourcesDao.remove(suborganizationId);
            } else {
                checkResourcesAvailability(suborganizationId,
                                           getResourcesCaps(suborganizationId),
                                           resourcesCaps);

                organizationDistributedResourcesDao.store(new OrganizationDistributedResourcesImpl(suborganizationId, resourcesCaps));
            }
        }
    }

    /**
     * Returns resources cap or empty list.
     *
     * @param suborganizationId
     *         suborganization id to fetch resources cap
     * @return resources cap or empty list
     * @throws NotFoundException
     *         when specified suborganization was not found
     * @throws ConflictException
     *         when suborganization is currently using more shared resources than should be capped
     * @throws ServerException
     *         when any other error occurs
     */
    public List<? extends Resource> getResourcesCaps(String suborganizationId) throws NotFoundException,
                                                                                      ConflictException,
                                                                                      ServerException {
        requireNonNull(suborganizationId, "Required non-null suborganization id");
        checkIsSuborganization(suborganizationId);
        try {
            return organizationDistributedResourcesDao.get(suborganizationId).getResourcesCap();
        } catch (NotFoundException e) {
            return emptyList();
        }
    }

    /**
     * Returns distributed resources for specified suborganization.
     *
     * @param suborganizationId
     *         organization id
     * @return distributed resources for suborganization with specified id
     * @throws NullPointerException
     *         when either {@code suborganizationId} is null
     * @throws NotFoundException
     *         when there is not distributed resources for specified suborganization
     * @throws ServerException
     *         when any other error occurs
     */
    public OrganizationDistributedResources get(String suborganizationId) throws NotFoundException, ServerException {
        requireNonNull(suborganizationId, "Required non-null organization id");

        return organizationDistributedResourcesDao.get(suborganizationId);
    }

    /**
     * Returns distributed resources for suborganizations by specified parent organization.
     *
     * @param organizationId
     *         organization id
     * @return distributed resources for suborganizations by specified parent organization
     * @throws NullPointerException
     *         when either {@code organizationId} is null
     * @throws ServerException
     *         when any other error occurs
     */
    public Page<? extends OrganizationDistributedResources> getByParent(String organizationId,
                                                                        int maxItems,
                                                                        long skipCount) throws ServerException {
        requireNonNull(organizationId, "Required non-null organization id");

        return organizationDistributedResourcesDao.getByParent(organizationId, maxItems, skipCount);
    }

    /**
     * Checks that parent organization and suborganization have enough available resources to perform resources distribution.
     *
     * <p>Parent organization should have enough available resource to capResources it initially or increase number of distributed one.
     * Suborganization should not use resource and it should not be distributed for its
     * suborganizations in case decreasing number or resetting of distributed one.
     *
     * @param suborganizationId
     *         identifier of suborganization
     * @param newResourcesCap
     *         resources to capResources
     * @param existingResourcesCap
     *         resources which are already distributed
     * @throws ConflictException
     *         when parent organization doesn't have enough resources to increase distributed resource amount
     * @throws ConflictException
     *         when resources can't be distributed because suborganization is using existing resources
     *         or when they are distributed to next organizations level
     * @throws ServerException
     *         when any other error occurs
     */
    @VisibleForTesting
    void checkResourcesAvailability(String suborganizationId,
                                    List<? extends Resource> existingResourcesCap,
                                    List<? extends Resource> newResourcesCap) throws NotFoundException,
                                                                                     ConflictException,
                                                                                     ServerException {

        List<Resource> suborganizationResourcesToCheck = new ArrayList<>();

        final Map<String, Resource> resourceTypeToExistingCap = existingResourcesCap.stream()
                                                                                    .collect(Collectors.toMap(Resource::getType,
                                                                                                              Function.identity()));

        for (Resource newResourceCap : newResourcesCap) {
            final Resource existingResourceCap = resourceTypeToExistingCap.get(newResourceCap.getType());
            if (existingResourceCap != null) {
                try {
                    resourceAggregator.deduct(existingResourceCap, newResourceCap);
                    // distributed resource amount is less than new one
                    // we should check availability of difference resource on suborganization level
                    suborganizationResourcesToCheck.add(newResourceCap);
                } catch (NoEnoughResourcesException e) {
                    // new resource capResources is greater than new one
                }
            } else {
                suborganizationResourcesToCheck.add(newResourceCap);
            }
        }

        if (!suborganizationResourcesToCheck.isEmpty()) {
            Map<String, Resource> usedResources = usageManager.getUsedResources(suborganizationId)
                                                              .stream()
                                                              .collect(Collectors.toMap(Resource::getType, Function.identity()));
            for (Resource resourceToCheck : suborganizationResourcesToCheck) {

                Resource usedResource = usedResources.get(resourceToCheck.getType());
                if (usedResource != null) {
                    try {
                        resourceAggregator.deduct(resourceToCheck, usedResource);
                    } catch (NoEnoughResourcesException e) {
                        throw new ConflictException("Resources are currently in use. You can't decrease them, while they are " +
                                                    "used. Free resources, by stopping workspaces, before changing the resources caps.");
                    }
                }
            }
        }
    }

    private String checkIsSuborganization(String organizationId) throws NotFoundException, ConflictException, ServerException {
        String parentOrganization = organizationManager.getById(organizationId).getParent();
        if (parentOrganization == null) {
            throw new ConflictException("It is not allowed to capResources resources for root organization.");
        }
        return parentOrganization;
    }

    class RemoveOrganizationDistributedResourcesSubscriber extends CascadeEventSubscriber<BeforeOrganizationRemovedEvent> {
        @Override
        public void onCascadeEvent(BeforeOrganizationRemovedEvent event) throws ServerException {
            if (event.getOrganization().getParent() != null) {
                organizationDistributedResourcesDao.remove(event.getOrganization().getId());
            }
        }
    }
}
