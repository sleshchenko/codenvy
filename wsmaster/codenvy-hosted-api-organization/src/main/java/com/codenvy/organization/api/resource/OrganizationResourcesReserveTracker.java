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
package com.codenvy.organization.api.resource;

import com.codenvy.organization.shared.model.OrganizationResourcesLimit;
import com.codenvy.resource.api.ResourceAggregator;
import com.codenvy.resource.api.ResourcesReserveTracker;
import com.codenvy.resource.model.Resource;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.codenvy.organization.spi.impl.OrganizationImpl.ORGANIZATIONAL_ACCOUNT;

/**
 * Make organization's resources unavailable for usage when organization share them for its suborganizations.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class OrganizationResourcesReserveTracker implements ResourcesReserveTracker {
    static final int LIMITS_PER_PAGE = 100;

    private final OrganizationResourcesManager organizationResourcesManager;
    private final ResourceAggregator           resourceAggregator;

    @Inject
    public OrganizationResourcesReserveTracker(OrganizationResourcesManager organizationResourcesManager,
                                               ResourceAggregator resourceAggregator) {
        this.organizationResourcesManager = organizationResourcesManager;
        this.resourceAggregator = resourceAggregator;
    }

    @Override
    public List<? extends Resource> getReservedResources(String accountId) throws ServerException {
        try {
            Page<? extends OrganizationResourcesLimit> limitsPage = organizationResourcesManager.getResourcesLimits(accountId,
                                                                                                                    LIMITS_PER_PAGE,
                                                                                                                    0);
            List<Resource> resources = new ArrayList<>();
            do {
                limitsPage.getItems()
                          .stream()
                          .flatMap(orgLimit -> orgLimit.getResources()
                                                       .stream())
                          .collect(Collectors.toCollection(() -> resources));
            } while ((limitsPage = getNextPage(limitsPage, accountId)) != null);

            return new ArrayList<>(resourceAggregator.aggregateByType(resources)
                                                     .values());
        } catch (NotFoundException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public String getAccountType() {
        return ORGANIZATIONAL_ACCOUNT;
    }

    private Page<? extends OrganizationResourcesLimit> getNextPage(Page<? extends OrganizationResourcesLimit> limitsPage,
                                                                   String organizationId) throws ServerException {
        if (!limitsPage.hasNextPage()) {
            return null;
        }

        final Page.PageRef nextPageRef = limitsPage.getNextPageRef();
        return organizationResourcesManager.getResourcesLimits(organizationId, nextPageRef.getPageSize(), nextPageRef.getItemsBefore());
    }
}
