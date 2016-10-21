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
package com.codenvy.resource.api.usage;

import com.codenvy.resource.api.exception.NoEnoughResourcesException;
import com.codenvy.resource.api.ResourceAggregator;
import com.codenvy.resource.api.ResourceLockProvider;
import com.codenvy.resource.api.ResourceUsageTracker;
import com.codenvy.resource.api.ResourcesReserveTracker;
import com.codenvy.resource.api.license.LicenseManager;
import com.codenvy.resource.model.Resource;
import com.codenvy.resource.spi.impl.LicenseImpl;
import com.codenvy.resource.spi.impl.ResourceImpl;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.spi.AccountImpl;
import org.eclipse.che.api.core.ConflictException;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link ResourceUsageManager}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class ResourceUsageManagerTest {
    private static final String TEST_RESOURCE_TYPE   = "test";
    private static final String TEST_RESOURCE_UNIT   = "unit";
    private static final String RESERVE_TEST_ACCOUNT = "reserveTest";
    private static final String LOCK_TEST_ACCOUNT    = "lockTest";

    @Mock
    private ResourceAggregator      resourceAggregator;
    @Mock
    private ResourceUsageTracker    resourceUsageTracker;
    @Mock
    private ResourcesReserveTracker resourcesReserveTracker;
    @Mock
    private ResourceLockProvider    resourceLockProvider;

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private AccountManager accountManager;
    @Mock
    private LicenseImpl    license;

    private ResourceUsageManager resourceUsageManager;

    @BeforeMethod
    public void setUp() throws Exception {
        when(accountManager.getById(any())).thenReturn(new AccountImpl("account123", "testAccount", "test"));

        when(resourcesReserveTracker.getAccountType()).thenReturn(RESERVE_TEST_ACCOUNT);
        when(resourceLockProvider.getAccountType()).thenReturn(LOCK_TEST_ACCOUNT);
        when(resourceLockProvider.getLockId(anyString())).thenReturn("lockId");

        resourceUsageManager = spy(new ResourceUsageManager(resourceAggregator,
                                                            singleton(resourceUsageTracker),
                                                            singleton(resourcesReserveTracker),
                                                            accountManager,
                                                            licenseManager,
                                                            singleton(resourceLockProvider)));
    }

    @Test
    public void shouldReturnLicenseResourcesWhenThereIsNoResourcesReserveTrackerWhenGettingTotalResources() throws Exception {
        ResourceImpl testResource = new ResourceImpl(TEST_RESOURCE_TYPE, 1000, TEST_RESOURCE_UNIT);
        when(license.getTotalResources()).thenReturn(singletonList(testResource));
        when(licenseManager.getByAccount(eq("account123"))).thenReturn(license);

        final List<? extends Resource> totalResources = resourceUsageManager.getTotalResources("account123");

        verify(licenseManager).getByAccount(eq("account123"));
        assertEquals(totalResources.size(), 1);
        assertEquals(totalResources.get(0), testResource);
    }

    @Test
    public void shouldDeductResourcesReserveFromLicenseResourcesWhenThereIsAResourcesReserveTrackerWhenGettingTotalResources()
            throws Exception {
        when(accountManager.getById(any())).thenReturn(new AccountImpl("account123", "testAccount", RESERVE_TEST_ACCOUNT));
        ResourceImpl reservedResource = new ResourceImpl(TEST_RESOURCE_TYPE, 300, TEST_RESOURCE_UNIT);
        doReturn(singletonList(reservedResource)).when(resourcesReserveTracker).getReservedResources(any());

        ResourceImpl licenseResource = new ResourceImpl(TEST_RESOURCE_TYPE, 1000, TEST_RESOURCE_UNIT);
        when(license.getTotalResources()).thenReturn(singletonList(licenseResource));
        when(licenseManager.getByAccount(eq("account123"))).thenReturn(license);

        final ResourceImpl availableResource = new ResourceImpl(TEST_RESOURCE_TYPE, 700, TEST_RESOURCE_UNIT);
        doReturn(singletonList(availableResource)).when(resourceAggregator).deduct(any(), any());

        final List<? extends Resource> totalResources = resourceUsageManager.getTotalResources("account123");

        verify(licenseManager).getByAccount(eq("account123"));
        assertEquals(totalResources.size(), 1);
        assertEquals(totalResources.get(0), availableResource);
        verify(resourceAggregator).deduct(eq(singletonList(licenseResource)), eq(singletonList(reservedResource)));
    }

    @Test
    public void shouldReturnUsedResourcesForAccount() throws Exception {
        ResourceImpl usedResource = new ResourceImpl(TEST_RESOURCE_TYPE, 1000, TEST_RESOURCE_UNIT);
        when(resourceUsageTracker.getUsedResource(eq("account123"))).thenReturn(usedResource);

        final List<? extends Resource> usedResources = resourceUsageManager.getUsedResources("account123");

        verify(resourceUsageTracker).getUsedResource(eq("account123"));
        assertEquals(usedResources.size(), 1);
        assertEquals(usedResources.get(0), usedResource);
    }

    @Test
    public void shouldReturnAvailableResourcesForAccount() throws Exception {
        ResourceImpl totalResources = new ResourceImpl(TEST_RESOURCE_TYPE, 2000, TEST_RESOURCE_UNIT);
        doReturn(singletonList(totalResources)).when(resourceUsageManager).getTotalResources(anyString());

        ResourceImpl usedResource = new ResourceImpl(TEST_RESOURCE_TYPE, 500, TEST_RESOURCE_UNIT);
        doReturn(singletonList(usedResource)).when(resourceUsageManager).getUsedResources(anyString());

        final ResourceImpl availableResource = new ResourceImpl(TEST_RESOURCE_TYPE, 1500, TEST_RESOURCE_UNIT);
        doReturn(singletonList(availableResource)).when(resourceAggregator).deduct(any(), any());

        final List<? extends Resource> availableResources = resourceUsageManager.getAvailableResources("account123");

        verify(resourceAggregator).deduct(eq(singletonList(totalResources)), eq(singletonList(usedResource)));
        verify(resourceUsageManager).getAvailableResources(eq("account123"));
        verify(resourceUsageManager).getUsedResources(eq("account123"));
        assertEquals(availableResources.size(), 1);
        assertEquals(availableResources.get(0), availableResource);
    }

    @Test(expectedExceptions = NoEnoughResourcesException.class)
    public void shouldThrowConflictExceptionWhenAccountDoesNotHaveEnoughResourcesToReserve() throws Exception {
        doReturn(singletonList(emptyList())).when(resourceUsageManager).getAvailableResources(anyString());
        when(resourceAggregator.deduct(any(), any())).thenThrow(new NoEnoughResourcesException(Collections.emptyList()));

        resourceUsageManager.lockResources("account123",
                                           Collections.singletonList(new ResourceImpl(TEST_RESOURCE_TYPE, 100, TEST_RESOURCE_UNIT)),
                                           () -> null);
    }

    @Test
    public void shouldReturnValueOfOperationOnResourcesReserve() throws Exception {
        final Object value = new Object();
        doReturn(singletonList(emptyList())).when(resourceUsageManager).getAvailableResources(anyString());
        doReturn(new ArrayList<>()).when(resourceAggregator).deduct(any(), any());

        final Object result = resourceUsageManager.lockResources("account123",
                                                                 Collections.singletonList(
                                                                         new ResourceImpl(TEST_RESOURCE_TYPE, 100, TEST_RESOURCE_UNIT)),
                                                                 () -> value);

        assertTrue(value == result);
    }

    @Test
    public void shouldUseCustomLockIdIfThereIsResourcesLockProviderOnResourcesReserve() throws Exception {
        when(accountManager.getById(any())).thenReturn(new AccountImpl("account123", "testAccount", LOCK_TEST_ACCOUNT));
        doReturn(singletonList(emptyList())).when(resourceUsageManager).getAvailableResources(anyString());
        doReturn(new ArrayList<>()).when(resourceAggregator).deduct(any(), any());

        resourceUsageManager.lockResources("account123",
                                           Collections.singletonList(
                                                   new ResourceImpl(TEST_RESOURCE_TYPE, 100, TEST_RESOURCE_UNIT)),
                                           () -> null);

        resourceLockProvider.getLockId("account123");
    }

    @Test(expectedExceptions = ConflictException.class,
          expectedExceptionsMessageRegExp = "error")
    public void shouldRethrowExceptionWhenSomeExceptionOccursWhenReservingResources() throws Exception {
        doReturn(singletonList(emptyList())).when(resourceUsageManager).getAvailableResources(anyString());
        doReturn(new ArrayList<>()).when(resourceAggregator).deduct(any(), any());

        resourceUsageManager.lockResources("account123",
                                           Collections.singletonList(new ResourceImpl(TEST_RESOURCE_TYPE, 100, TEST_RESOURCE_UNIT)),
                                           () -> {
                                               throw new ConflictException("error");
                                           });
    }
}
