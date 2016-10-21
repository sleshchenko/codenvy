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
package com.codenvy.organization.api.permissions;

import com.codenvy.api.permission.server.SystemDomain;
import com.codenvy.organization.api.OrganizationManager;
import com.codenvy.organization.api.resource.OrganizationResourcesService;
import com.codenvy.organization.shared.dto.OrganizationResourcesLimitDto;
import com.google.common.annotations.VisibleForTesting;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.everrest.CheMethodInvokerFilter;
import org.everrest.core.Filter;
import org.everrest.core.resource.GenericResourceMethod;

import javax.inject.Inject;
import javax.ws.rs.Path;

import static com.codenvy.organization.api.permissions.OrganizationDomain.DOMAIN_ID;
import static com.codenvy.organization.api.permissions.OrganizationPermissionsFilter.MANAGE_ORGANIZATIONS_ACTION;

/**
 * Restricts access to methods of {@link OrganizationResourcesService} by users' permissions
 *
 * <p>Filter contains rules for protecting of all methods of {@link OrganizationResourcesService}.<br>
 * In case when requested method is unknown filter throws {@link ForbiddenException}
 *
 * @author Sergii Leschenko
 */
@Filter
@Path("/organization/resource{path:(/.*)?}")
public class OrganizationResourceServicePermissionsFilter extends CheMethodInvokerFilter {
    static final String SET_SUBORGANIZATION_RESOURCES_LIMIT_METHOD      = "setResourcesLimit";
    static final String GET_RESOURCES_REDISTRIBUTION_INFORMATION_METHOD = "getResourcesLimits";
    static final String REMOVE_SUBORGANIZATION_RESOURCES_LIMIT_METHOD   = "removeResourcesLimit";

    @Inject
    private OrganizationManager organizationManager;

    @Override
    protected void filter(GenericResourceMethod genericMethodResource, Object[] arguments) throws ApiException {
        final String methodName = genericMethodResource.getMethod().getName();

        final Subject currentSubject = EnvironmentContext.getCurrent().getSubject();
        String organizationId = null;
        String suborganizationId = null;
        switch (methodName) {
            case SET_SUBORGANIZATION_RESOURCES_LIMIT_METHOD:
                final OrganizationResourcesLimitDto resourcesLimit = (OrganizationResourcesLimitDto)arguments[0];
                if (resourcesLimit == null) {
                    //do not check permissions because service should validate null body and throw BadRequestException
                    return;
                }
                suborganizationId = resourcesLimit.getOrganizationId();
                break;
            case REMOVE_SUBORGANIZATION_RESOURCES_LIMIT_METHOD:
                suborganizationId = (String)arguments[0];
                break;
            case GET_RESOURCES_REDISTRIBUTION_INFORMATION_METHOD:
                organizationId = (String)arguments[0];
                if (currentSubject.hasPermission(SystemDomain.DOMAIN_ID, null, MANAGE_ORGANIZATIONS_ACTION)) {
                    //user is able to see information about all organizations
                    return;
                }
                break;

            default:
                throw new ForbiddenException("The user does not have permission to perform this operation");
        }

        checkManageResourcesPermission(currentSubject, organizationId, suborganizationId);
    }

    /**
     * Checks permissions on parent organization level of organization with id {@code suborganizationId}.
     * Or on organization with id {@code organizationId} if {@code suborganizationId} is null.
     */
    @VisibleForTesting
    void checkManageResourcesPermission(Subject currentSubject, String organizationId, String suborganizationId) throws NotFoundException,
                                                                                                                        ForbiddenException,
                                                                                                                        ServerException {
        // if suborganization id is not null then we should check permission on parent organization level
        if (suborganizationId != null) {
            organizationId = getParentOrganizationId(suborganizationId);
            if (organizationId == null) {
                // requested organization is root
                throw new ForbiddenException("You can't set resources limits for root organization");
            }
        } else {
            //get organization to rethrow NotFoundException in case when organization doesn't exist
            organizationManager.getById(organizationId);
        }

        if (!currentSubject.hasPermission(DOMAIN_ID, organizationId, OrganizationDomain.MANAGE_RESOURCES)) {
            throw new ForbiddenException("The user does not have permission to manage resources of organization with id '"
                                         + organizationId + "'");
        }
    }

    private String getParentOrganizationId(String suborganizationId) throws NotFoundException, ServerException {
        return organizationManager.getById(suborganizationId).getParent();
    }
}
