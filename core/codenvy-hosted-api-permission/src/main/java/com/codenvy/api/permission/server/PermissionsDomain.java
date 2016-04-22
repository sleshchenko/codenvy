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

import com.google.common.collect.ImmutableSet;

import java.util.Objects;
import java.util.Set;

/**
 * Describes permissions domain
 *
 * @author gazarenkov
 * @author Sergii Leschenko
 */
public class PermissionsDomain {
    private final String      id;
    private final Set<String> allowedActions;

    public PermissionsDomain(String id, Set<String> allowedActions) {
        this.id = id;
        this.allowedActions = ImmutableSet.copyOf(allowedActions);
    }

    /**
     * Returns id of permissions domain
     */
    public String getId() {
        return id;
    }

    /**
     * Returns list actions which are allowed for domain
     */
    public Set<String> getAllowedActions() {
        return allowedActions;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PermissionsDomain)) return false;
        final PermissionsDomain other = (PermissionsDomain)obj;
        return Objects.equals(id, other.id) &&
               Objects.equals(allowedActions, other.allowedActions);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(id);
        hash = 31 * hash + Objects.hashCode(allowedActions);
        return hash;
    }

    @Override
    public String toString() {
        return "PermissionsDomain{" +
               "id='" + id + '\'' +
               ", allowedActions=" + allowedActions +
               "}";
    }
}
