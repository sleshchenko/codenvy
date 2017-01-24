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

import com.codenvy.organization.api.event.BeforeOrganizationRemovedEvent;
import com.codenvy.organization.api.resource.OrganizationResourcesDistributor;
import com.codenvy.organization.spi.impl.OrganizationImpl;

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.core.db.cascade.CascadeEventSubscriber;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Listens for {@link OrganizationImpl} removal events and removes distributed
 * resources that belong organization that is going to be removed.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class RemoveOrganizationDistributedResourcesSubscriber extends CascadeEventSubscriber<BeforeOrganizationRemovedEvent> {
    @Inject
    private EventService                     eventService;
    @Inject
    private OrganizationResourcesDistributor resourcesDistributor;

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
        resourcesDistributor.reset(event.getOrganization().getId());
    }
}
