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
package com.codenvy.organization.api;

import com.codenvy.organization.api.permissions.OrganizationDomain;
import com.codenvy.organization.shared.model.Organization;
import com.codenvy.organization.spi.MemberDao;
import com.codenvy.organization.spi.impl.MemberImpl;
import com.codenvy.organization.spi.impl.OrganizationImpl;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.user.server.event.BeforeUserRemovedEvent;
import org.eclipse.che.api.user.server.event.PostUserPersistedEvent;
import org.eclipse.che.api.user.server.event.PostUserUpdatedEvent;
import org.eclipse.che.core.db.cascade.CascadeEventSubscriber;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * //TODO Revise this class
 *
 * @author Sergii Leschenko
 */
@Singleton
public class PersonalOrganizationProvider {
    private final OrganizationManager             organizationManager;
    private final EventService                    eventService;
    private final MemberDao                       memberDao;
    private final PersonalOrganizationCreator     organizationCreator;
    private final PersonalOrganizationNameUpdater organizationNameUpdater;
    private final PersonalOrganizationRemover     organizationRemover;

    @Inject
    public PersonalOrganizationProvider(OrganizationManager organizationManager,
                                        EventService eventService,
                                        MemberDao memberDao) {
        this.organizationManager = organizationManager;
        this.eventService = eventService;
        this.memberDao = memberDao;
        this.organizationCreator = new PersonalOrganizationCreator();
        this.organizationNameUpdater = new PersonalOrganizationNameUpdater();
        this.organizationRemover = new PersonalOrganizationRemover();
    }

    @PostConstruct
    public void subscribe() {
        eventService.subscribe(organizationCreator, PostUserPersistedEvent.class);
        eventService.subscribe(organizationNameUpdater, PostUserUpdatedEvent.class);
        eventService.subscribe(organizationRemover, BeforeUserRemovedEvent.class);
    }

    public class PersonalOrganizationCreator extends CascadeEventSubscriber<PostUserPersistedEvent> {
        @Override
        public void onCascadeEvent(PostUserPersistedEvent event) throws ApiException {
            Organization organization = organizationManager.create(new OrganizationImpl(null,
                                                                                        event.getUser().getName(),
                                                                                        null));
            memberDao.store(new MemberImpl(event.getUser().getId(),
                                           organization.getId(),
                                           OrganizationDomain.getActions()));
        }
    }

    public class PersonalOrganizationNameUpdater extends CascadeEventSubscriber<PostUserUpdatedEvent> {
        @Override
        public void onCascadeEvent(PostUserUpdatedEvent event) throws ApiException {
            String oldUsername = event.getOriginal().getName();
            String newUserName = event.getUpdated().getName();
            if (!oldUsername.equals(newUserName)) {
                OrganizationImpl personalOrganization = new OrganizationImpl(organizationManager.getByName(oldUsername));
                personalOrganization.setName(event.getUpdated().getName());
                organizationManager.update(personalOrganization.getId(), personalOrganization);
            }
        }
    }

    public class PersonalOrganizationRemover extends CascadeEventSubscriber<BeforeUserRemovedEvent> {
        @Override
        public void onCascadeEvent(BeforeUserRemovedEvent event) throws ApiException {
            try {
                Organization toRemove = organizationManager.getByName(event.getUser().getName());
                organizationManager.remove(toRemove.getId());
            } catch (NotFoundException ignored) {
            }
        }
    }
}
