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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.environment.server.EnvironmentParser;
import org.eclipse.che.api.environment.server.compose.model.ComposeEnvironmentImpl;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;

/**
 * Intercepts {@link WorkspaceManager#startWorkspace(String, String, Boolean)}
 * and {@link WorkspaceManager#startWorkspace(WorkspaceConfig, String, boolean)}
 * and reserves RAM resource while workspace is starting
 *
 * @author Sergii Leschenko
 */
public class WorkspaceRamConsumer implements MethodInterceptor {
    @Inject
    private ResourcesManager resourcesManager;

    @Inject
    private WorkspaceManager workspaceManager;

    @Inject
    private AccountManager accountManager;

    @Inject
    private EnvironmentParser environmentParser;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final Object[] arguments = invocation.getArguments();

        WorkspaceConfigImpl config;
        String envName = null;
        String namespace;

        if (arguments[0] instanceof String) {
            //start existed workspace
            String workspaceId = (String)arguments[0];
            WorkspaceImpl workspace = workspaceManager.getWorkspace(workspaceId);
            namespace = workspace.getNamespace();
            config = workspace.getConfig();
            envName = (String)arguments[1];
        } else {
            //start from config with default env
            config = (WorkspaceConfigImpl)arguments[0];
            namespace = (String)arguments[1];
        }

        Optional<? extends Environment> envOptional = findEnv(config.getEnvironments(), envName);
        if (!envOptional.isPresent()) {
            envOptional = findEnv(config.getEnvironments(), config.getDefaultEnv());
        }

        final RamResource ramToUse = new RamResource(sumRam(envOptional.get()));
        final Account account = accountManager.getByName(namespace);
        return resourcesManager.reserveResources(account.getId(), singletonList(ramToUse), invocation::proceed);
    }

    private Optional<? extends Environment> findEnv(List<? extends Environment> environments, String envName) {
        return environments.stream()
                           .filter(env -> env.getName().equals(envName))
                           .findFirst();
    }

    /**
     * Parses (and fetches if needed) recipe of environment and sums RAM size of all machines in environment in megabytes.
     */
    private long sumRam(Environment environment) throws ServerException {
        ComposeEnvironmentImpl composeEnv = environmentParser.parse(environment);

        long sumBytes = composeEnv.getServices()
                                  .values()
                                  .stream()
                                  .mapToLong(value -> {
                                      if (value.getMemLimit() == null || value.getMemLimit() == 0) {
                                          return defaultMachineMemorySizeBytes;
                                      } else {
                                          return value.getMemLimit();
                                      }
                                  })
                                  .sum();
        return sumBytes / BYTES_TO_MEGABYTES_DIVIDER;
    }
    //TODO Fix it
    private long sumRam(List<? extends MachineConfig> machineConfigs) {
        return machineConfigs.stream()
                             .mapToInt(m -> m.getLimits().getRam())
                             .sum();
    }
}
