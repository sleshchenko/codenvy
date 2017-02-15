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
import com.codenvy.organization.shared.model.Organization;
import com.codenvy.organization.shared.model.OrganizationResources;
import com.codenvy.organization.spi.impl.OrganizationResourcesImpl;
import com.codenvy.resource.api.ResourceAggregator;
import com.codenvy.resource.api.ResourcesReserveTracker;
import com.codenvy.resource.api.exception.NoEnoughResourcesException;
import com.codenvy.resource.api.usage.ResourceUsageManager;
import com.codenvy.resource.model.Resource;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codenvy.organization.spi.impl.OrganizationImpl.ORGANIZATIONAL_ACCOUNT;
import static java.util.Collections.emptyList;

/**
 * Makes organization's resources unavailable for usage when suborganization
 * use shared resources or when parent reserves them for suborganization.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class OrganizationResourcesReserveTracker implements ResourcesReserveTracker {
    static final int ORGANIZATION_PER_PAGE = 100;

    private final OrganizationManager                        organizationManager;
    private final ResourceAggregator                         resourceAggregator;
    private final Provider<OrganizationResourcesDistributor> resourcesDistributorProvider;
    private final Provider<ResourceUsageManager>             usageManagerProvider;

    @Inject
    public OrganizationResourcesReserveTracker(OrganizationManager organizationManager,
                                               ResourceAggregator resourceAggregator,
                                               Provider<OrganizationResourcesDistributor> resourcesDistributorProvider,
                                               Provider<ResourceUsageManager> usageManagerProvider) {

        this.organizationManager = organizationManager;
        this.resourceAggregator = resourceAggregator;
        this.resourcesDistributorProvider = resourcesDistributorProvider;
        this.usageManagerProvider = usageManagerProvider;
    }

    @Override
    public String getAccountType() {
        return ORGANIZATIONAL_ACCOUNT;
    }

    @Override
    public List<? extends Resource> getReservedResources(String accountId) throws ServerException {
        //TODO Add reserve of direct child and used resources whole all tree
        //TODO get used resources whole all tree by busting
        ResourceUsageManager resourceUsageManager = usageManagerProvider.get();
        Page<? extends Organization> suborganizationsPage = organizationManager.getByParent(accountId,
                                                                                            ORGANIZATION_PER_PAGE,
                                                                                            0);
        List<Resource> reservedResources = new ArrayList<>();
        do {
            for (Organization suborganization : suborganizationsPage.getItems()) {
                try {
                    OrganizationResources organizationResources = getOrganizationResources(suborganization.getId());

                    List<? extends Resource> subOrgReserve = organizationResources.getReservedResources();
                    // make unavailable for parent resources that are reserved for suborganization
                    reservedResources.addAll(subOrgReserve);

                    // make unavailable for parent resources that are used from shared heap
                    reservedResources.addAll(getUsedFromHeap(subOrgReserve,
                                                             resourceUsageManager.getUsedResources(suborganization.getId())));

                    //TODO Fix it
//                    reservedResources.addAll(resourceUsageManager.getReservedResources(suborganization.getId()));
                } catch (NotFoundException e) {
                    throw new ServerException(e.getLocalizedMessage(), e);
                }
            }
        } while ((suborganizationsPage = getNextPage(suborganizationsPage, accountId)) != null);

        return new ArrayList<>(resourceAggregator.aggregateByType(reservedResources)
                                                 .values());
    }

    private OrganizationResources getOrganizationResources(String organizationId) throws NotFoundException, ServerException {
        try {
            return resourcesDistributorProvider.get().get(organizationId);
        } catch (NotFoundException ignored) {
            return new OrganizationResourcesImpl(organizationId, emptyList(), emptyList());
        }
    }

    private List<? extends Resource> getUsedFromHeap(List<? extends Resource> reservedResources,
                                                     List<? extends Resource> usedResources) {
        List<Resource> usedFromHeap = new ArrayList<>();
        Map<String, ? extends Resource> type2Reserved = reservedResources.stream()
                                                                         .collect(Collectors.toMap(Resource::getType, Function.identity()));
        for (Resource used : usedResources) {
            Resource reserved = type2Reserved.get(used.getType());
            if (reserved != null) {
                try {
                    usedFromHeap.add(resourceAggregator.deduct(used, reserved));
                } catch (NoEnoughResourcesException e) {
                    // usedResources is less than reserved
                }
            }
        }
        return usedFromHeap;
    }

    private Page<? extends Organization> getNextPage(Page<? extends Organization> organizationPage,
                                                     String organizationId) throws ServerException {
        if (!organizationPage.hasNextPage()) {
            return null;
        }

        final Page.PageRef nextPageRef = organizationPage.getNextPageRef();
        return organizationManager.getByParent(organizationId,
                                               nextPageRef.getPageSize(),
                                               nextPageRef.getItemsBefore());
    }
}
