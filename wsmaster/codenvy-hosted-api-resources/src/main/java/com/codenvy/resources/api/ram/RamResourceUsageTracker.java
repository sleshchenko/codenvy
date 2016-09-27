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

import com.codenvy.resources.api.ResourceUsageTracker;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.STOPPED;

/**
 * Tracks usage of RAM resource
 *
 * @author Sergii Leschenko
 */
@Singleton
public class RamResourceUsageTracker implements ResourceUsageTracker<RamResource> {
    private final WorkspaceManager workspaceManager;
    private final AccountManager   accountManager;

    @Inject
    public RamResourceUsageTracker(WorkspaceManager workspaceManager, AccountManager accountManager) {
        this.workspaceManager = workspaceManager;
        this.accountManager = accountManager;
    }

    @Override
    public RamResource getUsedResource(String accountId) throws NotFoundException, ServerException {
        final Account account = accountManager.getById(accountId);
        final List<WorkspaceImpl> accountWorkspaces = workspaceManager.getByNamespace(account.getName());
        final long currentlyUsedRamMB = accountWorkspaces.stream()
                                                         .filter(ws -> STOPPED != ws.getStatus())
                                                         .map(ws -> ws.getRuntime().getMachines())
                                                         .flatMap(List::stream)
                                                         .mapToInt(machine -> machine.getConfig()
                                                                                     .getLimits()
                                                                                     .getRam())
                                                         .sum();
        return new RamResource(currentlyUsedRamMB);
    }
}
