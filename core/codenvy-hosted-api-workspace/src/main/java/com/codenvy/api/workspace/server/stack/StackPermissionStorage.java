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
package com.codenvy.api.workspace.server.stack;

import com.codenvy.api.permission.server.PermissionsDomain;
import com.codenvy.api.permission.server.PermissionsImpl;
import com.codenvy.api.permission.server.dao.PermissionsStorage;
import com.codenvy.api.permission.shared.Permissions;
import com.codenvy.api.workspace.server.WorkspaceDomain;
import com.codenvy.api.workspace.server.dao.WorkerDao;
import com.codenvy.api.workspace.server.model.Worker;
import com.google.common.collect.ImmutableSet;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.model.AclImpl;
import org.eclipse.che.api.workspace.server.spi.StackDao;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link PermissionsStorage} for storing permissions of {@link WorkspaceDomain}
 * <p>
 * <p>This implementation adapts {@link Permissions} and {@link Worker} and use
 * {@link WorkerDao} as storage of permissions
 *
 * @author Sergii Leschenko
 */
@Singleton
public class StackPermissionStorage implements PermissionsStorage {
    private final StackDao stackDao;

    @Inject
    public StackPermissionStorage(StackDao stackDao) throws IOException {
        this.stackDao = stackDao;
    }

    @Override
    public Set<PermissionsDomain> getDomains() {
        return ImmutableSet.of(new WorkspaceDomain());
    }

    @Override
    public void store(PermissionsImpl permissions) throws ServerException {
        stackDao.storeACL(permissions.getInstance(), new AclImpl(permissions.getUser(),
                                                                 permissions.getActions()));
    }

    @Override
    public List<PermissionsImpl> get(String user) throws ServerException {
        return Collections.emptyList();
//        return toPermissions(stackDao.getACL(user));
    }

    @Override
    public List<PermissionsImpl> get(String user, String domain) throws ServerException {
        return Collections.emptyList();
//        return toPermissions(stackDao.getACL(user));
    }

    @Override
    public PermissionsImpl get(String user, String domain, String instance) throws ServerException, NotFoundException {
        return toPermission(instance, stackDao.getACL(instance, user));
    }

    @Override
    public List<PermissionsImpl> getByInstance(String domain, String instance) throws ServerException {
        return toPermissions(instance, stackDao.getACLs(instance));
    }

    @Override
    public boolean exists(String user, String domain, String instance, String action) throws ServerException {
        try {
            return stackDao.getACL(instance, user)
                           .getActions()
                           .stream()
                           .filter(actualAction -> actualAction.equals(action))
                           .findAny()
                           .isPresent();
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public void remove(String user, String domain, String instance) throws ServerException, ConflictException {
        stackDao.removeACL(instance, user);
    }

    private List<PermissionsImpl> toPermissions(String stack, List<AclImpl> acls) {
        return acls.stream()
                   .map(acl -> toPermission(stack, acl))
                   .collect(Collectors.toList());
    }

    private PermissionsImpl toPermission(String stack, AclImpl acl) {
        return new PermissionsImpl(acl.getUser(),
                                   StackDomain.DOMAIN_ID,
                                   stack,
                                   acl.getActions());
    }
}
