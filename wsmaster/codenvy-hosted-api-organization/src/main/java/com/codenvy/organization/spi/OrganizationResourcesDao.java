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
package com.codenvy.organization.spi;

import com.codenvy.organization.spi.impl.OrganizationResourcesImpl;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;

/**
 * Defines data access object contract for {@link OrganizationResourcesImpl}.
 *
 * @author Sergii Leschenko
 */
public interface OrganizationResourcesDao {
    /**
     * Stores (creates or updated) distributed resources for suborganization.
     *
     * @param distributedResources
     *         distributed resources to store
     * @throws NullPointerException
     *         when either {@code distributedResources} is null
     * @throws ServerException
     *         when any other error occurs
     */
    void store(OrganizationResourcesImpl distributedResources) throws ServerException;

    /**
     * Returns distributed resources for specified suborganization.
     *
     * @param organizationId
     *         organization id
     * @return distributed resources for specified suborganization
     * @throws NullPointerException
     *         when either {@code organizationId} is null
     * @throws NotFoundException
     *         when organization with specified id doesn't have distributed resources
     * @throws ServerException
     *         when any other error occurs
     */
    OrganizationResourcesImpl get(String organizationId) throws NotFoundException, ServerException;

    /**
     * Returns distributed resources for suborganizations of given parent organization.
     *
     * @param organizationId
     *         organization id
     * @return distributed resources for suborganizations of given parent organization
     * @throws NullPointerException
     *         when either {@code organizationId} is null
     * @throws ServerException
     *         when any other error occurs
     */
    Page<OrganizationResourcesImpl> getByParent(String organizationId, int maxItems, long skipCount) throws ServerException;

    /**
     * Remove distributed organization resources.
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
