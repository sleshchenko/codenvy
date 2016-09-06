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
import com.codenvy.organization.spi.impl.OrganizationImpl;

import org.eclipse.che.api.core.notification.EventService;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.PreRemove;

/**
 * Callback for {@link OrganizationImpl organization} jpa related events.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class OrganizationEntityListener {

    @Inject
    private EventService eventService;

    @PreRemove
    public void preRemove(OrganizationImpl organization) {
        eventService.publish(new BeforeOrganizationRemovedEvent(organization));
    }
}
