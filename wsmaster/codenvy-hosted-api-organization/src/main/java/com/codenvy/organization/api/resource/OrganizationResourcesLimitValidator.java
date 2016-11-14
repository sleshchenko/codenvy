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
import com.codenvy.resource.api.free.ResourceValidator;
import com.codenvy.resource.model.Resource;

import org.eclipse.che.api.core.BadRequestException;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Utils for validation of {@link OrganizationResourcesLimit}
 *
 * @author Sergii Leschenko
 */
@Singleton
public class OrganizationResourcesLimitValidator {
    private final ResourceValidator resourceValidator;

    @Inject
    public OrganizationResourcesLimitValidator(ResourceValidator resourceValidator) {
        this.resourceValidator = resourceValidator;
    }

    /**
     * Validates given {@code organizationResourcesLimit}
     *
     * @param organizationResourcesLimit
     *         resources limit to validate
     * @throws BadRequestException
     *         when {@code organizationResourcesLimit} is null
     * @throws BadRequestException
     *         when any of {@code organizationResourcesLimit.getResources} is not valid
     * @see ResourceValidator#check(Resource)
     */
    public void check(OrganizationResourcesLimit organizationResourcesLimit) throws BadRequestException {
        if (organizationResourcesLimit == null) {
            throw new BadRequestException("Missed organization resources limit description.");
        }
        if (organizationResourcesLimit.getOrganizationId() == null) {
            throw new BadRequestException("Missed organization id.");
        }

        for (Resource resource : organizationResourcesLimit.getResources()) {
            resourceValidator.check(resource);
        }
    }
}
