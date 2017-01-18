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
package com.codenvy.organization.api.listener;

import com.codenvy.organization.api.OrganizationManager;
import com.codenvy.organization.api.event.BeforeOrganizationRemovedEvent;
import com.codenvy.organization.shared.model.Organization;

import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.core.db.cascade.CascadeEventSubscriber;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * TODO Add docs
 *
 * @author Sergii Leschenko
 */
@Singleton
public class RemoveSuborganizationsSubscriber
        extends CascadeEventSubscriber<BeforeOrganizationRemovedEvent> {
    private static final int PAGE_SIZE = 100;

    @Inject
    private EventService eventService;

    @Inject
    private OrganizationManager organizationManager;

    @PostConstruct
    public void subscribe() {
        eventService.subscribe(this, BeforeOrganizationRemovedEvent.class);
    }

    @PreDestroy
    public void unsubscribe() {
        eventService.unsubscribe(this, BeforeOrganizationRemovedEvent.class);
    }

    @Override
    public void onCascadeEvent(BeforeOrganizationRemovedEvent event) throws Exception {
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
    void removeSuborganizations(String organizationId, int pageSize) throws ServerException {
        Page<? extends Organization> suborganizationsPage;
        do {
            // skip count always equals to 0 because elements will be shifted after removing previous items
            suborganizationsPage = organizationManager.getByParent(organizationId, pageSize, 0);
            for (Organization suborganization : suborganizationsPage.getItems()) {
                organizationManager.remove(suborganization.getId());
            }
        } while (suborganizationsPage.hasNextPage());
    }
}
