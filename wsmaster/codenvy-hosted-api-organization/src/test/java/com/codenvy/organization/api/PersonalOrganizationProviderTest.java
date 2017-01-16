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

import com.codenvy.api.permission.server.AbstractPermissionsDomain;
import com.codenvy.organization.api.permissions.OrganizationDomain;
import com.codenvy.organization.api.personal.PersonalOrganizationProvider;
import com.codenvy.organization.spi.MemberDao;
import com.codenvy.organization.spi.OrganizationDao;
import com.codenvy.organization.spi.impl.MemberImpl;
import com.codenvy.organization.spi.jpa.JpaMemberDao;
import com.codenvy.organization.spi.jpa.JpaOrganizationDao;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.persist.jpa.JpaPersistModule;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.user.server.jpa.JpaUserDao;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.user.server.spi.UserDao;
import org.eclipse.che.commons.test.db.H2JpaCleaner;
import org.eclipse.che.commons.test.tck.JpaCleaner;
import org.eclipse.che.core.db.DBInitializer;
import org.eclipse.che.core.db.schema.SchemaInitializer;
import org.eclipse.che.core.db.schema.impl.flyway.FlywaySchemaInitializer;
import org.eclipse.che.inject.lifecycle.InitModule;
import org.eclipse.che.security.PasswordEncryptor;
import org.eclipse.che.security.SHA512PasswordEncryptor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.PostConstruct;
import java.util.concurrent.Callable;

import static org.eclipse.che.commons.test.db.H2TestHelper.inMemoryDefault;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * TODO Rework test
 */
public class PersonalOrganizationProviderTest {
    private UserDao             userDao;
    private OrganizationManager organizationManager;
    private JpaCleaner          cleaner;

    @BeforeMethod
    private void setUp() {
        final Injector injector = Guice.createInjector(new InitModule(PostConstruct.class),
                                                       new AbstractModule() {
                                                           @Override
                                                           protected void configure() {
                                                               bind(String[].class)
                                                                       .annotatedWith(Names.named("che.auth.reserved_user_names"))
                                                                       .toInstance(new String[0]);
                                                           }
                                                       }, new TestModule());
        userDao = injector.getInstance(UserDao.class);
        organizationManager = injector.getInstance(OrganizationManager.class);
        cleaner = injector.getInstance(H2JpaCleaner.class);
    }

    @AfterMethod
    private void cleanup() {
        cleaner.clean();
    }

    @Test
    public void shouldTest() throws Exception {
        UserImpl user = new UserImpl("user123", "user@mail.com", "username");
        userDao.create(user);

        assertNotNull(notFoundToNull(() -> organizationManager.getByName(user.getName())));

        UserImpl newUser = new UserImpl(user);
        newUser.setName("newUsername");
        userDao.update(newUser);

        assertNull(notFoundToNull(() -> organizationManager.getByName(user.getName())));
        assertNotNull(notFoundToNull(() -> organizationManager.getByName(newUser.getName())));

        userDao.remove(user.getId());

        assertNull(notFoundToNull(() -> organizationManager.getByName(newUser.getName())));
    }

    @Test(expectedExceptions = ConflictException.class, expectedExceptionsMessageRegExp = "User with such id/name/email/alias already exists")
    public void shouldTest1() throws Exception {
        UserImpl user = new UserImpl("user123", "user@mail.com", "username");
        userDao.create(user);

        userDao.create(user);
    }

    private static class TestModule extends AbstractModule {
        @Override
        protected void configure() {
            install(new JpaPersistModule("main"));
            bind(SchemaInitializer.class).toInstance(new FlywaySchemaInitializer(inMemoryDefault(), "che-schema", "codenvy-schema"));
            bind(DBInitializer.class).asEagerSingleton();

            bind(OrganizationDao.class).to(JpaOrganizationDao.class);
            bind(MemberDao.class).to(JpaMemberDao.class);
            bind(UserDao.class).to(JpaUserDao.class);
            // SHA-512 encryptor is faster than PBKDF2 so it is better for testing
            bind(PasswordEncryptor.class).to(SHA512PasswordEncryptor.class).in(Singleton.class);

            bind(new TypeLiteral<AbstractPermissionsDomain<MemberImpl>>() {}).to(OrganizationDomain.class);
            bind(JpaMemberDao.RemoveMembersBeforeOrganizationRemovedEventSubscriber.class).asEagerSingleton();

            bind(PersonalOrganizationProvider.class).asEagerSingleton();
        }
    }

    private static <T> T notFoundToNull(Callable<T> action) throws Exception {
        try {
            return action.call();
        } catch (NotFoundException x) {
            return null;
        }
    }
}
