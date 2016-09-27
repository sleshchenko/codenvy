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
package com.codenvy.resources.api;

import com.codenvy.resources.model.Resource;
import com.codenvy.resources.spi.impl.LicenseImpl;

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
 * Tests for {@link ResourcesManager}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class ResourcesManagerTest {
    @Mock
    private ResourcesAggregator  resourcesAggregator;
    @Mock
    private LicenseManager       licenseManager;
    @Mock
    private ResourceUsageTracker resourceUsageTracker;
    @Mock
    private LicenseImpl          license;

    private ResourcesManager resourcesManager;

    @BeforeMethod
    public void setUp() throws Exception {
        resourcesManager = spy(new ResourcesManager(resourcesAggregator,
                                                    licenseManager,
                                                    singleton(resourceUsageTracker)));
    }

    @Test
    public void shouldReturnTotalResourcesForAccount() throws Exception {
        TestResource testResource = new TestResource(1000);
        when(license.getTotalResources()).thenReturn(singletonList(testResource));
        when(licenseManager.getByAccount(eq("account123"))).thenReturn(license);

        final List<? extends Resource> totalResources = resourcesManager.getTotalResources("account123");

        verify(licenseManager).getByAccount(eq("account123"));
        assertEquals(totalResources.size(), 1);
        assertEquals(totalResources.get(0), testResource);
    }

    @Test
    public void shouldReturnUsedResourcesForAccount() throws Exception {
        TestResource usedResource = new TestResource(1000);
        when(resourceUsageTracker.getUsedResource(eq("account123"))).thenReturn(usedResource);

        final List<? extends Resource> usedResources = resourcesManager.getUsedResources("account123");

        verify(resourceUsageTracker).getUsedResource(eq("account123"));
        assertEquals(usedResources.size(), 1);
        assertEquals(usedResources.get(0), usedResource);
    }

    @Test
    public void shouldReturnAvailableResourcesForAccount() throws Exception {
        TestResource totalResources = new TestResource(2000);
        doReturn(singletonList(totalResources)).when(resourcesManager).getTotalResources(anyString());

        TestResource usedResource = new TestResource(500);
        doReturn(singletonList(usedResource)).when(resourcesManager).getUsedResources(anyString());

        final TestResource availableResource = new TestResource(1500);
        doReturn(singletonList(availableResource)).when(resourcesAggregator).deduct(any(), any());

        final List<? extends Resource> availableResources = resourcesManager.getAvailableResources("account123");

        verify(resourcesAggregator).deduct(eq(singletonList(totalResources)), eq(singletonList(usedResource)));
        verify(resourcesManager).getAvailableResources(eq("account123"));
        verify(resourcesManager).getUsedResources(eq("account123"));
        assertEquals(availableResources.size(), 1);
        assertEquals(availableResources.get(0), availableResource);
    }

    @Test(expectedExceptions = ConflictException.class, expectedExceptionsMessageRegExp = "No enough resources")
    public void shouldThrowConflictExceptionWhenAccountDoesNotHaveEnoughResourcesToReserve() throws Exception {
        doReturn(singletonList(emptyList())).when(resourcesManager).getAvailableResources(anyString());
        when(resourcesAggregator.deduct(any(), any())).thenThrow(new ConflictException("No enough resources"));

        resourcesManager.reserveResources("account123",
                                          Collections.singletonList(new TestResource(100)),
                                          () -> null);
    }

    @Test
    public void shouldReturnValueOfOperationOnResourcesReserve() throws Exception {
        final Object value = new Object();
        doReturn(singletonList(emptyList())).when(resourcesManager).getAvailableResources(anyString());
        doReturn(new ArrayList<>()).when(resourcesAggregator).deduct(any(), any());

        final Object result = resourcesManager.reserveResources("account123",
                                                                Collections.singletonList(new TestResource(100)),
                                                                () -> value);

        assertTrue(value == result);
    }

    @Test(expectedExceptions = ConflictException.class,
          expectedExceptionsMessageRegExp = "error")
    public void shouldRethrowExceptionWhenSomeExceptionOccursWhenReservingResources() throws Exception {
        doReturn(singletonList(emptyList())).when(resourcesManager).getAvailableResources(anyString());
        doReturn(new ArrayList<>()).when(resourcesAggregator).deduct(any(), any());

        resourcesManager.reserveResources("account123",
                                          Collections.singletonList(new TestResource(100)),
                                          () -> {
                                              throw new ConflictException("error");
                                          });
    }
}
