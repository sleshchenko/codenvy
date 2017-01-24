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

import org.eclipse.che.api.core.notification.EventService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RemoveOrganizationDistributedResourcesSubscriber}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class RemoveOrganizationDistributedResourcesSubscriberTest {
    @Mock
    private EventService                     eventService;
    @Mock
    private OrganizationResourcesDistributor resourcesDistributor;

    @InjectMocks
    private RemoveOrganizationDistributedResourcesSubscriber suborganizationsRemover;

    @Test
    public void shouldSubscribe() {
        suborganizationsRemover.subscribe();

        verify(eventService).subscribe(suborganizationsRemover, BeforeOrganizationRemovedEvent.class);
    }

    @Test
    public void shouldUnsubscribe() {
        suborganizationsRemover.unsubscribe();

        verify(eventService).unsubscribe(suborganizationsRemover, BeforeOrganizationRemovedEvent.class);
    }
}
