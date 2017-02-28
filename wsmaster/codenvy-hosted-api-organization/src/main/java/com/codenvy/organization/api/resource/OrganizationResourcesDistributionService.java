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
package com.codenvy.organization.api.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import com.codenvy.organization.api.DtoConverter;
import com.codenvy.organization.shared.dto.OrganizationDistributedResourcesDto;
import com.codenvy.resource.api.free.ResourceValidator;
import com.codenvy.resource.shared.dto.ResourceDto;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * REST API for sharing resources for suborganizations.
 *
 * @author Sergii Leschenko
 */
@Api(value = "organization-resource", description = "REST API for resources sharing for organizations")
@Path("/organization/resource")
public class OrganizationResourcesDistributionService extends Service {
    private final OrganizationResourcesDistributor resourcesDistributor;
    private final ResourceValidator                resourceValidator;

    @Inject
    public OrganizationResourcesDistributionService(OrganizationResourcesDistributor resourcesDistributor,
                                                    ResourceValidator resourceValidator) {
        this.resourcesDistributor = resourcesDistributor;
        this.resourceValidator = resourceValidator;
    }

    @POST
    @Path("/{suborganizationId}/cap")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Cap usage of shared resources.",
                  notes = "By default suborganization is able to use all parent organization resources." +
                          "Cap allow to limit usage of shared resources by suborganization.")
    @ApiResponses({@ApiResponse(code = 204, message = "Resources successfully capped"),
                   @ApiResponse(code = 400, message = "Missed required parameters, parameters are not valid"),
                   @ApiResponse(code = 404, message = "Specified organization was not found"),
                   @ApiResponse(code = 409, message = "Specified organization is root organization"),
                   @ApiResponse(code = 409, message = "Suborganization is using shared resources"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void capResources(@ApiParam("Suborganization id")
                             @PathParam("suborganizationId") String suborganizationId,
                             @ApiParam("Resources to cap") List<ResourceDto> resources) throws BadRequestException,
                                                                                               NotFoundException,
                                                                                               ConflictException,
                                                                                               ServerException {
        checkArgument(resources != null, "Missed resources caps.");
        Set<String> resourcesToSet = new HashSet<>();
        for (ResourceDto resource : resources) {
            if (!resourcesToSet.add(resource.getType())) {
                throw new BadRequestException(format("Resources to cap must contain only one resource with type '%s'.",
                                                     resource.getType()));
            }
            resourceValidator.validate(resource);
        }

        resourcesDistributor.cap(suborganizationId, resources);
    }

    //TODO Add test for this method
    @GET
    @Path("/{suborganizationId}/cap")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get resources caps of specified suborganization.",
                  response = OrganizationDistributedResourcesDto.class,
                  responseContainer = "list")
    @ApiResponses({@ApiResponse(code = 200, message = "Resources caps successfully fetched"),
                   //TODO Add exception description here
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public List<ResourceDto> getResourcesCap(@ApiParam("Suborganization id")
                                             @PathParam("suborganizationId") String suborganization) throws NotFoundException,
                                                                                                            ConflictException,
                                                                                                            ServerException {
        return resourcesDistributor.getResourcesCaps(suborganization)
                                   .stream()
                                   .map(com.codenvy.resource.api.DtoConverter::asDto)
                                   .collect(toList());
    }

    @GET
    @Path("/{organizationId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get resources which are distributed by specified parent.",
                  response = OrganizationDistributedResourcesDto.class,
                  responseContainer = "list")
    @ApiResponses({@ApiResponse(code = 200, message = "Resources caps successfully fetched"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public List<OrganizationDistributedResourcesDto> getDistributedResources(@ApiParam("Organization id")
                                                                             @PathParam("organizationId") String organizationId,
                                                                             @ApiParam(value = "Max items")
                                                                             @QueryParam("maxItems") @DefaultValue("30") int maxItems,
                                                                             @ApiParam(value = "Skip count")
                                                                             @QueryParam("skipCount") @DefaultValue("0") long skipCount)
            throws ServerException {

        return resourcesDistributor.getByParent(organizationId, maxItems, skipCount)
                                   .getItems()
                                   .stream()
                                   .map(DtoConverter::asDto)
                                   .collect(toList());
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression
     *         a boolean expression
     * @param errorMessage
     *         the exception message to use if the check fails
     * @throws BadRequestException
     *         if {@code expression} is false
     */
    private void checkArgument(boolean expression, String errorMessage) throws BadRequestException {
        if (!expression) {
            throw new BadRequestException(errorMessage);
        }
    }
}
