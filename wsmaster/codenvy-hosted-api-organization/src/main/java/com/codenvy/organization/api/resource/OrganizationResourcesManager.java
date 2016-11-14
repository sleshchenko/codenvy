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

import com.codenvy.organization.shared.model.OrganizationResourcesLimit;
import com.codenvy.organization.spi.OrganizationResourcesLimitDao;
import com.codenvy.organization.spi.impl.OrganizationResourcesLimitImpl;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Singleton;

import static java.util.Objects.requireNonNull;

/**
 * Facade for organization resources redistribution related operations.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class OrganizationResourcesManager {
    private final OrganizationResourcesLimitDao organizationResourcesLimitDao;

    @Inject
    public OrganizationResourcesManager(OrganizationResourcesLimitDao organizationResourcesLimitDao) {
        this.organizationResourcesLimitDao = organizationResourcesLimitDao;
    }

    /**
     * Set resources limit for suborganization on parent's resources usage.
     *
     * @param resourcesLimit
     *         resources limit to store
     * @throws NullPointerException
     *         when either {@code resourcesLimit} is null
     * @throws ServerException
     *         when any other error occurs
     */
    public void setResourcesLimit(OrganizationResourcesLimit resourcesLimit) throws ServerException {
        requireNonNull(resourcesLimit, "Required non-null limit");
        organizationResourcesLimitDao.store(new OrganizationResourcesLimitImpl(resourcesLimit));
    }

    /**
     * Return resources limit for given organization
     *
     * @param organizationId
     *         organization id
     * @return resources limit for given organization
     * @throws NotFoundException
     *         when specified organization doesn't have resources limit
     * @throws ServerException
     *         when any other error occurs
     */
    public OrganizationResourcesLimit get(String organizationId) throws NotFoundException, ServerException {
        requireNonNull(organizationId, "Required non-null organization id");
        return organizationResourcesLimitDao.get(organizationId);
    }

    /**
     * Returns resources limits for suborganizations of given parent organization
     *
     * <p>It also returns limit for current organization that is calculated as
     * difference between provided resources by license and redistributed resources.
     *
     * @param organizationId
     *         organization id
     * @return resources limits for suborganizations of given parent organization
     * @throws NullPointerException
     *         when either {@code organizationId} is null
     * @throws ServerException
     *         when any other error occurs
     */
    public Page<? extends OrganizationResourcesLimit> getResourcesLimits(String organizationId,
                                                                         int maxItems,
                                                                         long skipCount) throws ServerException {
        requireNonNull(organizationId, "Required non-null organization id");

        return organizationResourcesLimitDao.getByParent(organizationId, maxItems, skipCount);
    }

    /**
     * Remove organization resources limit.
     *
     * <p>After removing limit given organization won't be able to use parent resources.
     *
     * @param organizationId
     *         organization id
     * @throws NullPointerException
     *         when either {@code organizationId} is null
     * @throws ServerException
     *         when any other error occurs
     */
    public void remove(String organizationId) throws ServerException {
        requireNonNull(organizationId, "Required non-null organization id");
        organizationResourcesLimitDao.remove(organizationId);
    }
}
