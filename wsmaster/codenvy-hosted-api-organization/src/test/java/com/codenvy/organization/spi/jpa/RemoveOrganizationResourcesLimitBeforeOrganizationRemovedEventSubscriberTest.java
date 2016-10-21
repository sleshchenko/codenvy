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

import com.codenvy.organization.api.OrganizationJpaModule;
import com.codenvy.organization.spi.impl.OrganizationImpl;
import com.codenvy.organization.spi.impl.OrganizationResourcesLimitImpl;
import com.codenvy.organization.spi.jpa.JpaOrganizationResourcesLimitDao.RemoveOrganizationResourcesLimitBeforeOrganizationRemovedEventSubscriber;
import com.codenvy.resource.spi.impl.ResourceImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.persist.jpa.JpaPersistModule;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.jdbc.jpa.eclipselink.EntityListenerInjectionManagerInitializer;
import org.eclipse.che.api.core.jdbc.jpa.guice.JpaInitializer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static java.util.Collections.singletonList;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.testng.Assert.assertNull;

/**
 * Tests for {@link RemoveOrganizationResourcesLimitBeforeOrganizationRemovedEventSubscriber}
 *
 * @author Sergii Leschenko
 */
public class RemoveOrganizationResourcesLimitBeforeOrganizationRemovedEventSubscriberTest {
    private EntityManager manager;

    private JpaOrganizationDao               jpaOrganizationDao;
    private JpaOrganizationResourcesLimitDao limitDao;

    private RemoveOrganizationResourcesLimitBeforeOrganizationRemovedEventSubscriber suborganizationsRemover;

    private OrganizationImpl               organization;
    private OrganizationResourcesLimitImpl limit;


    @BeforeClass
    public void setupEntities() throws Exception {
        organization = new OrganizationImpl("org1", "parentOrg", null);
        limit = new OrganizationResourcesLimitImpl(organization.getId(),
                                                   singletonList(new ResourceImpl("test",
                                                                                  1020,
                                                                                  "unit")));

        Injector injector = com.google.inject.Guice.createInjector(new OrganizationJpaModule(), new TestModule());

        manager = injector.getInstance(EntityManager.class);
        jpaOrganizationDao = injector.getInstance(JpaOrganizationDao.class);
        limitDao = injector.getInstance(JpaOrganizationResourcesLimitDao.class);
        suborganizationsRemover = injector.getInstance(RemoveOrganizationResourcesLimitBeforeOrganizationRemovedEventSubscriber.class);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        manager.getTransaction().begin();
        manager.persist(organization);
        manager.persist(limit);
        manager.getTransaction().commit();
        suborganizationsRemover.subscribe();
    }

    @AfterMethod
    public void cleanup() {
        suborganizationsRemover.unsubscribe();

        manager.getTransaction().begin();

        final OrganizationImpl managedOrganization = manager.find(OrganizationImpl.class, this.organization.getId());
        if (managedOrganization != null) {
            manager.remove(managedOrganization);
        }

        final OrganizationResourcesLimitImpl managedLimit = manager.find(OrganizationResourcesLimitImpl.class, this.organization.getId());
        if (managedLimit != null) {
            manager.remove(managedLimit);
        }

        manager.getTransaction().commit();
    }

    @AfterClass
    public void shutdown() throws Exception {
        manager.getEntityManagerFactory().close();
    }

    @Test
    public void shouldRemoveOrganizationLimitWhenOrganizationIsRemoved() throws Exception {
        jpaOrganizationDao.remove(organization.getId());

        assertNull(notFoundToNull(() -> limitDao.get(organization.getId())));
    }

    private static <T> T notFoundToNull(Callable<T> action) throws Exception {
        try {
            return action.call();
        } catch (NotFoundException x) {
            return null;
        }
    }

    private static class TestModule extends AbstractModule {
        @Override
        protected void configure() {
            Map<String, String> properties = new HashMap<>();
            if (System.getProperty("jdbc.driver") != null) {
                properties.put(JDBC_DRIVER, System.getProperty("jdbc.driver"));
                properties.put(JDBC_URL, System.getProperty("jdbc.url"));
                properties.put(JDBC_USER, System.getProperty("jdbc.user"));
                properties.put(JDBC_PASSWORD, System.getProperty("jdbc.password"));
            }
            JpaPersistModule main = new JpaPersistModule("main");
            main.properties(properties);
            install(main);
            bind(JpaInitializer.class).asEagerSingleton();
            bind(EntityListenerInjectionManagerInitializer.class).asEagerSingleton();
        }
    }
}
