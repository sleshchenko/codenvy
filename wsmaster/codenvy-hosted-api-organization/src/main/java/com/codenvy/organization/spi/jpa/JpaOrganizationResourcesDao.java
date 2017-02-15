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
package com.codenvy.organization.spi.jpa;

import com.codenvy.organization.spi.OrganizationResourcesDao;
import com.codenvy.organization.spi.impl.OrganizationResourcesImpl;
import com.google.inject.persist.Transactional;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * JPA based implementation of {@link OrganizationResourcesDao}.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class JpaOrganizationResourcesDao implements OrganizationResourcesDao {
    @Inject
    private Provider<EntityManager> managerProvider;

    @Override
    public void store(OrganizationResourcesImpl distributedResources) throws ServerException {
        requireNonNull(distributedResources, "Required non-null distributed resources");
        try {
            doStore(distributedResources);
        } catch (RuntimeException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackOn = ApiException.class)
    public OrganizationResourcesImpl get(String organizationId) throws NotFoundException, ServerException {
        requireNonNull(organizationId, "Required non-null organization id");
        try {
            OrganizationResourcesImpl distributedResources = managerProvider.get()
                                                                            .find(OrganizationResourcesImpl.class,
                                                                                  organizationId);
            if (distributedResources == null) {
                throw new NotFoundException("There are no distributed resources for organization with id '" + organizationId + "'.");
            }

            return new OrganizationResourcesImpl(distributedResources);
        } catch (RuntimeException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackOn = ApiException.class)
    public Page<OrganizationResourcesImpl> getByParent(String organizationId, int maxItems, long skipCount) throws ServerException {
        requireNonNull(organizationId, "Required non-null organization id");
        checkArgument(skipCount <= Integer.MAX_VALUE, "The number of items to skip can't be greater than " + Integer.MAX_VALUE);
        try {
            final EntityManager manager = managerProvider.get();
            final List<OrganizationResourcesImpl> distributedResources =
                    manager.createNamedQuery("OrganizationResources.getByParent",
                                             OrganizationResourcesImpl.class)
                           .setParameter("parent", organizationId)
                           .setMaxResults(maxItems)
                           .setFirstResult((int)skipCount)
                           .getResultList();
            final Long distributedResourcesCount = manager.createNamedQuery("OrganizationResources.getCountByParent", Long.class)
                                                          .setParameter("parent", organizationId)
                                                          .getSingleResult();
            return new Page<>(distributedResources.stream()
                                                  .map(OrganizationResourcesImpl::new)
                                                  .collect(Collectors.toList()),
                              skipCount,
                              maxItems,
                              distributedResourcesCount);
        } catch (RuntimeException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    public void remove(String organizationId) throws ServerException {
        requireNonNull(organizationId, "Required non-null organization id");
        try {
            doRemove(organizationId);
        } catch (RuntimeException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Transactional
    protected void doRemove(String id) {
        EntityManager manager = managerProvider.get();
        OrganizationResourcesImpl distributedResources = manager.find(OrganizationResourcesImpl.class, id);
        if (distributedResources != null) {
            manager.remove(distributedResources);
            manager.flush();
        }
    }

    @Transactional
    protected void doStore(OrganizationResourcesImpl organizationResources) throws ServerException {
        EntityManager manager = managerProvider.get();
        final OrganizationResourcesImpl existingDistributedResources = manager.find(OrganizationResourcesImpl.class,
                                                                                    organizationResources.getOrganizationId());
        if (existingDistributedResources == null) {
            manager.persist(organizationResources);
        } else {
            existingDistributedResources.getResourcesCap().clear();
            existingDistributedResources.getResourcesCap().addAll(organizationResources.getResourcesCap());
            existingDistributedResources.getReservedResources().clear();
            existingDistributedResources.getReservedResources().addAll(organizationResources.getReservedResources());
        }
        manager.flush();
    }

}
