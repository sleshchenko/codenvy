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
package com.codenvy.organization.api.resource;

import com.codenvy.organization.api.OrganizationManager;
import com.codenvy.organization.shared.model.Organization;
import com.codenvy.organization.spi.impl.OrganizationImpl;
import com.codenvy.resource.api.ResourceLockProvider;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Singleton;

import static java.lang.String.format;

/**
 * Provides resources lock id for accounts with organizational type.
 *
 * <p>Organizational account can share resources with its suborganizations,
 * so we should lock resources by root organization id.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class OrganizationResourceLockProvider implements ResourceLockProvider {
    private OrganizationManager organizationManager;

    @Inject
    public OrganizationResourceLockProvider(OrganizationManager organizationManager) {
        this.organizationManager = organizationManager;
    }

    @Override
    public String getLockId(String accountId) throws ServerException {
        String currentOrganizationId = accountId;
        try {
            Organization organization = organizationManager.getById(currentOrganizationId);
            while (organization.getParent() != null) {
                currentOrganizationId = organization.getParent();
                organization = organizationManager.getById(currentOrganizationId);
            }
            return organization.getId();
        } catch (NotFoundException e) {
            throw new ServerException(format("Organization with id '%s' was not found during resources locking.",
                                             currentOrganizationId));
        }
    }

    @Override
    public String getAccountType() {
        return OrganizationImpl.ORGANIZATIONAL_ACCOUNT;
    }
}
