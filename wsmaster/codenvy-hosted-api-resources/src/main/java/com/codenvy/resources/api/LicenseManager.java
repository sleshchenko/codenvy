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

import com.codenvy.resources.api.provider.ResourcesProvider;
import com.codenvy.resources.spi.impl.AbstractResource;
import com.codenvy.resources.spi.impl.LicenseImpl;
import com.codenvy.resources.spi.impl.ProvidedResourceImpl;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Facade for License related operations.
 *
 * @author Sergii Leschenko
 */
public class LicenseManager {
    private final Set<ResourcesProvider> resourcesProviders;
    private final ResourcesAggregator    resourcesAggregator;

    @Inject
    public LicenseManager(Set<ResourcesProvider> resourcesProviders,
                          ResourcesAggregator resourcesAggregator) {
        this.resourcesProviders = resourcesProviders;
        this.resourcesAggregator = resourcesAggregator;
    }

    /**
     * Returns license which given account can use
     *
     * @param accountId
     *         account id
     * @return license which can be used by given account
     * @throws NotFoundException
     *         when account with specified id was not found
     * @throws ServerException
     *         when some exception occurs
     */
    public LicenseImpl getByAccount(String accountId) throws NotFoundException, ServerException {
        final List<ProvidedResourceImpl> resources = new ArrayList<>();
        for (ResourcesProvider resourcesProvider : resourcesProviders) {
            resources.addAll(resourcesProvider.getResources(accountId));
        }

        final List<AbstractResource> allResources = resources.stream()
                                                             .flatMap(de -> de.getResources().stream())
                                                             .collect(Collectors.toList());

        return new LicenseImpl(accountId,
                               resources,
                               new ArrayList<>(resourcesAggregator.aggregateByType(allResources).values()));
    }
}
