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
package com.codenvy.organization.spi.tck;

import com.codenvy.organization.spi.OrganizationResourcesLimitDao;
import com.codenvy.organization.spi.impl.OrganizationImpl;
import com.codenvy.organization.spi.impl.OrganizationResourcesLimitImpl;
import com.codenvy.resource.spi.impl.ResourceImpl;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.commons.test.tck.TckListener;
import org.eclipse.che.commons.test.tck.repository.TckRepository;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link OrganizationResourcesLimitDao}
 *
 * @author Sergii Leschenko
 */
@Listeners(TckListener.class)
@Test(suiteName = OrganizationResourcesLimitDaoTest.SUITE_NAME)
public class OrganizationResourcesLimitDaoTest {
    public static final String SUITE_NAME = "SuborganizationResourcesLimitDaoTck";

    private static final String TEST_RESOURCE_TYPE = "Test";
    private static final int    COUNTS_OF_LIMITS   = 3;

    private OrganizationResourcesLimitImpl[] limits;
    private OrganizationImpl                 parentOrganization;
    private OrganizationImpl[]               suborganizations;

    @Inject
    private TckRepository<OrganizationResourcesLimitImpl> limitRepository;

    @Inject
    private TckRepository<OrganizationImpl> organizationsRepository;

    @Inject
    private OrganizationResourcesLimitDao limitDao;

    @BeforeMethod
    private void setUp() throws Exception {
        parentOrganization = new OrganizationImpl("parentOrg", "parentOrgName", null);
        suborganizations = new OrganizationImpl[COUNTS_OF_LIMITS];
        limits = new OrganizationResourcesLimitImpl[COUNTS_OF_LIMITS];
        for (int i = 0; i < COUNTS_OF_LIMITS; i++) {
            suborganizations[i] = new OrganizationImpl("suborgId-" + i, "suborgName" + i, parentOrganization.getId());
            limits[i] = new OrganizationResourcesLimitImpl(suborganizations[i].getId(),
                                                           singletonList(new ResourceImpl(TEST_RESOURCE_TYPE,
                                                                                          i,
                                                                                          "test")));
        }
        organizationsRepository.createAll(Collections.singletonList(parentOrganization));
        organizationsRepository.createAll(Arrays.asList(suborganizations));
        limitRepository.createAll(Arrays.asList(limits));
    }

    @AfterMethod
    private void cleanup() throws Exception {
        limitRepository.removeAll();
        organizationsRepository.removeAll();
    }

    @Test
    public void shouldCreateNewResourcesLimitWhenStoringNotExistentOne() throws Exception {
        //given
        OrganizationResourcesLimitImpl toStore = limits[0];
        limitDao.remove(toStore.getOrganizationId());

        //when
        limitDao.store(toStore);

        //then
        assertEquals(limitDao.get(toStore.getOrganizationId()), toStore);
    }

    @Test
    public void shouldUpdateResourcesLimitWhenStoringExistentOne() throws Exception {
        //given
        OrganizationResourcesLimitImpl toStore = new OrganizationResourcesLimitImpl(limits[0].getOrganizationId(),
                                                                                    singletonList(new ResourceImpl(TEST_RESOURCE_TYPE,
                                                                                                                   1000,
                                                                                                                   "unit")));

        //when
        limitDao.store(toStore);

        //then
        assertEquals(limitDao.get(toStore.getOrganizationId()), toStore);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenStoringNullableResourcesLimit() throws Exception {
        //when
        limitDao.store(null);
    }

    @Test
    public void shouldGetResourcesLimitForSpecifiedOrganizationId() throws Exception {
        //given
        OrganizationResourcesLimitImpl stored = limits[0];

        //when
        OrganizationResourcesLimitImpl fetched = limitDao.get(stored.getOrganizationId());

        //then
        assertEquals(fetched, stored);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldThrowNotFoundExceptionWhenGettingNonExistentResourcesLimit() throws Exception {
        //when
        limitDao.get("account123");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenGettingResourcesLimitByNullOrganizationId() throws Exception {
        //when
        limitDao.get(null);
    }

    @Test
    public void shouldGetLimitsByParent() throws Exception {
        //when
        final Page<OrganizationResourcesLimitImpl> children = limitDao.getByParent(parentOrganization.getId(), 1, 1);

        //then
        assertEquals(children.getTotalItemsCount(), 3);
        assertEquals(children.getItemsCount(), 1);
        assertTrue(children.getItems().contains(limits[0])
                   ^ children.getItems().contains(limits[1])
                   ^ children.getItems().contains(limits[2]));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenGettingResourcesLimitsByNullParentId() throws Exception {
        //when
        limitDao.getByParent(null, 1, 1);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldRemoveResourcesLimit() throws Exception {
        //given
        OrganizationResourcesLimitImpl existedLimit = limits[0];

        //when
        limitDao.remove(existedLimit.getOrganizationId());

        //then
        limitDao.get(existedLimit.getOrganizationId());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenRemovingResourcesLimitByNullId() throws Exception {
        //when
        limitDao.remove(null);
    }
}
