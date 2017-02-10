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
package com.codenvy.organization.api;

import com.codenvy.organization.api.event.BeforeOrganizationRemovedEvent;
import com.codenvy.organization.api.event.OrganizationPersistedEvent;
import com.codenvy.organization.api.permissions.OrganizationDomain;
import com.codenvy.organization.shared.model.Organization;
import com.codenvy.organization.spi.MemberDao;
import com.codenvy.organization.spi.OrganizationDao;
import com.codenvy.organization.spi.impl.MemberImpl;
import com.codenvy.organization.spi.impl.OrganizationImpl;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.SubjectImpl;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.management.MemoryUsage;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Tests for {@link OrganizationManager}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class OrganizationManagerTest {
    @Captor
    private ArgumentCaptor<OrganizationImpl>               organizationCaptor;
    @Captor
    private ArgumentCaptor<OrganizationPersistedEvent>     persistEventCaptor;
    @Captor
    private ArgumentCaptor<BeforeOrganizationRemovedEvent> removeEventCaptor;

    @Mock
    private OrganizationDao organizationDao;

    @Mock
    private MemberDao memberDao;

    @Mock
    private EventService eventService;

    private OrganizationManager manager;

    @BeforeMethod
    public void setUp() throws Exception {
        manager = spy(new OrganizationManager(eventService,
                                              organizationDao,
                                              memberDao,
                                              new String[] {"reserved"}));

        when(eventService.publish(any())).thenAnswer(invocation -> invocation.getArguments()[0]);
    }

    @Test
    public void shouldCreateOrganization() throws Exception {
        EnvironmentContext.getCurrent().setSubject(new SubjectImpl("user-name", "user-id", "token", false));
        final Organization toCreate = createOrganization();

        manager.create(toCreate);

        verify(organizationDao).create(organizationCaptor.capture());
        final OrganizationImpl createdOrganization = organizationCaptor.getValue();
        assertEquals(createdOrganization.getName(), toCreate.getName());
        assertEquals(createdOrganization.getParent(), toCreate.getParent());
        verify(eventService).publish(persistEventCaptor.capture());
        assertEquals(persistEventCaptor.getValue().getOrganization(), createdOrganization);
        verify(memberDao).store(new MemberImpl("user-id", createdOrganization.getId(), OrganizationDomain.getActions()));
    }

    @Test
    public void shouldGenerateIdentifierWhenCreatingOrganization() throws Exception {
        final Organization organization = createOrganization();

        manager.create(organization);

        verify(organizationDao).create(organizationCaptor.capture());
        final String id = organizationCaptor.getValue().getId();
        assertNotNull(id);
        assertNotEquals(id, "identifier");
    }

    @Test(expectedExceptions = ConflictException.class)
    public void shouldThrowConflictExceptionOnCreationIfOrganizationNameIsReserved() throws Exception {
        final OrganizationImpl organization = new OrganizationImpl("identifier", "reserved", "parentId");

        manager.create(organization);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenCreatingNullableOrganization() throws Exception {
        manager.create(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenUpdatingOrganizationWithNullEntity() throws Exception {
        manager.update("organizationId", null);
    }

    @Test
    public void shouldUpdateOrganizationAndIgnoreIdAndParentFields() throws Exception {
        final OrganizationImpl existing = mock(OrganizationImpl.class);
        when(organizationDao.getById(any())).thenReturn(existing);
        final OrganizationImpl update = new OrganizationImpl(existing);
        update.setId("newId");
        update.setName("newName");
        update.setParent("newParent");

        final Organization updated = manager.update("organizationId", update);

        verify(organizationDao).getById("organizationId");
        verify(existing).setName("newName");
        verify(organizationDao).update(existing);
        assertEquals(updated, existing);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void shouldThrowConflictExceptionOnUpdatingIfOrganizationNameIsReserved() throws Exception {
        manager.update("id", new OrganizationImpl("id", "reserved", "parentId"));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenUpdatingOrganizationByNullId() throws Exception {
        manager.update(null, new OrganizationImpl());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenRemovingOrganizationByNullId() throws Exception {
        manager.remove(null);
    }

    @Test
    public void shouldRemoveOrganization() throws Exception {
        doNothing().when(manager).removeSuborganizations(anyString(), anyInt());
        doNothing().when(manager).removeMembers(anyString(), anyInt());
        OrganizationImpl toRemove = createOrganization();
        when(organizationDao.getById(anyString())).thenReturn(toRemove);

        manager.remove(toRemove.getId());

        verify(organizationDao).remove(toRemove.getId());
        verify(manager).removeMembers(eq(toRemove.getId()), anyInt());
        verify(manager).removeSuborganizations(eq(toRemove.getId()), anyInt());
        verify(eventService).publish(removeEventCaptor.capture());
        assertEquals(removeEventCaptor.getValue().getOrganization(), toRemove);
    }

    @Test
    public void shouldRemoveMembersByOrganizationId() throws Exception {
        MemberImpl member1 = new MemberImpl("user1", "org1", singletonList("read"));
        MemberImpl member2 = new MemberImpl("user2", "org1", singletonList("read"));
        doReturn(new Page<>(singletonList(member1), 0, 1, 2))
                .doReturn(new Page<>(singletonList(member2), 1, 1, 2))
                .when(memberDao).getMembers(anyString(), anyInt(), anyInt());

        manager.removeMembers("org1", 1);

        verify(memberDao, times(2)).getMembers("org1", 1, 0);
        verify(memberDao).remove("user1", "org1");
        verify(memberDao).remove("user2", "org1");
    }

    @Test
    public void shouldRemoveSuborganizationsByParentOrganizationId() throws Exception {
        doNothing().when(manager).remove(any());
        OrganizationImpl subOrg1 = new OrganizationImpl("subOrg1", "subOrg1", "org1");
        OrganizationImpl subOrg2 = new OrganizationImpl("subOrg2", "subOrg2", "org1");
        doReturn(new Page<>(singletonList(subOrg1), 0, 1, 2))
                .doReturn(new Page<>(singletonList(subOrg2), 1, 1, 2))
                .when(organizationDao).getByParent(anyString(), anyInt(), anyInt());

        manager.removeSuborganizations("org1", 1);

        verify(organizationDao, times(2)).getByParent("org1", 1, 0);
        verify(manager).remove("subOrg1");
        verify(manager).remove("subOrg2");
    }

    @Test
    public void shouldNotTryToRemoveOrganizationWhenItIsNotExistRemoveOrganization() throws Exception {
        when(organizationDao.getById(anyString())).thenThrow(new NotFoundException("not found"));

        manager.remove("id");

        verify(organizationDao, never()).remove(anyString());
        verify(eventService, never()).publish(any());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenGettingOrganizationByNullName() throws Exception {
        manager.getById(null);
    }

    @Test
    public void shouldGetOrganizationByName() throws Exception {
        final OrganizationImpl toFetch = createOrganization();
        when(organizationDao.getByName(eq("org123"))).thenReturn(toFetch);

        final Organization fetched = manager.getByName("org123");

        assertEquals(fetched, toFetch);
        verify(organizationDao).getByName("org123");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenGettingOrganizationByNullId() throws Exception {
        manager.getById(null);
    }

    @Test
    public void shouldGetOrganizationById() throws Exception {
        final OrganizationImpl toFetch = createOrganization();
        when(organizationDao.getById(eq("org123"))).thenReturn(toFetch);

        final Organization fetched = manager.getById("org123");

        assertEquals(fetched, toFetch);
        verify(organizationDao).getById("org123");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenGettingSuborganizationsByNullParent() throws Exception {
        manager.getByParent(null, 30, 0);
    }

    @Test
    public void shouldGetOrganizationsByParent() throws Exception {
        final OrganizationImpl toFetch = createOrganization();
        when(organizationDao.getByParent(eq("org123"), anyInt(), anyInt()))
                .thenReturn(new Page<>(singletonList(toFetch), 0, 1, 1));

        final Page<? extends Organization> organizations = manager.getByParent("org123", 30, 0);

        assertEquals(organizations.getItemsCount(), 1);
        assertEquals(organizations.getItems().get(0), toFetch);
        verify(organizationDao).getByParent("org123", 30, 0);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenGettingOrganizationsByNullUserId() throws Exception {
        manager.getByMember(null, 30, 0);
    }

    @Test
    public void shouldGetOrganizationsByMember() throws Exception {
        final OrganizationImpl toFetch = createOrganization();
        when(memberDao.getOrganizations(eq("org123"), anyInt(), anyInt()))
                .thenReturn(new Page<>(singletonList(toFetch), 0, 1, 1));

        final Page<? extends Organization> organizations = manager.getByMember("org123", 30, 0);

        assertEquals(organizations.getItemsCount(), 1);
        assertEquals(organizations.getItems().get(0), toFetch);
        verify(memberDao).getOrganizations("org123", 30, 0);
    }

    private OrganizationImpl createOrganization() {
        return new OrganizationImpl("identifier", "orgName", "parentId");
    }
}
