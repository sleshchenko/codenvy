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
package com.codenvy.organization.spi.tck;

import com.codenvy.organization.spi.OrganizationResourcesDao;
import com.codenvy.organization.spi.impl.OrganizationImpl;
import com.codenvy.organization.spi.impl.OrganizationResourcesImpl;
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
 * Tests for {@link OrganizationResourcesDao}
 *
 * @author Sergii Leschenko
 */
@Listeners(TckListener.class)
@Test(suiteName = OrganizationResourcesDaoTest.SUITE_NAME)
public class OrganizationResourcesDaoTest {
    public static final String SUITE_NAME = "OrganizationDistributedResourcesDaoTck";

    private static final String TEST_RESOURCE_TYPE           = "Test";
    private static final int    ORGANIZATION_RESOURCES_COUNT = 3;

    private OrganizationResourcesImpl[] distributedResources;
    private OrganizationImpl            parentOrganization;
    private OrganizationImpl[]          suborganizations;

    @Inject
    private TckRepository<OrganizationResourcesImpl> distributedResourcesRepository;

    @Inject
    private TckRepository<OrganizationImpl> organizationsRepository;

    @Inject
    private OrganizationResourcesDao distributedResourcesDao;

    @BeforeMethod
    private void setUp() throws Exception {
        parentOrganization = new OrganizationImpl("parentOrg", "parentOrgName", null);
        suborganizations = new OrganizationImpl[ORGANIZATION_RESOURCES_COUNT];
        distributedResources = new OrganizationResourcesImpl[ORGANIZATION_RESOURCES_COUNT];
        for (int i = 0; i < ORGANIZATION_RESOURCES_COUNT; i++) {
            suborganizations[i] = new OrganizationImpl("suborgId-" + i, "suborgName" + i, parentOrganization.getId());
            distributedResources[i] = new OrganizationResourcesImpl(suborganizations[i].getId(),
                                                                    singletonList(new ResourceImpl(TEST_RESOURCE_TYPE,
                                                                                                   i,
                                                                                                   "test")),
                                                                    singletonList(new ResourceImpl(TEST_RESOURCE_TYPE,
                                                                                                   i,
                                                                                                   "test")));
        }
        organizationsRepository.createAll(Collections.singletonList(parentOrganization));
        organizationsRepository.createAll(Arrays.asList(suborganizations));
        distributedResourcesRepository.createAll(Arrays.asList(distributedResources));
    }

    @AfterMethod
    private void cleanup() throws Exception {
        distributedResourcesRepository.removeAll();
        organizationsRepository.removeAll();
    }

    @Test
    public void shouldCreateDistributedResourcesWhenStoringNotExistentOne() throws Exception {
        //given
        OrganizationResourcesImpl toStore = distributedResources[0];
        distributedResourcesDao.remove(toStore.getOrganizationId());

        //when
        distributedResourcesDao.store(toStore);

        //then
        assertEquals(distributedResourcesDao.get(toStore.getOrganizationId()), copy(toStore));
    }

    @Test
    public void shouldUpdateDistributedResourcesWhenStoringExistentOne() throws Exception {
        //given
        OrganizationResourcesImpl toStore = new OrganizationResourcesImpl(distributedResources[0].getOrganizationId(),
                                                                          singletonList(new ResourceImpl(TEST_RESOURCE_TYPE,
                                                                                                         1000,
                                                                                                         "unit")),
                                                                          singletonList(new ResourceImpl(TEST_RESOURCE_TYPE,
                                                                                                         1000,
                                                                                                         "unit")));

        //when
        distributedResourcesDao.store(toStore);

        //then
        assertEquals(distributedResourcesDao.get(toStore.getOrganizationId()), copy(toStore));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenStoringNullableDistributedResources() throws Exception {
        //when
        distributedResourcesDao.store(null);
    }

    @Test
    public void shouldGetDistributedResourcesForSpecifiedOrganizationId() throws Exception {
        //given
        OrganizationResourcesImpl stored = distributedResources[0];

        //when
        OrganizationResourcesImpl fetched = distributedResourcesDao.get(stored.getOrganizationId());

        //then
        assertEquals(fetched, copy(stored));
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldThrowNotFoundExceptionWhenGettingNonExistingDistributedResources() throws Exception {
        //when
        distributedResourcesDao.get("account123");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenGettingDistributedResourcesByNullOrganizationId() throws Exception {
        //when
        distributedResourcesDao.get(null);
    }

    @Test
    public void shouldGetDistributedResourcesByParent() throws Exception {
        //when
        final Page<OrganizationResourcesImpl> children = distributedResourcesDao.getByParent(parentOrganization.getId(), 1, 1);

        //then
        assertEquals(children.getTotalItemsCount(), 3);
        assertEquals(children.getItemsCount(), 1);
        assertTrue(children.getItems().contains(copy(distributedResources[0]))
                   ^ children.getItems().contains(copy(distributedResources[1]))
                   ^ children.getItems().contains(copy(distributedResources[2])));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenGettingDistributedResourcesByNullParentId() throws Exception {
        //when
        distributedResourcesDao.getByParent(null, 1, 1);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldRemoveDistributedResources() throws Exception {
        //given
        OrganizationResourcesImpl distributedResource = distributedResources[0];

        //when
        distributedResourcesDao.remove(distributedResource.getOrganizationId());

        //then
        distributedResourcesDao.get(distributedResource.getOrganizationId());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldThrowNpeWhenRemovingDistributedResourcesByNullId() throws Exception {
        //when
        distributedResourcesDao.remove(null);
    }

    private OrganizationResourcesImpl copy(OrganizationResourcesImpl distributedResource) {
        return new OrganizationResourcesImpl(distributedResource);
    }
}
