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
package com.codenvy.integration.jpa.cascaderemoval;

import com.codenvy.api.machine.server.jpa.JpaRecipePermissionsDao;
import com.codenvy.api.machine.server.jpa.OnPremisesJpaMachineModule;
import com.codenvy.api.machine.server.recipe.RecipeDomain;
import com.codenvy.api.machine.server.recipe.RecipePermissionsImpl;
import com.codenvy.api.permission.server.model.impl.AbstractPermissions;
import com.codenvy.api.permission.server.spi.PermissionsDao;
import com.codenvy.api.workspace.server.jpa.OnPremisesJpaWorkspaceModule;
import com.codenvy.api.workspace.server.jpa.listener.RemoveStackOnLastUserRemovedEventSubscriber;
import com.codenvy.api.workspace.server.spi.WorkerDao;
import com.codenvy.api.workspace.server.spi.jpa.JpaStackPermissionsDao;
import com.codenvy.api.workspace.server.stack.StackDomain;
import com.codenvy.api.workspace.server.stack.StackPermissionsImpl;
import com.codenvy.organization.api.OrganizationJpaModule;
import com.codenvy.organization.api.OrganizationManager;
import com.codenvy.organization.api.event.BeforeOrganizationRemovedEvent;
import com.codenvy.organization.api.listener.RemoveOrganizationDistributedResourcesSubscriber;
import com.codenvy.organization.api.listener.RemoveOrganizationOnLastUserRemovedEventSubscriber;
import com.codenvy.organization.api.permissions.OrganizationDomain;
import com.codenvy.organization.api.resource.SuborganizationResourcesProvider;
import com.codenvy.organization.spi.MemberDao;
import com.codenvy.organization.spi.OrganizationDao;
import com.codenvy.organization.spi.OrganizationResourcesDao;
import com.codenvy.organization.spi.impl.MemberImpl;
import com.codenvy.organization.spi.impl.OrganizationImpl;
import com.codenvy.organization.spi.impl.OrganizationResourcesImpl;
import com.codenvy.resource.api.RamResourceType;
import com.codenvy.resource.api.ResourceLockKeyProvider;
import com.codenvy.resource.api.ResourceUsageTracker;
import com.codenvy.resource.api.ResourcesReserveTracker;
import com.codenvy.resource.api.license.ResourcesProvider;
import com.codenvy.resource.model.ResourceType;
import com.codenvy.resource.spi.FreeResourcesLimitDao;
import com.codenvy.resource.spi.impl.FreeResourcesLimitImpl;
import com.codenvy.resource.spi.impl.ResourceImpl;
import com.codenvy.resource.spi.jpa.JpaFreeResourcesLimitDao;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.persist.jpa.JpaPersistModule;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.api.AccountModule;
import org.eclipse.che.account.spi.AccountDao;
import org.eclipse.che.account.spi.AccountImpl;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.factory.server.jpa.FactoryJpaModule;
import org.eclipse.che.api.factory.server.model.impl.FactoryImpl;
import org.eclipse.che.api.factory.server.spi.FactoryDao;
import org.eclipse.che.api.machine.server.jpa.MachineJpaModule;
import org.eclipse.che.api.machine.server.model.impl.SnapshotImpl;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.server.spi.RecipeDao;
import org.eclipse.che.api.machine.server.spi.SnapshotDao;
import org.eclipse.che.api.ssh.server.jpa.SshJpaModule;
import org.eclipse.che.api.ssh.server.model.impl.SshPairImpl;
import org.eclipse.che.api.ssh.server.spi.SshDao;
import org.eclipse.che.api.user.server.event.BeforeUserRemovedEvent;
import org.eclipse.che.api.user.server.jpa.UserJpaModule;
import org.eclipse.che.api.user.server.model.impl.ProfileImpl;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.user.server.spi.PreferenceDao;
import org.eclipse.che.api.user.server.spi.ProfileDao;
import org.eclipse.che.api.user.server.spi.UserDao;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.WorkspaceRuntimes;
import org.eclipse.che.api.workspace.server.WorkspaceSharedPool;
import org.eclipse.che.api.workspace.server.jpa.WorkspaceJpaModule;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackImpl;
import org.eclipse.che.api.workspace.server.spi.StackDao;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.test.db.H2JpaCleaner;
import org.eclipse.che.commons.test.tck.TckResourcesCleaner;
import org.eclipse.che.core.db.DBInitializer;
import org.eclipse.che.core.db.cascade.CascadeEventSubscriber;
import org.eclipse.che.core.db.cascade.event.CascadeEvent;
import org.eclipse.che.core.db.schema.SchemaInitializer;
import org.eclipse.che.core.db.schema.impl.flyway.FlywaySchemaInitializer;
import org.eclipse.che.inject.lifecycle.InitModule;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.codenvy.api.permission.server.AbstractPermissionsDomain.SET_PERMISSIONS;
import static com.codenvy.integration.jpa.cascaderemoval.TestObjectsFactory.createAccount;
import static com.codenvy.integration.jpa.cascaderemoval.TestObjectsFactory.createFactory;
import static com.codenvy.integration.jpa.cascaderemoval.TestObjectsFactory.createFreeResourcesLimit;
import static com.codenvy.integration.jpa.cascaderemoval.TestObjectsFactory.createPreferences;
import static com.codenvy.integration.jpa.cascaderemoval.TestObjectsFactory.createProfile;
import static com.codenvy.integration.jpa.cascaderemoval.TestObjectsFactory.createRecipe;
import static com.codenvy.integration.jpa.cascaderemoval.TestObjectsFactory.createSnapshot;
import static com.codenvy.integration.jpa.cascaderemoval.TestObjectsFactory.createSshPair;
import static com.codenvy.integration.jpa.cascaderemoval.TestObjectsFactory.createStack;
import static com.codenvy.integration.jpa.cascaderemoval.TestObjectsFactory.createUser;
import static com.codenvy.integration.jpa.cascaderemoval.TestObjectsFactory.createWorker;
import static com.codenvy.integration.jpa.cascaderemoval.TestObjectsFactory.createWorkspace;
import static com.codenvy.resource.spi.jpa.JpaFreeResourcesLimitDao.RemoveFreeResourcesLimitSubscriber;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.eclipse.che.commons.test.db.H2TestHelper.inMemoryDefault;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Tests top-level entities cascade removals.
 *
 * @author Yevhenii Voevodin
 */
public class JpaEntitiesCascadeRemovalTest {

    private Injector                 injector;
    private EventService             eventService;
    private AccountDao               accountDao;
    private PreferenceDao            preferenceDao;
    private UserDao                  userDao;
    private ProfileDao               profileDao;
    private WorkspaceDao             workspaceDao;
    private SnapshotDao              snapshotDao;
    private SshDao                   sshDao;
    private FactoryDao               factoryDao;
    private RecipeDao                recipeDao;
    private StackDao                 stackDao;
    private WorkerDao                workerDao;
    private JpaRecipePermissionsDao  recipePermissionsDao;
    private JpaStackPermissionsDao   stackPermissionsDao;
    private FreeResourcesLimitDao    freeResourcesLimitDao;
    private OrganizationManager      organizationManager;
    private OrganizationDao          organizationDao;
    private MemberDao                memberDao;
    private OrganizationResourcesDao organizationResourcesDao;

    /** User is a root of dependency tree. */
    private UserImpl user;

    private UserImpl user2;
    private UserImpl user3;

    private AccountImpl account;

    /** Profile depends on user. */
    private ProfileImpl profile;

    /** Preferences depend on user. */
    private Map<String, String> preferences;


    /** Workspaces depend on user. */
    private WorkspaceImpl workspace1;
    private WorkspaceImpl workspace2;

    /** to test workers */
    private WorkspaceImpl workspace3;
    /** to test workspace removing after organization removing */
    private WorkspaceImpl workspace4;

    /** SshPairs depend on user. */
    private SshPairImpl sshPair1;
    private SshPairImpl sshPair2;

    /** Factories depend on user. */
    private FactoryImpl factory1;
    private FactoryImpl factory2;

    /** Snapshots depend on workspace. */
    private SnapshotImpl snapshot1;
    private SnapshotImpl snapshot2;
    private SnapshotImpl snapshot3;
    private SnapshotImpl snapshot4;


    /** Recipe depend on user via permissions */
    private RecipeImpl recipe1;
    private RecipeImpl recipe2;

    /** Stack depend on user via permissions */
    private StackImpl stack1;
    private StackImpl stack2;
    private StackImpl stack3;

    /** Organization depends on user via permissions */
    private OrganizationImpl organization;
    private OrganizationImpl childOrganization;
    private OrganizationImpl organization2;

    /** Free resources limit depends on user via personal account */
    private FreeResourcesLimitImpl freeResourcesLimit;
    private FreeResourcesLimitImpl freeResourcesLimit2;

    @BeforeMethod
    public void setUp() throws Exception {
        injector = Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                install(new JpaPersistModule("main"));
                bind(EventService.class).in(Singleton.class);
                bind(SchemaInitializer.class).toInstance(new FlywaySchemaInitializer(inMemoryDefault(),
                                                                                     "che-schema",
                                                                                     "codenvy-schema"));
                bind(DBInitializer.class).asEagerSingleton();
                bind(TckResourcesCleaner.class).to(H2JpaCleaner.class);
                install(new InitModule(PostConstruct.class));
                install(new UserJpaModule());
                install(new AccountModule());
                install(new SshJpaModule());
                install(new WorkspaceJpaModule());
                install(new MachineJpaModule());
                install(new FactoryJpaModule());
                install(new OrganizationJpaModule());
                install(new OnPremisesJpaWorkspaceModule());
                install(new OnPremisesJpaMachineModule());

                bind(FreeResourcesLimitDao.class).to(JpaFreeResourcesLimitDao.class);
                bind(RemoveFreeResourcesLimitSubscriber.class).asEagerSingleton();
                bind(WorkspaceManager.class);
                final WorkspaceRuntimes wR = Mockito.mock(WorkspaceRuntimes.class);
                when(wR.hasRuntime(Mockito.anyString())).thenReturn(false);
                bind(WorkspaceRuntimes.class).toInstance(wR);
                bind(AccountManager.class);
                bind(Boolean.class).annotatedWith(Names.named("che.workspace.auto_snapshot")).toInstance(false);
                bind(Boolean.class).annotatedWith(Names.named("che.workspace.auto_restore")).toInstance(false);
                bind(WorkspaceSharedPool.class).toInstance(new WorkspaceSharedPool("cached", null, null));

                bind(String[].class).annotatedWith(Names.named("che.auth.reserved_user_names")).toInstance(new String[0]);
                bind(RemoveOrganizationOnLastUserRemovedEventSubscriber.class).asEagerSingleton();
                bind(RemoveOrganizationDistributedResourcesSubscriber.class).asEagerSingleton();
                Multibinder.newSetBinder(binder(), ResourceLockKeyProvider.class);
                Multibinder.newSetBinder(binder(), ResourceUsageTracker.class);
                Multibinder.newSetBinder(binder(), ResourcesReserveTracker.class);
                Multibinder.newSetBinder(binder(), ResourcesProvider.class)
                           .addBinding().to(SuborganizationResourcesProvider.class);
                Multibinder.newSetBinder(binder(), ResourceType.class)
                           .addBinding().to(RamResourceType.class);
            }
        });

        eventService = injector.getInstance(EventService.class);
        accountDao = injector.getInstance(AccountDao.class);
        userDao = injector.getInstance(UserDao.class);
        preferenceDao = injector.getInstance(PreferenceDao.class);
        profileDao = injector.getInstance(ProfileDao.class);
        sshDao = injector.getInstance(SshDao.class);
        snapshotDao = injector.getInstance(SnapshotDao.class);
        workspaceDao = injector.getInstance(WorkspaceDao.class);
        factoryDao = injector.getInstance(FactoryDao.class);
        stackDao = injector.getInstance(StackDao.class);
        recipeDao = injector.getInstance(RecipeDao.class);
        workerDao = injector.getInstance(WorkerDao.class);
        freeResourcesLimitDao = injector.getInstance(FreeResourcesLimitDao.class);
        organizationManager = injector.getInstance(OrganizationManager.class);
        organizationDao = injector.getInstance(OrganizationDao.class);
        memberDao = injector.getInstance(MemberDao.class);
        organizationResourcesDao = injector.getInstance(OrganizationResourcesDao.class);

        TypeLiteral<Set<PermissionsDao<? extends AbstractPermissions>>> lit =
                new TypeLiteral<Set<PermissionsDao<? extends AbstractPermissions>>>() {
                };
        Key<Set<PermissionsDao<? extends AbstractPermissions>>> key = Key.get(lit);
        for (PermissionsDao<? extends AbstractPermissions> dao : injector.getInstance(key)) {
            if (dao.getDomain().getId().equals(RecipeDomain.DOMAIN_ID)) {
                recipePermissionsDao = (JpaRecipePermissionsDao)dao;
            } else if (dao.getDomain().getId().equals(StackDomain.DOMAIN_ID)) {
                stackPermissionsDao = (JpaStackPermissionsDao)dao;
            }
        }
    }

    @AfterMethod
    public void cleanup() {
        injector.getInstance(EntityManagerFactory.class).close();
    }

    @Test
    public void shouldDeleteAllTheEntitiesWhenUserIsDeleted() throws Exception {
        createTestData();

        // Remove the user, all entries must be removed along with the user
        accountDao.remove(account.getId());
        userDao.remove(user.getId());
        userDao.remove(user2.getId());

        // Check all the entities are removed
        assertNull(notFoundToNull(() -> userDao.getById(user.getId())));
        assertNull(notFoundToNull(() -> profileDao.getById(user.getId())));
        assertTrue(preferenceDao.getPreferences(user.getId()).isEmpty());
        assertTrue(sshDao.get(user.getId()).isEmpty());
        assertTrue(workspaceDao.getByNamespace(account.getName()).isEmpty());
        assertTrue(factoryDao.getByAttribute(0, 0, singletonList(Pair.of("creator.userId", user.getId()))).isEmpty());
        assertTrue(snapshotDao.findSnapshots(workspace1.getId()).isEmpty());
        assertTrue(snapshotDao.findSnapshots(workspace2.getId()).isEmpty());
        //Check workers and parent entity is removed
        assertTrue(workspaceDao.getByNamespace(user2.getId()).isEmpty());
        assertEquals(workerDao.getWorkers(workspace3.getId(), 1, 0).getTotalItemsCount(), 0);
        // Check stack and recipes are removed
        assertNull(notFoundToNull(() -> recipeDao.getById(recipe1.getId())));
        assertNull(notFoundToNull(() -> recipeDao.getById(recipe2.getId())));
        assertNull(notFoundToNull(() -> stackDao.getById(stack1.getId())));
        assertNull(notFoundToNull(() -> stackDao.getById(stack2.getId())));
        // Permissions are removed
        assertTrue(recipePermissionsDao.getByUser(user2.getId()).isEmpty());
        assertTrue(stackPermissionsDao.getByUser(user2.getId()).isEmpty());
        // Non-removed user permissions and stack are present
        assertNotNull(notFoundToNull(() -> stackDao.getById(stack3.getId())));
        assertFalse(stackPermissionsDao.getByUser(user3.getId()).isEmpty());
        // Check existence of organizations
        assertNull(notFoundToNull(() -> organizationManager.getById(organization.getId())));
        assertEquals(memberDao.getMembers(organization.getId(), 1, 0).getTotalItemsCount(), 0);
        // Check workspace is removed along with organization account
        assertNull(notFoundToNull(() -> workspaceDao.get(workspace4.getId())));

        assertNull(notFoundToNull(() -> organizationManager.getById(childOrganization.getId())));
        assertEquals(memberDao.getMembers(childOrganization.getId(), 1, 0).getTotalItemsCount(), 0);

        assertNotNull(notFoundToNull(() -> organizationManager.getById(organization2.getId())));
        assertEquals(memberDao.getMembers(organization2.getId(), 1, 0).getTotalItemsCount(), 1);

        // free resources limit is removed
        assertNull(notFoundToNull(() -> freeResourcesLimitDao.get(user.getId())));
        assertNull(notFoundToNull(() -> freeResourcesLimitDao.get(user2.getId())));

        // distributed resources is removed
        assertNull(notFoundToNull(() -> organizationResourcesDao.get(childOrganization.getId())));

        //cleanup
        stackDao.remove(stack3.getId());
        memberDao.remove(organization2.getId(), user3.getId());
        organizationManager.remove(organization2.getId());
        userDao.remove(user3.getId());
    }

    @Test(dataProvider = "beforeRemoveRollbackActions")
    public void shouldRollbackTransactionWhenFailedToRemoveAnyOfEntries(
            Class<CascadeEventSubscriber<CascadeEvent>> subscriberClass,
            Class<CascadeEvent> eventClass) throws Exception {
        createTestData();
        eventService.unsubscribe(injector.getInstance(subscriberClass), eventClass);

        // Remove the user, all entries must be rolled back after fail
        try {
            userDao.remove(user2.getId());
            fail("UserDao#remove had to throw exception");
        } catch (Exception ignored) {
        }

        // Check all the data rolled back
        assertNotNull(userDao.getById(user2.getId()));
        assertFalse(recipePermissionsDao.getByUser(user2.getId()).isEmpty());
        assertNotNull(notFoundToNull(() -> recipeDao.getById(recipe1.getId())));
        assertFalse(stackPermissionsDao.getByUser(user2.getId()).isEmpty());
        assertNotNull(notFoundToNull(() -> recipeDao.getById(recipe2.getId())));
        assertNotNull(notFoundToNull(() -> stackDao.getById(stack1.getId())));
        assertNotNull(notFoundToNull(() -> stackDao.getById(stack2.getId())));
        assertNotNull(notFoundToNull(() -> freeResourcesLimitDao.get(freeResourcesLimit.getAccountId())));
        assertNotNull(notFoundToNull(() -> freeResourcesLimitDao.get(freeResourcesLimit.getAccountId())));
        assertNotNull(notFoundToNull(() -> organizationManager.getById(organization.getId())));
        assertNotNull(notFoundToNull(() -> organizationManager.getById(childOrganization.getId())));
        assertNotNull(notFoundToNull(() -> organizationManager.getById(organization2.getId())));
        assertNotNull(notFoundToNull(() -> organizationResourcesDao.get(childOrganization.getId())));
        wipeTestData();
    }

    @DataProvider(name = "beforeRemoveRollbackActions")
    public Object[][] beforeRemoveActions() {
        return new Class[][] {
                {RemoveStackOnLastUserRemovedEventSubscriber.class, BeforeUserRemovedEvent.class},
                {RemoveOrganizationOnLastUserRemovedEventSubscriber.class, BeforeUserRemovedEvent.class},
                {RemoveOrganizationDistributedResourcesSubscriber.class, BeforeOrganizationRemovedEvent.class},
                };
    }

    private void createTestData() throws ConflictException, ServerException {
        userDao.create(user = createUser("bobby"));
        accountDao.create(account = createAccount("bobby"));
        // test permissions users
        userDao.create(user2 = createUser("worker"));
        userDao.create(user3 = createUser("stacker"));

        profileDao.create(profile = createProfile(user.getId()));

        preferenceDao.setPreferences(user.getId(), preferences = createPreferences());

        // dao is using instead of manager to avoid automatic creation of members
        organizationDao.create(organization = new OrganizationImpl("org123", "testOrg", null));
        organizationDao.create(childOrganization = new OrganizationImpl("suborg123", "childTestOrg", organization.getId()));
        organizationDao.create(organization2 = new OrganizationImpl("org321", "anotherOrg", null));

        workspaceDao.create(workspace1 = createWorkspace("workspace1", account));
        workspaceDao.create(workspace2 = createWorkspace("workspace2", account));
        workspaceDao.create(workspace3 = createWorkspace("workspace3", account));
        workspaceDao.create(workspace4 = createWorkspace("workspace4", organization.getAccount()));

        sshDao.create(sshPair1 = createSshPair(user.getId(), "service", "name1"));
        sshDao.create(sshPair2 = createSshPair(user.getId(), "service", "name2"));

        factoryDao.create(factory1 = createFactory("factory1", user.getId()));
        factoryDao.create(factory2 = createFactory("factory2", user.getId()));

        snapshotDao.saveSnapshot(snapshot1 = createSnapshot("snapshot1", workspace1.getId()));
        snapshotDao.saveSnapshot(snapshot2 = createSnapshot("snapshot2", workspace1.getId()));
        snapshotDao.saveSnapshot(snapshot3 = createSnapshot("snapshot3", workspace2.getId()));
        snapshotDao.saveSnapshot(snapshot4 = createSnapshot("snapshot4", workspace2.getId()));

        recipeDao.create(recipe1 = createRecipe("recipe1"));
        recipeDao.create(recipe2 = createRecipe("recipe2"));

        stackDao.create(stack1 = createStack("stack1", "st1"));
        stackDao.create(stack2 = createStack("stack2", "st2"));
        stackDao.create(stack3 = createStack("stack3", "st3"));

        workerDao.store(createWorker(user2.getId(), workspace3.getId()));

        stackPermissionsDao.store(new StackPermissionsImpl(user2.getId(), stack1.getId(), asList(SET_PERMISSIONS, "read", "write")));
        stackPermissionsDao.store(new StackPermissionsImpl(user2.getId(),
                                                           stack2.getId(),
                                                           asList(SET_PERMISSIONS, "read", "execute")));
        // To test removal only permissions if more users with setPermissions are present
        stackPermissionsDao.store(new StackPermissionsImpl(user2.getId(),
                                                           stack3.getId(),
                                                           asList(SET_PERMISSIONS, "read", "write")));
        stackPermissionsDao.store(new StackPermissionsImpl(user3.getId(),
                                                           stack3.getId(),
                                                           asList(SET_PERMISSIONS, "read", "write", "execute")));

        recipePermissionsDao.store(new RecipePermissionsImpl(user2.getId(), recipe1.getId(), asList(SET_PERMISSIONS, "read", "write")));
        recipePermissionsDao.store(new RecipePermissionsImpl(user2.getId(), recipe2.getId(), asList(SET_PERMISSIONS, "read", "execute")));

        memberDao.store(new MemberImpl(user.getId(), organization.getId(), singletonList(OrganizationDomain.SET_PERMISSIONS)));

        memberDao.store(new MemberImpl(user.getId(), organization2.getId(), singletonList(OrganizationDomain.SET_PERMISSIONS)));
        memberDao.store(new MemberImpl(user2.getId(), organization2.getId(), singletonList(OrganizationDomain.SET_PERMISSIONS)));
        memberDao.store(new MemberImpl(user2.getId(), childOrganization.getId(), singletonList(OrganizationDomain.SET_PERMISSIONS)));
        memberDao.store(new MemberImpl(user3.getId(), organization2.getId(), singletonList(OrganizationDomain.SET_PERMISSIONS)));


        freeResourcesLimitDao.store(freeResourcesLimit = createFreeResourcesLimit(account.getId()));
        freeResourcesLimitDao.store(freeResourcesLimit2 = createFreeResourcesLimit(organization.getAccount().getId()));

        organizationResourcesDao.store(new OrganizationResourcesImpl(childOrganization.getId(),
                                                                     singletonList(new ResourceImpl(RamResourceType.ID,
                                                                                                    1024,
                                                                                                    RamResourceType.UNIT)),
                                                                     singletonList(new ResourceImpl(RamResourceType.ID,
                                                                                                    1024,
                                                                                                    RamResourceType.UNIT))));
    }

    private void wipeTestData() throws ConflictException, ServerException, NotFoundException {
        organizationResourcesDao.remove(childOrganization.getId());

        freeResourcesLimitDao.remove(freeResourcesLimit.getAccountId());
        freeResourcesLimitDao.remove(freeResourcesLimit2.getAccountId());

        memberDao.remove(organization.getId(), user.getId());
        memberDao.remove(childOrganization.getId(), user.getId());
        memberDao.remove(organization2.getId(), user.getId());
        memberDao.remove(organization2.getId(), user2.getId());
        memberDao.remove(organization2.getId(), user3.getId());

        organizationManager.remove(childOrganization.getId());
        organizationManager.remove(organization.getId());
        organizationManager.remove(organization2.getId());

        snapshotDao.removeSnapshot(snapshot1.getId());
        snapshotDao.removeSnapshot(snapshot2.getId());
        snapshotDao.removeSnapshot(snapshot3.getId());
        snapshotDao.removeSnapshot(snapshot4.getId());

        stackPermissionsDao.remove(user2.getId(), stack1.getId());
        stackPermissionsDao.remove(user2.getId(), stack2.getId());
        stackPermissionsDao.remove(user2.getId(), stack3.getId());
        stackPermissionsDao.remove(user3.getId(), stack3.getId());

        recipePermissionsDao.remove(user2.getId(), recipe1.getId());
        recipePermissionsDao.remove(user2.getId(), recipe2.getId());

        recipeDao.remove(recipe1.getId());
        recipeDao.remove(recipe2.getId());

        stackDao.remove(stack1.getId());
        stackDao.remove(stack2.getId());
        stackDao.remove(stack3.getId());

        workerDao.removeWorker(workspace3.getId(), user2.getId());


        factoryDao.remove(factory1.getId());
        factoryDao.remove(factory2.getId());

        sshDao.remove(sshPair1.getOwner(), sshPair1.getService(), sshPair1.getName());
        sshDao.remove(sshPair2.getOwner(), sshPair2.getService(), sshPair2.getName());

        workspaceDao.remove(workspace1.getId());
        workspaceDao.remove(workspace2.getId());
        workspaceDao.remove(workspace3.getId());
        workspaceDao.remove(workspace4.getId());

        preferenceDao.remove(user3.getId());
        preferenceDao.remove(user2.getId());
        preferenceDao.remove(user.getId());

        profileDao.remove(user3.getId());
        profileDao.remove(user2.getId());
        profileDao.remove(user.getId());

        userDao.remove(user3.getId());
        userDao.remove(user2.getId());
        userDao.remove(user.getId());

        accountDao.remove(account.getId());
    }

    private static <T> T notFoundToNull(Callable<T> action) throws Exception {
        try {
            return action.call();
        } catch (NotFoundException x) {
            return null;
        }
    }
}
