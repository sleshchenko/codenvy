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
package com.codenvy.api.workspace;

import com.codenvy.api.workspace.LimitsCheckingWorkspaceManager.WorkspaceCallback;
import com.google.common.collect.ImmutableList;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.environment.server.EnvironmentParser;
import org.eclipse.che.api.environment.server.compose.ComposeFileParser;
import org.eclipse.che.api.machine.server.spi.SnapshotDao;
import org.eclipse.che.api.machine.server.util.RecipeDownloader;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.codenvy.api.workspace.TestObjects.createConfig;
import static java.util.Collections.emptyList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LimitsCheckingWorkspaceManager}.
 *
 * @author Yevhenii Voevodin
 * @author Alexander Garagatyi
 */
@Listeners(MockitoTestNGListener.class)
public class LimitsCheckingWorkspaceManagerTest {
    @Mock
    SnapshotDao snapshotDao;

    @Mock
    RecipeDownloader recipeDownloader;

    @Mock
    AccountManager accountManager;

    ComposeFileParser composeFileParser = new ComposeFileParser();

    EnvironmentParser environmentParser = new EnvironmentParser(composeFileParser, recipeDownloader);

    @Test(expectedExceptions = LimitExceededException.class,
          expectedExceptionsMessageRegExp = "The maximum workspaces allowed per user is set to '2' and you are currently at that limit. " +
                                            "This value is set by your admin with the 'limits.user.workspaces.count' property")
    public void shouldNotBeAbleToCreateNewWorkspaceIfLimitIsExceeded() throws Exception {
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2, // <- workspaces max count
                                                                                              "1gb",
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000));
        doReturn(ImmutableList.of(mock(WorkspaceImpl.class), mock(WorkspaceImpl.class))) // <- currently used 2
                .when(manager)
                .getByNamespace(anyString());

        manager.checkCountAndPropagateCreation("user123", null);
    }

    @Test
    public void shouldNotCheckAllowedWorkspacesPerUserWhenItIsSetToMinusOne() throws Exception {
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(-1, // <- workspaces max count
                                                                                              "1gb",
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000));
        doReturn(ImmutableList.of(mock(WorkspaceImpl.class), mock(WorkspaceImpl.class))) // <- currently used 2
                .when(manager)
                .getByNamespace(anyString());
        final WorkspaceCallback callback = mock(WorkspaceCallback.class);

        manager.checkCountAndPropagateCreation("user123", callback);

        verify(callback).call();
        verify(manager, never()).getWorkspaces(any());
    }


    @Test
    public void shouldCallCreateCallBackIfEverythingIsOkayWithLimits() throws Exception {
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2, // <- workspaces max count
                                                                                              "1gb",
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000));
        doReturn(emptyList()).when(manager).getByNamespace(anyString()); // <- currently used 0

        final WorkspaceCallback callback = mock(WorkspaceCallback.class);
        manager.checkCountAndPropagateCreation("user123", callback);

        verify(callback).call();
    }

    @Test(expectedExceptions = LimitExceededException.class,
          expectedExceptionsMessageRegExp = "The maximum RAM per workspace is set to '2048mb' and you requested '3072mb'. " +
                                            "This value is set by your admin with the 'limits.workspace.env.ram' property")
    public void shouldNotBeAbleToCreateWorkspaceWhichExceedsRamLimit() throws Exception {
        final WorkspaceConfig config = createConfig("3gb");
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "2gb", // <- workspaces env ram limit
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000));

        manager.checkMaxEnvironmentRam(config);
    }

    @Test
    public void shouldNotCheckWorkspaceRamLimitIfItIsSetToMinusOne() throws Exception {
        final WorkspaceConfig config = createConfig("3gb");
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "-1", // <- workspaces env ram limit
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000));

        manager.checkMaxEnvironmentRam(config);
    }

    @Test(expectedExceptions = LimitExceededException.class,
          expectedExceptionsMessageRegExp = "The maximum RAM per workspace is set to '2048mb' and you requested '2304mb'. " +
                                            "This value is set by your admin with the 'limits.workspace.env.ram' property")
    public void shouldNotBeAbleToCreateWorkspaceWithMultipleMachinesWhichExceedsRamLimit() throws Exception {
        final WorkspaceConfig config = createConfig("1gb", "1gb", "256mb");

        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "2gb", // <- workspaces env ram limit
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000));
        manager.checkMaxEnvironmentRam(config);
    }

    @Test
    public void shouldBeAbleToCreateWorkspaceWithMultipleMachinesWhichDoesNotExceedRamLimit() throws Exception {
        final WorkspaceConfig config = createConfig("1gb", "1gb", "256mb");

        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb", // <- workspaces env ram limit
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000));
        manager.checkMaxEnvironmentRam(config);
    }

    @Test
    public void shouldBeAbleToCreateWorkspaceWithMultipleMachinesIncludingMachineWithoutLimitsWhichDoesNotExceedRamLimit()
            throws Exception {
        final WorkspaceConfig config = createConfig("256mb", "256mb", null);

        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb", // <- workspaces env ram limit
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000)); // <- default limit for machines without set limit
        manager.checkMaxEnvironmentRam(config);
    }
}
