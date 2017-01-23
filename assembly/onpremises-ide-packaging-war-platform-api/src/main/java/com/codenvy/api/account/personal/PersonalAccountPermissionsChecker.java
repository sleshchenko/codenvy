/*
 *  [2012] - [2017] Codenvy, S.A.
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
package com.codenvy.api.account.personal;

import com.codenvy.api.workspace.server.filters.AccountPermissionsChecker;
import com.codenvy.organization.api.permissions.OrganizationDomain;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;

import javax.inject.Singleton;

@Singleton
public class PersonalAccountPermissionsChecker implements AccountPermissionsChecker {
    @Override
    public void checkAccess(String id, String action) throws ForbiddenException {
        if (!EnvironmentContext.getCurrent().getSubject().getUserId().equals(id)) {
            throw new ForbiddenException("User is not authorized to use specified namespace.");
        }
    }
}
