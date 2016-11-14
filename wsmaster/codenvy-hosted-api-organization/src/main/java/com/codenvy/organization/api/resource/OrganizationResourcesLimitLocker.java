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
import com.codenvy.organization.shared.model.OrganizationResourcesLimit;
import com.codenvy.resource.api.exception.NoEnoughResourcesException;
import com.codenvy.resource.api.ResourceAggregator;
import com.codenvy.resource.api.usage.ResourceUsageManager;
import com.codenvy.resource.api.usage.ResourceUsageManager.ResourceUsageOperation;
import com.codenvy.resource.model.Resource;
import com.google.inject.Inject;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Check resources availability and lock resources during setting organization resources limit
 *
 * @author Sergii Leschenko
 */
@Singleton
public class OrganizationResourcesLimitLocker implements MethodInterceptor {
    @Inject
    private OrganizationResourcesManager suborganizationResourcesLimitDao;
    @Inject
    private ResourceAggregator           resourceAggregator;
    @Inject
    private OrganizationManager          organizationManager;
    @Inject
    private ResourceUsageManager         resourceUsageManager;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (invocation.getMethod().getName().equals("setResourcesLimit")) {
            final OrganizationResourcesLimit limitToStore = (OrganizationResourcesLimit)invocation.getArguments()[0];

            final String organizationId = limitToStore.getOrganizationId();

            final OrganizationResourcesLimit existedLimit = getExistedLimit(organizationId);

            return checkLimitChanging(organizationId, limitToStore.getResources(), existedLimit, invocation);
        } else if (invocation.getMethod().getName().equals("remove")) {
            final String organizationId = (String)invocation.getArguments()[0];
            final OrganizationResourcesLimit existedLimit = getExistedLimit(organizationId);
            if (existedLimit != null) {
                return lockResources(organizationId,
                                     existedLimit.getResources(),
                                     invocation::proceed,
                                     // TODO Implement stopping of workspaces and
                                     // TODO removing limits whole all suborganization's tree automatically
                                     "Stop workspaces before removing resources.");
            }
            //there is no limit. We should not lock resources
        }
        return invocation.proceed();
    }

    private Object checkLimitChanging(String organizationId,
                                      List<? extends Resource> newResources,
                                      OrganizationResourcesLimit existedLimit,
                                      MethodInvocation invocation) throws Throwable {
        if (existedLimit == null) {
            // limit is new, so there is no suborganization isn't using any resources at this moment
            // try to lock resources on parent organization level
            return lockResources(getParentId(organizationId),
                                 newResources,
                                 invocation::proceed,
                                 "Parent organization doesn't have enough resources. Try to stop ws, or redistribute resources" +
                                 "in other way");
        }

        // there is old limit, so we should check used resources if new limit is less than existed one
        final List<Resource> difference = new ArrayList<>();
        int diffResult = fillWithLimitsDifference(newResources,
                                                  existedLimit.getResources(),
                                                  difference);
        if (diffResult > 0) {
            // new limit is greater than existed and we should check used resources of parent organization
            // we should reserve difference
            return lockResources(getParentId(organizationId),
                                 difference,
                                 invocation::proceed,
                                 "Parent organization doesn't have enough resources. Try to stop ws, or redistribute resources");
        } else if (diffResult < 0) {
            //new limit is less than existed one we should check used resources of suborganization
            return lockResources(organizationId,
                                 difference,
                                 invocation::proceed,
                                 "Stop wss before resources redistribution");
        } else {
            //TODO Fix it
            throw new ServerException("Parent organization decreases and increases resource limit." +
                                      "It is required to check resources availability on parent org and sub org.");
        }
    }

    private int fillWithLimitsDifference(List<? extends Resource> newResources,
                                         List<? extends Resource> existedLimitResources,
                                         List<Resource> container) throws ServerException {
        try {
            final List<? extends Resource> deduct = resourceAggregator.deduct(newResources, existedLimitResources);
            container.addAll(deduct);
            return 1;
        } catch (NoEnoughResourcesException e) {
            try {
                container.addAll(resourceAggregator.deduct(existedLimitResources, newResources));
                return -1;
            } catch (NoEnoughResourcesException e1) {
                //TODO It will be possible in case when parent organization increases limit for one resource and decreases for another one
                // for example old limit [20 gb, 10 workspaces] and new limit is [15 gb, 15 workspaces]. So fix it
                return 0;
            } catch (NotFoundException e1) {
                throw new ServerException("Should not happen");
            }
        } catch (NotFoundException e) {
            throw new ServerException("Should not happen");
        }
    }

    private <T, X extends Throwable> T lockResources(String resourcesOwner,
                                                     List<? extends Resource> toReserve,
                                                     ResourceUsageOperation<T, X> operation,
                                                     String message) throws X,
                                                                            NotFoundException,
                                                                            ServerException,
                                                                            ConflictException {
        try {
            return resourceUsageManager.lockResources(resourcesOwner,
                                                      toReserve,
                                                      operation);
        } catch (NoEnoughResourcesException e) {
            throw new ConflictException(message);
        }
    }

    private String getParentId(String suborganization) throws ServerException {
        try {
            return organizationManager.getById(suborganization).getParent();
        } catch (NotFoundException e) {
            throw new ServerException("Organization is not found");
        }
    }

    private OrganizationResourcesLimit getExistedLimit(String organizationId) throws ServerException {
        try {
            return suborganizationResourcesLimitDao.get(organizationId);
        } catch (NotFoundException e1) {
            return null;
        }
    }
}
