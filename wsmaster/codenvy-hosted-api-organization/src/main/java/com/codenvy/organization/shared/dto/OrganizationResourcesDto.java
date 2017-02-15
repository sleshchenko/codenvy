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
package com.codenvy.organization.shared.dto;

import com.codenvy.organization.shared.model.OrganizationResources;
import com.codenvy.resource.shared.dto.ResourceDto;

import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * @author Sergii Leschenko
 */
@DTO
public interface OrganizationResourcesDto extends OrganizationResources {
    @Override
    String getOrganizationId();

    void setOrganizationId(String organizationId);

    OrganizationResourcesDto withOrganizationId(String organizationId);

    @Override
    List<ResourceDto> getResourcesCap();

    void setResourcesCap(List<ResourceDto> resourcesCap);

    OrganizationResourcesDto withResourcesCap(List<ResourceDto> resourcesCap);

    @Override
    List<ResourceDto> getReservedResources();

    void setReservedResources(List<ResourceDto> reservedResources);

    OrganizationResourcesDto withReservedResources(List<ResourceDto> reservedResources);
}
