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
package com.codenvy.organization.api.resource;

import com.codenvy.organization.api.OrganizationManager;
import com.codenvy.organization.spi.OrganizationDistributedResourcesDao;
import com.codenvy.organization.spi.impl.OrganizationDistributedResourcesImpl;
import com.codenvy.organization.spi.impl.OrganizationImpl;
import com.codenvy.resource.api.ResourceAggregator;
import com.codenvy.resource.api.exception.NoEnoughResourcesException;
import com.codenvy.resource.api.usage.ResourceUsageManager;
import com.codenvy.resource.api.usage.ResourcesLocks;
import com.codenvy.resource.model.Resource;
import com.codenvy.resource.spi.impl.ResourceImpl;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.commons.lang.concurrent.Unlocker;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link OrganizationResourcesDistributor}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class OrganizationResourcesDistributorTest {
    private static final String PARENT_ORG_ID = "parentOrg123";
    private static final String ORG_ID        = "organization123";

    @Mock
    private Unlocker                            lock;
    @Mock
    private OrganizationDistributedResourcesDao distributedResourcesDao;
    @Mock
    private ResourcesLocks                      resourcesLocks;
    @Mock
    private ResourceUsageManager                usageManager;
    @Mock
    private ResourceAggregator                  resourceAggregator;
    @Mock
    private OrganizationManager                 organizationManager;

    @Spy
    @InjectMocks
    private OrganizationResourcesDistributor manager;

    @BeforeMethod
    public void setUp() throws Exception {
        doNothing().when(manager).checkResourcesAvailability(anyString(), any(), any());
        when(resourcesLocks.lock(anyString())).thenReturn(lock);

        when(organizationManager.getById(ORG_ID)).thenReturn(new OrganizationImpl(ORG_ID, ORG_ID + "name", PARENT_ORG_ID));
        when(organizationManager.getById(PARENT_ORG_ID)).thenReturn(new OrganizationImpl(PARENT_ORG_ID, PARENT_ORG_ID + "name", null));
    }

    @Test
    public void shouldDistributeResources() throws Exception {
        doThrow(new NotFoundException("no distributed resources"))
                .when(distributedResourcesDao).get(anyString());
        List<ResourceImpl> toDistribute = singletonList(createTestResource(1000));

        //when
        //TODO FIx
        manager.cap(ORG_ID, toDistribute);

        //then
        verify(distributedResourcesDao).get(ORG_ID);
        verify(manager).checkResourcesAvailability(ORG_ID,

                                                   emptyList(),
                                                   toDistribute);
        verify(distributedResourcesDao).store(new OrganizationDistributedResourcesImpl(ORG_ID,
                                                                                       toDistribute));
        verify(resourcesLocks).lock(ORG_ID);
        verify(lock).close();
    }

    @Test
    public void shouldDistributeResourcesWhenThereIsOldOne() throws Exception {
        //given
        final OrganizationDistributedResourcesImpl distributedResources = createDistributedResources(500);
        when(distributedResourcesDao.get(anyString())).thenReturn(distributedResources);
        List<ResourceImpl> toDistribute = singletonList(createTestResource(1000));

        //when
        manager.cap(ORG_ID, toDistribute);

        //then
        verify(distributedResourcesDao).get(ORG_ID);
        verify(manager).checkResourcesAvailability(ORG_ID,

                                                   distributedResources.getResourcesCap(),
                                                   toDistribute);
        verify(distributedResourcesDao).store(new OrganizationDistributedResourcesImpl(ORG_ID,
                                                                                       toDistribute));
        verify(resourcesLocks).lock(ORG_ID);
        verify(lock).close();
    }

    @Test(expectedExceptions = ConflictException.class,
          expectedExceptionsMessageRegExp = "It is not allowed to capResources resources for root organization.")
    public void shouldThrowConflictExceptionOnDistributingResourcesForRootOrganization() throws Exception {
        //given
        final OrganizationDistributedResourcesImpl distributedResources = createDistributedResources(500);
        when(distributedResourcesDao.get(anyString())).thenReturn(distributedResources);
        List<ResourceImpl> toDistribute = singletonList(createTestResource(1000));

        //when
        //TODO Fix
//        manager.capResources(PARENT_ORG_ID, toDistribute);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeOnDistributionResourcesWithNullOrganizationId() throws Exception {
        //when
        manager.cap(null, emptyList());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeOnDistributionNullResourcesList() throws Exception {
        //when
        manager.cap(null, emptyList());
    }

    @Test
    public void shouldGetDistributedResources() throws Exception {
        //given
        final OrganizationDistributedResourcesImpl distributedResources = createDistributedResources(1000);
        when(distributedResourcesDao.get(anyString())).thenReturn(distributedResources);

        //when
        final List<? extends Resource> fetchedDistributedResources = manager.getResourcesCaps(ORG_ID);

        //then
        assertEquals(fetchedDistributedResources, distributedResources.getResourcesCap());
        verify(distributedResourcesDao).get(ORG_ID);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeOnGettingDistributedResourcesByNullOrganizationId() throws Exception {
        //when
        manager.getResourcesCaps(null);
    }

    @Test
    public void shouldCheckTestResourceDifferenceAvailabilityOnSuborganizationLevelOnAmountDecreasing() throws Exception {
        //given
        doCallRealMethod().when(manager).checkResourcesAvailability(anyString(), any(), any());
        ResourceImpl used = createTestResource(500);
        doReturn(singletonList(used))
                .when(usageManager).getUsedResources(any());
        ResourceImpl caped = createTestResource(1000);
        ResourceImpl toCap = createTestResource(700);
        doReturn(createTestResource(300))
                .doReturn(createTestResource(500))
                .when(resourceAggregator).deduct((Resource)any(), any());

        //when
        manager.checkResourcesAvailability(ORG_ID,
                                           singletonList(caped),
                                           singletonList(toCap));

        //then
        verify(usageManager).getUsedResources(ORG_ID);
        verify(resourceAggregator).deduct(caped, toCap);
        verify(resourceAggregator).deduct(toCap, used);
    }

    @Test
    //TODO Fix name
    public void shouldNotCheckTestResourceDifferenceAvailabilityOnSuborganizationLevelOnAmountDecreasing() throws Exception {
        //given
        doCallRealMethod().when(manager).checkResourcesAvailability(anyString(), any(), any());
        doReturn(emptyList())
                .when(usageManager).getUsedResources(any());
        ResourceImpl caped = createTestResource(1000);
        ResourceImpl toCap = createTestResource(700);
        doReturn(createTestResource(300))
                .doReturn(createTestResource(500))
                .when(resourceAggregator).deduct((Resource)any(), any());

        //when
        manager.checkResourcesAvailability(ORG_ID,
                                           singletonList(caped),
                                           singletonList(toCap));

        //then
        verify(usageManager).getUsedResources(ORG_ID);
        verify(resourceAggregator).deduct(caped, toCap);
        verifyNoMoreInteractions(resourceAggregator);
    }

    @Test(expectedExceptionsMessageRegExp = "Resources are currently in use. You can't decrease them, while they are used. " +
                                            "Free resources, by stopping workspaces, before changing the resources caps.")
    //TODO Fix name
    public void shouldCheckTestResourceDifferenceAvailabilityOnSuborganizationLevelOnAmountDecreasingBlabla() throws Exception {
        //given
        doCallRealMethod().when(manager).checkResourcesAvailability(anyString(), any(), any());
        ResourceImpl used = createTestResource(1000);
        doReturn(singletonList(used))
                .when(usageManager).getUsedResources(any());
        ResourceImpl caped = createTestResource(1000);
        ResourceImpl toCap = createTestResource(700);
        doReturn(createTestResource(300))
                .doThrow(new NoEnoughResourcesException(emptyList(), emptyList(), emptyList()))
                .when(resourceAggregator).deduct((Resource)any(), any());

        //when
        manager.checkResourcesAvailability(ORG_ID,
                                           singletonList(caped),
                                           singletonList(toCap));

        //then
        verify(usageManager).getUsedResources(ORG_ID);
        verify(resourceAggregator).deduct(caped, toCap);
        verify(resourceAggregator).deduct(toCap, used);
    }

    @Test
    public void shouldNotCheckTestResourceAvailabilityOnCapIncreasing() throws Exception {
        //given
        doCallRealMethod().when(manager).checkResourcesAvailability(anyString(), any(), any());
        ResourceImpl caped = createTestResource(700);
        ResourceImpl toCap = createTestResource(1000);
        doThrow(new NoEnoughResourcesException(emptyList(), emptyList(), emptyList()))
                .when(resourceAggregator).deduct((Resource)any(), any());

        //when
        manager.checkResourcesAvailability(ORG_ID,
                                           singletonList(caped),
                                           singletonList(toCap));

        //then
        verify(usageManager, never()).getUsedResources(ORG_ID);
    }

    @Test
    public void shouldCheckTestResourceOnSuborganizationLevelOnCapInitializing() throws Exception {
        //given
        doCallRealMethod().when(manager).checkResourcesAvailability(anyString(), any(), any());
        ResourceImpl toCap = createTestResource(1000);
        ResourceImpl used = createTestResource(500);
        doReturn(singletonList(used))
                .when(usageManager).getUsedResources(any());

        //when
        manager.checkResourcesAvailability(ORG_ID,
                                           emptyList(),
                                           singletonList(toCap));

        //then
        verify(usageManager).getUsedResources(ORG_ID);
        verify(resourceAggregator).deduct(toCap, used);
    }

    private ResourceImpl createTestResource(long amount) {
        return new ResourceImpl("test",
                                amount,
                                "init");
    }

    private OrganizationDistributedResourcesImpl createDistributedResources(long resourceAmount) {
        return new OrganizationDistributedResourcesImpl(ORG_ID,
                                                        singletonList(createTestResource(resourceAmount)));
    }
}
