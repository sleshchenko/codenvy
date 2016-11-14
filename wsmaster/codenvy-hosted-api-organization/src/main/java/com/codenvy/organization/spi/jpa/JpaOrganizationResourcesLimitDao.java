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
package com.codenvy.organization.spi.jpa;

import com.codenvy.organization.api.event.BeforeOrganizationRemovedEvent;
import com.codenvy.organization.spi.OrganizationResourcesLimitDao;
import com.codenvy.organization.spi.impl.OrganizationResourcesLimitImpl;
import com.google.inject.persist.Transactional;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * JPA based implementation of {@link OrganizationResourcesLimitDao}.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class JpaOrganizationResourcesLimitDao implements OrganizationResourcesLimitDao {
    private static final Logger LOG = LoggerFactory.getLogger(JpaOrganizationResourcesLimitDao.class);

    @Inject
    private Provider<EntityManager> managerProvider;

    @Override
    public void store(OrganizationResourcesLimitImpl resourcesLimit) throws ServerException {
        requireNonNull(resourcesLimit, "Required non-null resources limit");
        try {
            doStore(resourcesLimit);
        } catch (RuntimeException x) {
            throw new ServerException(x.getMessage(), x);
        }
    }

    @Override
    @Transactional
    public OrganizationResourcesLimitImpl get(String organizationId) throws NotFoundException, ServerException {
        requireNonNull(organizationId, "Required non-null organization id");
        try {
            final OrganizationResourcesLimitImpl resourcesLimit = managerProvider.get()
                                                                                 .find(OrganizationResourcesLimitImpl.class,
                                                                                       organizationId);
            if (resourcesLimit == null) {
                throw new NotFoundException("Resources limit for organization '" + organizationId + "' was not found");
            }

            return resourcesLimit;
        } catch (RuntimeException e) {
            throw new ServerException(e.getMessage(), e);

        }
    }

    @Override
    @Transactional
    public Page<OrganizationResourcesLimitImpl> getByParent(String organizationId, int maxItems, long skipCount) throws ServerException {
        requireNonNull(organizationId, "Required non-null organization id");
        checkArgument(skipCount <= Integer.MAX_VALUE, "The number of items to skip can't be greater than " + Integer.MAX_VALUE);
        try {
            final EntityManager manager = managerProvider.get();
            final List<OrganizationResourcesLimitImpl> limits = manager.createNamedQuery("OrganizationResourcesLimit.getByParent",
                                                                                         OrganizationResourcesLimitImpl.class)
                                                                       .setParameter("parent", organizationId)
                                                                       .setMaxResults(maxItems)
                                                                       .setFirstResult((int)skipCount)
                                                                       .getResultList();
            final Long limitsCount = manager.createNamedQuery("OrganizationResourcesLimit.getCountByParent", Long.class)
                                            .setParameter("parent", organizationId)
                                            .getSingleResult();
            return new Page<>(limits, skipCount, maxItems, limitsCount);
        } catch (RuntimeException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    public void remove(String organizationId) throws ServerException {
        requireNonNull(organizationId, "Required non-null resources limit");
        try {
            doRemove(organizationId);
        } catch (RuntimeException x) {
            throw new ServerException(x.getMessage(), x);
        }
    }

    @Transactional
    protected void doRemove(String id) {
        final OrganizationResourcesLimitImpl organizationLimit = managerProvider.get().find(OrganizationResourcesLimitImpl.class, id);
        if (organizationLimit != null) {
            managerProvider.get().remove(organizationLimit);
        }
    }

    @Transactional
    void doStore(OrganizationResourcesLimitImpl resourcesLimit) throws ServerException {
        EntityManager manager = managerProvider.get();
        try {
            final OrganizationResourcesLimitImpl existed = get(resourcesLimit.getOrganizationId());
            existed.getResources().clear();
            existed.getResources().addAll(resourcesLimit.getResources());
        } catch (NotFoundException n) {
            manager.persist(resourcesLimit);
        }
    }

    @Singleton
    public static class RemoveOrganizationResourcesLimitBeforeOrganizationRemovedEventSubscriber
            implements EventSubscriber<BeforeOrganizationRemovedEvent> {
        @Inject
        private EventService                  eventService;
        @Inject
        private OrganizationResourcesLimitDao organizationResourcesLimitDao;

        @PostConstruct
        public void subscribe() {
            eventService.subscribe(this);
        }

        @PreDestroy
        public void unsubscribe() {
            eventService.unsubscribe(this);
        }

        @Override
        public void onEvent(BeforeOrganizationRemovedEvent event) {
            try {
                organizationResourcesLimitDao.remove(event.getOrganization().getId());
            } catch (Exception x) {
                LOG.error(format("Couldn't remove organization resources limit before organization '%s' is removed",
                                 event.getOrganization().getId()),
                          x);
            }
        }
    }
}
