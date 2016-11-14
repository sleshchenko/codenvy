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
package com.codenvy.organization.spi;

import com.codenvy.organization.spi.impl.OrganizationResourcesLimitImpl;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;

/**
 * Defines data access object contract for {@link OrganizationResourcesLimitImpl}.
 *
 * @author Sergii Leschenko
 */
public interface OrganizationResourcesLimitDao {
    /**
     * Stores resources limit for suborganization on parent's resources usage.
     *
     * @param resourcesLimit
     *         resources limit to store
     * @throws NullPointerException
     *         when either {@code resourcesLimit} is null
     * @throws ServerException
     *         when any other error occurs
     */
    void store(OrganizationResourcesLimitImpl resourcesLimit) throws ServerException;

    /**
     * Returns resources limit for given suborganization
     *
     * @param organizationId
     *         organization id
     * @return resources limit for suborganization with specified id
     * @throws NullPointerException
     *         when either {@code organizationId} is null
     * @throws ServerException
     *         when any other error occurs
     */
    OrganizationResourcesLimitImpl get(String organizationId) throws NotFoundException, ServerException;

    /**
     * Returns resources limits for suborganizations of given parent organization
     *
     * @param organizationId
     *         organization id
     * @return resources limit for suborganizations of given parent organization
     * @throws NullPointerException
     *         when either {@code organizationId} is null
     * @throws ServerException
     *         when any other error occurs
     */
    Page<OrganizationResourcesLimitImpl> getByParent(String organizationId, int maxItems, long skipCount) throws ServerException;

    /**
     * Remove organization resources limit.
     *
     * @param organizationId
     *         organization id
     * @throws NullPointerException
     *         when either {@code organizationId} is null
     * @throws ServerException
     *         when any other error occurs
     */
    void remove(String organizationId) throws ServerException;
}
