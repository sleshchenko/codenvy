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

import com.codenvy.organization.spi.MemberDao;
import com.codenvy.organization.spi.impl.MemberImpl;

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.user.server.event.BeforeUserRemovedEvent;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static com.codenvy.organization.spi.jpa.JpaMemberDao.RemoveMembersBeforeUserRemovedEventSubscriber;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RemoveMembersBeforeUserRemovedEventSubscriber}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class RemoveMembersBeforeUserRemovedEventSubscriberTest {
    @Mock
    private EventService eventService;
    @Mock
    private MemberDao    memberDao;

    @InjectMocks
    RemoveMembersBeforeUserRemovedEventSubscriber subscriber;

    @Test
    public void shouldSubscribeItself() {
        subscriber.subscribe();

        verify(eventService).subscribe(eq(subscriber));
    }

    @Test
    public void shouldUnsubscribeItself() {
        subscriber.unsubscribe();

        verify(eventService).unsubscribe(eq(subscriber));
    }

    @Test
    public void shouldRemoveMembersOnBeforeUserRemovedEvent() throws Exception {
        final UserImpl user = new UserImpl("user123", "user@test.com", "userok");

        final MemberImpl member1 = new MemberImpl("user123", "org123", Collections.emptyList());
        final MemberImpl member2 = new MemberImpl("user123", "org321", Collections.emptyList());
        when(memberDao.getMemberships(eq("user123"))).thenReturn(Arrays.asList(member1, member2));

        subscriber.onEvent(new BeforeUserRemovedEvent(user));

        verify(memberDao).getMemberships(eq("user123"));
        verify(memberDao).remove(eq("org123"), eq("user123"));
        verify(memberDao).remove(eq("org321"), eq("user123"));
    }
}
