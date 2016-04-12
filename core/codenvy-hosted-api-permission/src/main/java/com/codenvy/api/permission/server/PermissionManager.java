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
package com.codenvy.api.permission.server;

import com.codenvy.api.permission.server.dao.PermissionsStorage;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facade for Permissions related operations.
 *
 * @author gazarenkov
 * @author Sergii Leschenko
 */
@Singleton
public class PermissionManager {
    private final Map<String, PermissionsStorage> domainToStorage = new HashMap<>();
    private final Map<String, PermissionsDomain>  domains         = new HashMap<>();

    @Inject
    public PermissionManager(Set<PermissionsStorage> storages) throws ServerException {
        for (PermissionsStorage storage : storages) {
            for (PermissionsDomain domain : storage.getDomains()) {
                domains.put(domain.getId(), domain);
                PermissionsStorage oldStorage = domainToStorage.put(domain.getId(), storage);
                if (oldStorage != null) {
                    throw new ServerException("Permissions Domain '" + domain.getId() + "' should be stored in only one storage. " +
                                              "Duplicated in " + storage.getClass() + " and " + oldStorage.getClass());
                }
            }
        }
    }

    /**
     * Stores (adds or updates) permissions.
     *
     * @param permissions
     *         permission to store
     * @throws ConflictException
     *         when permissions have unsupported domain
     * @throws ConflictException
     *         when new permissions remove last 'setPermissions' of given instance
     * @throws ServerException
     *         when any other error occurs during permissions storing
     */
    public void storePermission(PermissionsImpl permissions) throws ServerException, ConflictException {
        final String domain = permissions.getDomain();
        final String instance = permissions.getInstance();
        final String user = permissions.getUser();

        final PermissionsStorage permissionsStorage = getPermissionsStorage(domain);
        if (!permissions.getActions().contains("setPermissions")
            && userHasLastSetPermissions(permissionsStorage, user, domain, instance)) {
            throw new ConflictException("Can't edit permissions because there is not any another user with permission 'setPermissions'");
        }
        permissionsStorage.store(permissions);
    }

    /**
     * @param user
     *         user id
     * @param domain
     *         domain id
     * @param instance
     *         instance id
     * @return user's permissions for specified instance
     * @throws ConflictException
     *         when given domain is unsupported
     * @throws NotFoundException
     *         when permissions with given user and domain and instance was not found
     * @throws ServerException
     *         when any other error occurs during permissions fetching
     */
    public PermissionsImpl get(String user, String domain, String instance) throws ServerException, ConflictException, NotFoundException {
        return getPermissionsStorage(domain).get(user, domain, instance);
    }

    /**
     * @param domain
     *         domain id
     * @param instance
     *         instance id
     * @return set of permissions
     * @throws ConflictException
     *         when given domain is unsupported
     * @throws ServerException
     *         when any other error occurs during permissions fetching
     */
    public List<PermissionsImpl> getByInstance(String domain, String instance) throws ServerException, ConflictException {
        return getPermissionsStorage(domain).getByInstance(domain, instance);
    }

    /**
     * @param user
     *         user id
     * @return set of permissions
     * @throws ServerException
     *         when any other error occurs during permissions fetching
     */
    public List<PermissionsImpl> get(String user) throws ServerException {
        List<PermissionsImpl> result = new ArrayList<>();
        for (PermissionsStorage permissionsStorage : domainToStorage.values()) {
            result.addAll(permissionsStorage.get(user));
        }
        return result;
    }

    /**
     * @param user
     *         user id
     * @param domain
     *         domain id
     * @return set of permissions
     * @throws ConflictException
     *         when given domain is unsupported
     * @throws ServerException
     *         when any other error occurs during permissions fetching
     */
    public List<PermissionsImpl> get(String user, String domain) throws ServerException, ConflictException {
        return getPermissionsStorage(domain).get(user, domain);
    }

    /**
     * Removes permissions of user related to the particular instance of specified domain
     *
     * @param user
     *         user id
     * @param domain
     *         domain id
     * @param instance
     *         instance id
     * @throws ConflictException
     *         when given domain is unsupported
     * @throws ConflictException
     *         when removes last 'setPermissions' of given instance
     * @throws ServerException
     *         when any other error occurs during permissions removing
     */
    public void remove(String user, String domain, String instance) throws ConflictException, ServerException {
        final PermissionsStorage permissionsStorage = getPermissionsStorage(domain);
        if (userHasLastSetPermissions(permissionsStorage, user, domain, instance)) {
            throw new ConflictException("Can't remove permissions because there is not any another user with permission 'setPermissions'");
        }
        permissionsStorage.remove(user, domain, instance);
    }

    /**
     * @param user
     *         user id
     * @param domain
     *         domain id
     * @param instance
     *         instance id
     * @param action
     *         action name
     * @return true if the permission exists
     * @throws ConflictException
     *         when given domain is unsupported
     * @throws ServerException
     *         when any other error occurs during permission existence checking
     */
    public boolean exists(String user, String domain, String instance, String action) throws ServerException, ConflictException {
        return getPermissionsStorage(domain).exists(user, domain, instance, action);
    }

    /**
     * Returns identifiers of supported domains
     */
    public Set<String> getDomains() {
        return domains.keySet();
    }

    /**
     * Returns supported actions of specified domain
     *
     * @param domainId
     *         id of domain
     */
    public Set<String> getDomainsActions(String domainId) throws ConflictException {
        final PermissionsDomain permissionsDomain = domains.get(domainId);
        if (permissionsDomain == null) {
            throw new ConflictException("Requested unsupported domain '" + domainId + "'");
        }
        return permissionsDomain.getAllowedActions();
    }

    private PermissionsStorage getPermissionsStorage(String domain) throws ConflictException {
        final PermissionsStorage permissionsStorage = domainToStorage.get(domain);
        if (permissionsStorage == null) {
            throw new ConflictException("Requested unsupported domain '" + domain + "'");
        }
        return permissionsStorage;
    }

    private boolean userHasLastSetPermissions(PermissionsStorage permissionsStorage, String user, String domain, String instance)
            throws ServerException, ConflictException {
        return permissionsStorage.exists(user, domain, instance, "setPermissions")
               && !permissionsStorage.getByInstance(domain, instance)
                                     .stream()
                                     .anyMatch(permission -> !permission.getUser().equals(user)
                                                             && permission.getActions().contains("setPermissions"));
    }
}
