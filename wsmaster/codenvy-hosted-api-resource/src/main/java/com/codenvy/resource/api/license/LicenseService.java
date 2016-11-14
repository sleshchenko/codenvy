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
package com.codenvy.resource.api.license;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import com.codenvy.resource.shared.dto.LicenseDto;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static com.codenvy.resource.api.DtoConverter.asDto;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Defines License REST API.
 *
 * @author Sergii Leschenko
 */
@Api(value = "/license", description = "License REST API")
@Path("/license")
public class LicenseService {
    private LicenseManager licenseManager;

    @Inject
    public LicenseService(LicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }

    @GET
    @Path("/{accountId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get license for given account",
                  response = LicenseDto.class)
    @ApiResponses({@ApiResponse(code = 200, message = "The license successfully fetched"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public LicenseDto getLicense(@ApiParam("Account id")
                                 @PathParam("accountId") String accountId) throws NotFoundException, ServerException {
        return asDto(licenseManager.getByAccount(accountId));
    }
}
