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

import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link OrganizationResourcesReserveTracker}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class OrganizationResourcesReserveTrackerTest {
    /*
    TODO Fix
    @Captor
    private ArgumentCaptor<List<? extends Resource>>   resourcesToAggregateCaptor;
    @Mock
    private Provider<OrganizationResourcesDistributor> managerProvider;
    @Mock
    private OrganizationResourcesDistributor           organizationResourcesDistributor;
    @Mock
    private ResourceAggregator                         resourceAggregator;

    @Mock
    private Page<OrganizationDistributedResources> distributedResourcesPage;
    @Mock
    private Page.PageRef                nextPageRef;
    @Mock
    private OrganizationDistributedResources       distributedResources;

    @InjectMocks
    private OrganizationResourcesReserveTracker resourcesReserveTracker;

    @BeforeMethod
    public void setUp() throws Exception {
        when(managerProvider.get()).thenReturn(organizationResourcesDistributor);

        when(nextPageRef.getPageSize()).thenReturn(ORGANIZATION_RESOURCES_PER_PAGE);
        when(nextPageRef.getItemsBefore()).thenReturn(1L);

        when(distributedResourcesPage.getNextPageRef()).thenReturn(nextPageRef);
        when(distributedResourcesPage.hasNextPage()).thenReturn(true)
                                                    .thenReturn(false);
        when(distributedResourcesPage.getItems()).thenReturn(singletonList(distributedResources));

        doReturn(distributedResourcesPage).when(organizationResourcesDistributor).get(any(), anyInt(), anyLong());
    }

    @Test
    public void shouldReturnSumOfSuborganizationsDistributedResourcesWhenGettingReservedResources() throws Exception {
        //given
        final ResourceImpl workspacesResource = new ResourceImpl("workspaces", 1, "unit");
        final ResourceImpl ramResource1 = new ResourceImpl("RAM", 1200, "mb");
        final ResourceImpl ramResource2 = new ResourceImpl("RAM", 800, "mb");
        final ResourceImpl aggregatedRAM = new ResourceImpl("RAM", 2000, "mb");

        doReturn(ImmutableMap.of("RAM", aggregatedRAM,
                                 "workspaces", workspacesResource))
                .when(resourceAggregator).aggregateByType(any());
        doReturn(singletonList(ramResource1))
                .doReturn(asList(ramResource2,
                                 workspacesResource))
                .when(distributedResources).getReservedResources();

        //when
        final List<? extends Resource> reservedResources = resourcesReserveTracker.getReservedResources("organization123");

        //then
        verify(resourceAggregator).aggregateByType(resourcesToAggregateCaptor.capture());
        final List<? extends Resource> resourcesToAggregate = resourcesToAggregateCaptor.getValue();
        assertTrue(resourcesToAggregate.contains(ramResource1));
        assertTrue(resourcesToAggregate.contains(ramResource2));
        assertTrue(resourcesToAggregate.contains(workspacesResource));

        verify(organizationResourcesDistributor).get("organization123", ORGANIZATION_RESOURCES_PER_PAGE, 0);
        verify(organizationResourcesDistributor).get("organization123", ORGANIZATION_RESOURCES_PER_PAGE, 1);
        assertEquals(reservedResources.size(), 2);
        assertTrue(reservedResources.contains(aggregatedRAM));
        assertTrue(reservedResources.contains(workspacesResource));
    }

    @Test
    public void shouldReturnOrganizationalAccountType() throws Exception {
        final String accountType = resourcesReserveTracker.getAccountType();

        assertEquals(accountType, OrganizationImpl.ORGANIZATIONAL_ACCOUNT);
    }
    */
}
