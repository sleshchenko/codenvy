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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import com.codenvy.organization.api.DtoConverter;
import com.codenvy.organization.shared.dto.OrganizationResourcesLimitDto;
import com.codenvy.organization.shared.model.OrganizationResourcesLimit;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * REST API for resources redistribution between suborganizations
 *
 * @author Sergii Leschenko
 */
@Api(value = "/organization/resource", description = "REST API for resources redistribution between suborganizations")
@Path("/organization/resource")
public class OrganizationResourcesService extends Service {
    private final OrganizationResourcesManager        organizationResourcesManager;
    private final OrganizationResourcesLimitValidator validator;

    @Inject
    public OrganizationResourcesService(OrganizationResourcesManager organizationResourcesManager,
                                        OrganizationResourcesLimitValidator validator) {
        this.organizationResourcesManager = organizationResourcesManager;
        this.validator = validator;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Set limit for suborganization on parent's resources usage",
                  response = OrganizationResourcesLimitDto.class)
    @ApiResponses({@ApiResponse(code = 204, message = "The organization resources limit successfully stored"),
                   @ApiResponse(code = 400, message = "Missed required parameters, parameters are not valid"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void setResourcesLimit(@ApiParam(value = "Organization resources limit to store")
                                  OrganizationResourcesLimitDto limit) throws BadRequestException,
                                                                              ServerException,
                                                                              ConflictException {
        validator.check(limit);

        organizationResourcesManager.setResourcesLimit(limit);
    }

    @GET
    @Path("{organizationId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get stored resources limit for organizations",
                  response = OrganizationResourcesLimitDto.class,
                  responseContainer = "list")
    @ApiResponses({@ApiResponse(code = 200, message = "The organizations resources limit successfully fetched"),
                   @ApiResponse(code = 400, message = "Missed required parameters, parameters are not valid"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public Response getResourcesLimits(@ApiParam("Organization id")
                                       @PathParam("organizationId") String organizationId,
                                       @ApiParam(value = "Max items")
                                       @QueryParam("maxItems") @DefaultValue("30") int maxItems,
                                       @ApiParam(value = "Skip count")
                                       @QueryParam("skipCount") @DefaultValue("0") long skipCount)
            throws ServerException, BadRequestException {

        checkArgument(maxItems >= 0, "The number of items to return can't be negative.");
        checkArgument(skipCount >= 0, "The number of items to skip can't be negative.");

        final Page<? extends OrganizationResourcesLimit> limitsPage = organizationResourcesManager.getResourcesLimits(organizationId,
                                                                                                                         maxItems,
                                                                                                                         skipCount);
        return Response.ok()
                       .entity(limitsPage.getItems(DtoConverter::asDto))
                       .header("Link", createLinkHeader(limitsPage))
                       .build();
    }

    @DELETE
    @Path("/{organizationId}")
    @ApiOperation(value = "Remove organization resources limit.",
                  notes = "After removing limit given organization won't be able to use parent resources.")
    @ApiResponses({@ApiResponse(code = 201, message = "The resources limit successfully removed"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void removeResourcesLimit(@ApiParam(value = "Organization id")
                                     @PathParam("organizationId") String organizationId) throws ServerException {
        organizationResourcesManager.remove(organizationId);
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
