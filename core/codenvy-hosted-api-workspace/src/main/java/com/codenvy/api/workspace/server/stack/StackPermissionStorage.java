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
import com.google.common.collect.ImmutableSet;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.acl.AclEntry;
import org.eclipse.che.api.core.acl.AclEntryImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackImpl;
import org.eclipse.che.api.workspace.server.spi.StackDao;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link PermissionsStorage} for storing permissions of {@link StackDomain}
 *
 * <p>This implementation adapts {@link Permissions} and {@link AclEntry} and use
 * {@link StackDao} as storage of permissions
 *
 * @author Sergii Leschenko
 */
@Singleton
public class StackPermissionStorage implements PermissionsStorage {
    private final StackDao stackDao;

    @Inject
    public StackPermissionStorage(StackDao stackDao) {
        this.stackDao = stackDao;
    }

    @Override
    public Set<PermissionsDomain> getDomains() {
        return ImmutableSet.of(new StackDomain());
    }

    @Override
    public void store(PermissionsImpl permissions) throws ServerException, NotFoundException {
        final StackImpl stack = stackDao.getById(permissions.getInstance());
        stack.getAcl().removeIf(aclEntry -> aclEntry.getUser().equals(permissions.getUser()));
        stack.getAcl().add(new AclEntryImpl(permissions.getUser(),
                                            permissions.getActions()));
        stackDao.update(stack);
    }

    @Override
    public PermissionsImpl get(String user, String domain, String instance) throws ServerException, NotFoundException {
        final StackImpl recipe = stackDao.getById(instance);
        final Optional<AclEntryImpl> result = recipe.getAcl()
                                                    .stream()
                                                    .filter(aclEntry -> aclEntry.getUser().equals(user))
                                                    .findAny();

        if (!result.isPresent()) {
            throw new NotFoundException(String.format("Permissions for user '%s' and instance '%s' of domain '%s' was not found",
                                                      user, instance, domain));
        }

        return toPermission(instance, result.get());
    }

    @Override
    public List<PermissionsImpl> getByInstance(String domain, String instance) throws ServerException, NotFoundException {
        final StackImpl recipe = stackDao.getById(instance);
        return toPermissions(instance, recipe.getAcl());
    }

    @Override
    public boolean exists(String user, String domain, String instance, String action) throws ServerException {
        StackImpl recipe;
        try {
            recipe = stackDao.getById(instance);
        } catch (NotFoundException e) {
            return false;
        }

        final Set<String> allowedActions = recipe.getAcl()
                                                 .stream()
                                                 .filter(entry -> entry.getUser().equals(user) || entry.getUser().equals("*"))
                                                 .flatMap(entry -> entry.getActions().stream())
                                                 .collect(Collectors.toSet());
        return allowedActions.contains(action);

    }

    @Override
    public void remove(String user, String domain, String instance) throws NotFoundException, ServerException {
        final StackImpl recipe;
        try {
            recipe = stackDao.getById(instance);
        } catch (NotFoundException e) {
            return;
        }
        recipe.getAcl().removeIf(aclEntry -> aclEntry.getUser().equals(user));
        try {
            stackDao.update(recipe);
        } catch (NotFoundException ignored) {
        }
    }

    private List<PermissionsImpl> toPermissions(String stack, List<AclEntryImpl> acls) {
        return acls.stream()
                   .map(acl -> toPermission(stack, acl))
                   .collect(Collectors.toList());
    }

    private PermissionsImpl toPermission(String stack, AclEntryImpl acl) {
        return new PermissionsImpl(acl.getUser(),
                                   StackDomain.DOMAIN_ID,
                                   stack,
                                   acl.getActions());
    }
}
