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
package com.codenvy.api.workspace.server;

import com.codenvy.api.permission.server.PermissionsDomain;
import com.codenvy.api.permission.server.PermissionsImpl;
import com.codenvy.api.permission.server.dao.PermissionsStorage;
import com.codenvy.api.permission.shared.Permissions;
import com.codenvy.api.workspace.server.dao.WorkerDao;
import com.codenvy.api.workspace.server.model.Worker;
import com.codenvy.api.workspace.server.model.WorkerImpl;
import com.google.common.collect.ImmutableSet;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link PermissionsStorage} for storing permissions of {@link WorkspaceDomain}
 *
 * <p>This implementation adapts {@link Permissions} and {@link Worker} and use
 * {@link WorkerDao} as storage of permissions
 *
 * @author Sergii Leschenko
 */
@Singleton
public class WorkspacePermissionStorage implements PermissionsStorage {
    private final WorkerDao workerDao;

    @Inject
    public WorkspacePermissionStorage(WorkerDao workerDao) {
        this.workerDao = workerDao;
    }

    @Override
    public Set<PermissionsDomain> getDomains() {
        return ImmutableSet.of(new WorkspaceDomain());
    }

    @Override
    public void store(PermissionsImpl permissions) throws ServerException {
        workerDao.store(new WorkerImpl(permissions.getUser(),
                                       permissions.getInstance(),
                                       permissions.getActions()
                                                  .stream()
                                                  .map(WorkspaceAction::getAction)
                                                  .collect(Collectors.toList())));
    }

    @Override
    public PermissionsImpl get(String user, String domain, String instance) throws ServerException, NotFoundException {
        return toPermission(workerDao.getWorker(instance, user));
    }

    @Override
    public List<PermissionsImpl> getByInstance(String domain, String instance) throws ServerException {
        return toPermissions(workerDao.getWorkers(instance));
    }

    @Override
    public boolean exists(String user, String domain, String instance, String action) throws ServerException {
        try {
            return workerDao.getWorker(instance, user).getActions()
                            .stream()
                            .filter(actualAction -> actualAction.toString().equals(action))
                            .findAny()
                            .isPresent();
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public void remove(String user, String domain, String instance) throws ServerException {
        workerDao.removeWorker(instance, user);
    }

    private PermissionsImpl toPermission(WorkerImpl worker) {
        return new PermissionsImpl(worker.getUser(),
                                   WorkspaceDomain.DOMAIN_ID,
                                   worker.getWorkspace(),
                                   worker.getActions()
                                         .stream()
                                         .map(WorkspaceAction::toString)
                                         .collect(Collectors.toList()));
    }

    private List<PermissionsImpl> toPermissions(List<WorkerImpl> workers) {
        return workers.stream()
                      .map(this::toPermission)
                      .collect(Collectors.toList());
    }
}
