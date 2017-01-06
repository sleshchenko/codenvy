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
import org.eclipse.che.api.core.model.user.User;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.core.db.cascade.CascadeEventSubscriber;

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
public class OrganizationPermissionsProvider extends CascadeEventSubscriber<PostOrganizationPersistedEvent> {
    private final MemberDao    memberDao;
    private final EventService eventService;
    private final UserManager  userManager;

    @Inject
    public OrganizationPermissionsProvider(EventService eventService,
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
    public void onCascadeEvent(PostOrganizationPersistedEvent event) throws ServerException {
        try {
            User user = userManager.getByName(event.getOrganization().getName());
            //personal organization creation
            grantPermissions(user.getId(), event.getOrganization().getId());
        } catch (NotFoundException e) {
            //normal organization creation
            grantPermissions(EnvironmentContext.getCurrent().getSubject().getUserId(),
                             event.getOrganization().getId());
        }
    }

    private void grantPermissions(String userId, String organizationId) throws ServerException {
        memberDao.store(new MemberImpl(userId,
                                       organizationId,
                                       OrganizationDomain.getActions()));
    }
}
