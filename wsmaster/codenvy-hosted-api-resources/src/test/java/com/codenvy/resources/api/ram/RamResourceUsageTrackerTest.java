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
package com.codenvy.resources.api.ram;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.machine.server.model.impl.LimitsImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineConfigImpl;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceRuntimeImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link RamResourceUsageTracker}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class RamResourceUsageTrackerTest {
    @Mock
    private Account          account;
    @Mock
    private WorkspaceManager workspaceManager;
    @Mock
    private AccountManager   accountManager;

    @InjectMocks
    private RamResourceUsageTracker ramUsageTracker;

    @Test(expectedExceptions = NotFoundException.class,
          expectedExceptionsMessageRegExp = "Account was not found")
    public void shouldThrowNotFoundExceptionWhenAccountDoesNotExistOnGettingUsedRam() throws Exception {
        when(accountManager.getById(any())).thenThrow(new NotFoundException("Account was not found"));

        ramUsageTracker.getUsedResource("account123");
    }

    @Test
    public void shouldReturnUsedRamForGivenAccount() throws Exception {
        when(accountManager.getById(any())).thenReturn(account);
        when(account.getName()).thenReturn("testAccount");

        when(workspaceManager.getByNamespace(anyString()))
                .thenReturn(singletonList(createWorkspace(WorkspaceStatus.RUNNING, 1000, 500, 500)));

        RamResource usedRam = ramUsageTracker.getUsedResource("account123");

        assertEquals(usedRam.getAmount(), 2000L);
        verify(accountManager).getById(eq("account123"));
        verify(workspaceManager).getByNamespace(eq("testAccount"));
    }

    @Test
    public void shouldNotSumRamOfStoppedWorkspaceWhenGettingUsedRamForGivenAccount() throws Exception {
        when(accountManager.getById(any())).thenReturn(account);
        when(account.getName()).thenReturn("testAccount");

        when(workspaceManager.getByNamespace(anyString()))
                .thenReturn(singletonList(createWorkspace(WorkspaceStatus.STOPPED, 1000, 500, 500)));

        RamResource usedRam = ramUsageTracker.getUsedResource("account123");

        assertEquals(usedRam.getAmount(), 0L);
        verify(accountManager).getById(eq("account123"));
        verify(workspaceManager).getByNamespace(eq("testAccount"));
    }

    /** Creates users workspace object based on the owner and machines RAM. */
    public static WorkspaceImpl createWorkspace(WorkspaceStatus status, Integer... machineRams) {
        final List<MachineConfigImpl> machineConfigs = new ArrayList<>(1 + machineRams.length);
        for (Integer machineRam : machineRams) {
            machineConfigs.add(createMachineConfig(machineRam));
        }
        return WorkspaceImpl.builder()
                            .setConfig(WorkspaceConfigImpl.builder()
                                                          .setEnvironments(singletonList(new EnvironmentImpl("dev-env",
                                                                                                             null,
                                                                                                             machineConfigs)))
                                                          .setDefaultEnv("dev-env")
                                                          .build())
                            .setRuntime(new WorkspaceRuntimeImpl("dev-env"))
                            .setStatus(status)
                            .build();
    }

    /** Creates machine config object based on ram and dev flag. */
    public static MachineConfigImpl createMachineConfig(int ramLimit) {
        return new MachineConfigImpl(false,
                                     null,
                                     null,
                                     null,
                                     new LimitsImpl(ramLimit),
                                     null,
                                     null);
    }
}
