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

import com.codenvy.organization.api.event.BeforeOrganizationRemovedEvent;
import com.codenvy.organization.api.event.PostOrganizationPersistedEvent;
import com.codenvy.organization.spi.OrganizationDao;
import com.codenvy.organization.spi.impl.OrganizationImpl;
import com.google.inject.persist.Transactional;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.core.db.cascade.CascadeEventService;
import org.eclipse.che.core.db.cascade.CascadeEventSubscriber;
import org.eclipse.che.core.db.jpa.DuplicateKeyException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * JPA based implementation of {@link OrganizationDao}.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class JpaOrganizationDao implements OrganizationDao {

    private final Provider<EntityManager> managerProvider;
    private final CascadeEventService     eventService;

    @Inject
    public JpaOrganizationDao(Provider<EntityManager> managerProvider, CascadeEventService eventService) {
        this.managerProvider = managerProvider;
        this.eventService = eventService;
    }

    @Override
    public void create(OrganizationImpl organization) throws ServerException, ConflictException {
        requireNonNull(organization, "Required non-null organization");
        try {
            doCreate(organization);
        } catch (DuplicateKeyException e) {
            throw new ConflictException("Organization with such id or name already exists");
        } catch (RuntimeException x) {
            throw new ServerException(x.getLocalizedMessage(), x);
        }
    }

    @Override
    public void update(OrganizationImpl update) throws NotFoundException, ConflictException, ServerException {
        requireNonNull(update, "Required non-null organization");
        try {
            doUpdate(update);
        } catch (DuplicateKeyException e) {
            throw new ConflictException("Organization with such name already exists");
        } catch (RuntimeException x) {
            throw new ServerException(x.getLocalizedMessage(), x);
        }
    }

    @Override
    public void remove(String organizationId) throws ConflictException, ServerException {
        requireNonNull(organizationId, "Required non-null organization id");
        try {
            doRemove(organizationId);
        } catch (RuntimeException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    @Transactional
    public OrganizationImpl getById(String organizationId) throws NotFoundException, ServerException {
        requireNonNull(organizationId, "Required non-null organization id");
        try {
            final EntityManager manager = managerProvider.get();
            OrganizationImpl organization = manager.find(OrganizationImpl.class, organizationId);
            if (organization == null) {
                throw new NotFoundException(format("Organization with id '%s' doesn't exist", organizationId));
            }
            return organization;
        } catch (RuntimeException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    @Transactional
    public OrganizationImpl getByName(String organizationName) throws NotFoundException, ServerException {
        requireNonNull(organizationName, "Required non-null organization name");
        try {
            final EntityManager manager = managerProvider.get();
            return manager.createNamedQuery("Organization.getByName", OrganizationImpl.class)
                          .setParameter("name", organizationName)
                          .getSingleResult();
        } catch (NoResultException e) {
            throw new NotFoundException(format("Organization with name '%s' doesn't exist", organizationName));
        } catch (RuntimeException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    @Transactional
    public Page<OrganizationImpl> getByParent(String parent, int maxItems, long skipCount) throws ServerException {
        requireNonNull(parent, "Required non-null parent");
        checkArgument(skipCount <= Integer.MAX_VALUE, "The number of items to skip can't be greater than " + Integer.MAX_VALUE);
        try {
            final EntityManager manager = managerProvider.get();
            final List<OrganizationImpl> result = manager.createNamedQuery("Organization.getByParent", OrganizationImpl.class)
                                                         .setParameter("parent", parent)
                                                         .setMaxResults(maxItems)
                                                         .setFirstResult((int)skipCount)
                                                         .getResultList();
            final Long suborganizationsCount = manager.createNamedQuery("Organization.getSuborganizationsCount", Long.class)
                                                      .setParameter("parent", parent)
                                                      .getSingleResult();

            return new Page<>(result, skipCount, maxItems, suborganizationsCount);
        } catch (RuntimeException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    @Transactional(rollbackOn = {RuntimeException.class, ApiException.class})
    protected void doCreate(OrganizationImpl organization) throws ConflictException, ServerException {
        EntityManager manager = managerProvider.get();
        manager.persist(organization);
        manager.flush();
        eventService.publish(new PostOrganizationPersistedEvent(organization));
    }

    @Transactional
    protected void doUpdate(OrganizationImpl update) throws NotFoundException {
        final EntityManager manager = managerProvider.get();
        if (manager.find(OrganizationImpl.class, update.getId()) == null) {
            throw new NotFoundException(format("Couldn't update organization with id '%s' because it doesn't exist", update.getId()));
        }
        manager.merge(update);
        manager.flush();
    }

    @Transactional(rollbackOn = {RuntimeException.class, ApiException.class})
    protected void doRemove(String organizationId) throws ConflictException, ServerException {
        final EntityManager manager = managerProvider.get();
        final OrganizationImpl organization = manager.find(OrganizationImpl.class, organizationId);
        if (organization != null) {
            eventService.publish(new BeforeOrganizationRemovedEvent(new OrganizationImpl(organization)));
            manager.remove(organization);
            manager.flush();
        }
    }

    @Singleton
    public static class RemoveSuborganizationsBeforeParentOrganizationRemovedEventSubscriber
            extends CascadeEventSubscriber<BeforeOrganizationRemovedEvent> {
        private static final int PAGE_SIZE = 100;

        @Inject
        private CascadeEventService eventService;

        @Inject
        private OrganizationDao organizationDao;

        @PostConstruct
        public void subscribe() {
            eventService.subscribe(this, BeforeOrganizationRemovedEvent.class);
        }

        @PreDestroy
        public void unsubscribe() {
            eventService.unsubscribe(this, BeforeOrganizationRemovedEvent.class);
        }

        @Override
        public void onCascadeEvent(BeforeOrganizationRemovedEvent event) throws ApiException {
            removeSuborganizations(event.getOrganization().getId(), PAGE_SIZE);
        }

        /**
         * Removes suborganizations of given parent organization page by page
         *
         * @param organizationId
         *         parent organization id
         * @param pageSize
         *         number of items which should removed by one request
         */
        void removeSuborganizations(String organizationId, int pageSize) throws ConflictException, ServerException {
            Page<OrganizationImpl> suborganizationsPage;
            do {
                // skip count always equals to 0 because elements will be shifted after removing previous items
                suborganizationsPage = organizationDao.getByParent(organizationId, pageSize, 0);
                for (OrganizationImpl suborganization : suborganizationsPage.getItems()) {
                    organizationDao.remove(suborganization.getId());
                }
            } while (suborganizationsPage.hasNextPage());
        }
    }
}
