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

import com.codenvy.organization.api.OrganizationManager;
import com.codenvy.resource.api.ResourceAggregator;
import com.codenvy.resource.api.usage.ResourceUsageManager;
import com.google.inject.Inject;

import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Test;
import org.mockito.*;
import org.testng.annotations.*;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Tests for {@link OrganizationResourcesLimitLocker}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class OrganizationResourcesLimitLockerTest {
    @Mock
    private OrganizationResourcesManager suborganizationResourcesLimitDao;
    @Mock
    private ResourceAggregator           resourceAggregator;
    @Mock
    private OrganizationManager          organizationManager;
    @Mock
    private ResourceUsageManager         resourceUsageManager;

    @InjectMocks
    private OrganizationResourcesLimitLocker limitLocker;

    @Test
    public void shouldTest() throws Exception {
        //given
        //TODO Implement tests

        //when

        //then
    }
}
