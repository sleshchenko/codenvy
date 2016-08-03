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

import com.codenvy.resources.api.ResourcesManager;

import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.spi.AccountImpl;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.machine.server.model.impl.LimitsImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineConfigImpl;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WorkspaceRamConsumer}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class WorkspaceRamConsumerTest {
    @Mock
    private ResourcesManager resourcesManager;
    @Mock
    private WorkspaceManager workspaceManager;
    @Mock
    private AccountManager   accountManager;
    @Mock
    private MethodInvocation invocation;
    @Mock
    private AccountImpl      account;

    @InjectMocks
    private WorkspaceRamConsumer ramConsumer;

    @Test
    public void shouldReserveRamOnStartingWorkspaceById() throws Throwable {
        when(invocation.getArguments()).thenReturn(new Object[] {"workspace123", "dev-env", false});
        final WorkspaceImpl testWorkspace = createWorkspace("testAccount", "dev-env", "default", 700, 300);
        when(workspaceManager.getWorkspace(any())).thenReturn(testWorkspace);
        when(accountManager.getByName(any())).thenReturn(account);
        when(account.getId()).thenReturn("account123");

        ramConsumer.invoke(invocation);

        verify(workspaceManager).getWorkspace(eq("workspace123"));
        verify(accountManager).getByName(eq("testAccount"));
        verify(resourcesManager).reserveResources(eq("account123"), eq(Collections.singletonList(new RamResource(1000))), any());
    }

    @Test
    public void shouldReserveRamFromDefaultEvnIfItIsNotSpecifiedOnStartingWorkspaceById() throws Throwable {
        when(invocation.getArguments()).thenReturn(new Object[] {"workspace123", null, false});
        final WorkspaceImpl testWorkspace = createWorkspace("testAccount", "default", 700, 300);
        when(workspaceManager.getWorkspace(any())).thenReturn(testWorkspace);
        when(accountManager.getByName(any())).thenReturn(account);
        when(account.getId()).thenReturn("account123");

        ramConsumer.invoke(invocation);

        verify(workspaceManager).getWorkspace(eq("workspace123"));
        verify(accountManager).getByName(eq("testAccount"));
        verify(resourcesManager).reserveResources(eq("account123"), eq(Collections.singletonList(new RamResource(1000))), any());
    }

    @Test
    public void shouldReserveRamFromDefaultEvnOnStartingWorkspaceFromConfig() throws Throwable {
        final WorkspaceConfigImpl config = createConfig("default", "default", 700, 300);
        when(invocation.getArguments()).thenReturn(new Object[] {config, "testAccount"});
        when(accountManager.getByName(any())).thenReturn(account);
        when(account.getId()).thenReturn("account123");

        ramConsumer.invoke(invocation);

        verify(accountManager).getByName(eq("testAccount"));
        verify(resourcesManager).reserveResources(eq("account123"), eq(Collections.singletonList(new RamResource(1000))), any());
    }

    @Test(dataProvider = "interceptedMethods")
    public void shouldTestRequiredMethodsExistence(Object[] methodDescriptor) throws Exception {
        //
        final Method method = getServiceMethod(methodDescriptor);
        when(invocation.getMethod()).thenReturn(method);
        // preparing invocation arguments which are actually the same size as method parameters
        // settings the last argument value to fake account identifier
        final Object[] invocationArgs = new Object[method.getParameterCount()];
        invocationArgs[invocationArgs.length - 1] = "not default account id";
        when(invocation.getArguments()).thenReturn(invocationArgs);
    }

    @DataProvider(name = "interceptedMethods")
    private Object[][] interceptedMethodsProvider() {
        return new Object[][] {
                {new Object[] {"startWorkspace", String.class, String.class, Boolean.class}},
                {new Object[] {"startWorkspace", WorkspaceConfig.class, String.class, boolean.class}}
        };
    }

    /**
     * Gets a {@link WorkspaceManager} method based on data provided by {@link #interceptedMethodsProvider()
     */
    private Method getServiceMethod(Object[] methodDescription) throws NoSuchMethodException {
        final Class<?>[] methodParams = new Class<?>[methodDescription.length - 1];
        for (int i = 1; i < methodDescription.length; i++) {
            methodParams[i - 1] = (Class<?>)methodDescription[i];
        }
        return WorkspaceManager.class.getMethod(methodDescription[0].toString(), methodParams);
    }

    public static WorkspaceImpl createWorkspace(String namespace, String envName, Integer... machineRams) {
        return createWorkspace(namespace, envName, envName, machineRams);
    }

    /** Creates users workspace object based on the owner and machines RAM. */
    public static WorkspaceImpl createWorkspace(String namespace, String envName, String defaultEnvName, Integer... machineRams) {
        return WorkspaceImpl.builder()
                            .setConfig(createConfig(envName, defaultEnvName, machineRams))
                            .setNamespace(namespace)
                            .build();
    }

    public static WorkspaceConfigImpl createConfig(String envName, String defaultEnvName, Integer... machineRams) {
        final List<MachineConfigImpl> machineConfigs = new ArrayList<>(1 + machineRams.length);
        for (Integer machineRam : machineRams) {
            machineConfigs.add(createMachineConfig(machineRam));
        }
        return WorkspaceConfigImpl.builder()
                                  .setEnvironments(singletonList(new EnvironmentImpl(envName,
                                                                                     null,
                                                                                     machineConfigs)))
                                  .setDefaultEnv(defaultEnvName)
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
