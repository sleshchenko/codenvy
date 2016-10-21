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
package com.codenvy.organization.api.resource;

import com.codenvy.organization.shared.model.OrganizationResourcesLimit;
import com.codenvy.organization.spi.OrganizationResourcesLimitDao;
import com.codenvy.organization.spi.impl.OrganizationResourcesLimitImpl;
import com.codenvy.resource.spi.impl.ResourceImpl;

import org.eclipse.che.api.core.Page;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link OrganizationResourcesManager}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class OrganizationResourcesManagerTest {
    @Mock
    private OrganizationResourcesLimitDao limitsDao;

    @InjectMocks
    private OrganizationResourcesManager manager;

    @Test
    public void shouldSetResourceLimit() throws Exception {
        //given
        final OrganizationResourcesLimitImpl limitToStore = createLimit();
        //when
        manager.setResourcesLimit(limitToStore);

        //then
        verify(limitsDao).store(limitToStore);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeOnSettingNullResourceLimit() throws Exception {
        //when
        manager.setResourcesLimit(null);
    }

    @Test
    public void shouldGetResourcesLimit() throws Exception {
        //given
        final OrganizationResourcesLimitImpl existedLimit = createLimit();
        when(limitsDao.get(anyString())).thenReturn(existedLimit);

        //when
        final OrganizationResourcesLimit fetchedLimit = manager.get("organization123");

        //then
        assertEquals(fetchedLimit, existedLimit);
        verify(limitsDao).get("organization123");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeOnGettingLimitByNullOrganizationId() throws Exception {
        //when
        manager.get(null);
    }

    @Test
    public void shouldGetResourcesLimitsByParentOrganizationId() throws Exception {
        //given
        final Page<OrganizationResourcesLimitImpl> existedPage = new Page<>(singletonList(createLimit()), 2, 1, 4);
        when(limitsDao.getByParent(anyString(), anyInt(), anyLong())).thenReturn(existedPage);

        //when
        final Page<? extends OrganizationResourcesLimit> fetchedPage = manager.getResourcesLimits("organization123", 1, 2);

        //then
        assertEquals(fetchedPage, existedPage);
        verify(limitsDao).getByParent("organization123", 1, 2L);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeOnGettingLimitsByNullOrganizationId() throws Exception {
        //when
        manager.getResourcesLimits(null, 1, 1);
    }

    @Test
    public void shouldRemoveResourcesLimit() throws Exception {
        //when
        manager.remove("organization123");

        //then
        verify(limitsDao).remove("organization123");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeOnRemovingLimitByNullOrganizationId() throws Exception {
        //when
        manager.remove(null);
    }

    private OrganizationResourcesLimitImpl createLimit() {
        return new OrganizationResourcesLimitImpl("organization123",
                                                  singletonList(new ResourceImpl("test",
                                                                                 100,
                                                                                 "init")));
    }
}
