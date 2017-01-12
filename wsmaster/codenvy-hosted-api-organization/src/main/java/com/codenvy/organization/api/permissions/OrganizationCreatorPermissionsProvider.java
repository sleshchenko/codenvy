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
package com.codenvy.organization.api.permissions;

import com.codenvy.organization.api.event.PostOrganizationPersistedEvent;
import com.codenvy.organization.spi.MemberDao;
import com.codenvy.organization.spi.impl.MemberImpl;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Adds permissions for creator after organization creation
 *
 * @author Sergii Leschenko
 */
@Singleton
public class OrganizationCreatorPermissionsProvider implements EventSubscriber<PostOrganizationPersistedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationCreatorPermissionsProvider.class);

    private final MemberDao    memberDao;
    private final EventService eventService;
    private final UserManager  userManager;

    @Inject
    public OrganizationCreatorPermissionsProvider(EventService eventService,
                                                  MemberDao memberDao,
                                                  UserManager userManager) {
        this.memberDao = memberDao;
        this.eventService = eventService;
        this.userManager = userManager;
    }

    @PostConstruct
    void subscribe() {
        eventService.subscribe(this);
    }

    @PreDestroy
    void unsubscribe() {
        eventService.unsubscribe(this);
    }

    @Override
    public void onEvent(PostOrganizationPersistedEvent event) {
        if (EnvironmentContext.getCurrent().getSubject() != null) {
            try {
                userManager.getByName(event.getOrganization().getName());
                return;
            } catch (NotFoundException e) {
                try {
                    memberDao.store(new MemberImpl(EnvironmentContext.getCurrent().getSubject().getUserId(),
                                                   event.getOrganization().getId(),
                                                   OrganizationDomain.getActions()));
                } catch (ServerException e1) {
                    LOG.error("Can't add creator's permissions for organization with id '" + event.getOrganization().getId() + "'", e);
                }
            } catch (ServerException e) {
                LOG.error("Can't add creator's permissions for organization with id '" + event.getOrganization().getId() + "'", e);
            }
        }
    }
}
