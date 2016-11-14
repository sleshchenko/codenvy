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

import com.codenvy.organization.shared.dto.OrganizationResourcesLimitDto;
import com.codenvy.resource.shared.dto.ResourceDto;
import com.jayway.restassured.response.Response;

import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.api.core.rest.CheJsonProvider;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.assured.EverrestJetty;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_NAME;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_PASSWORD;
import static org.everrest.assured.JettyHttpServer.SECURE_PATH;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link OrganizationResourcesService}
 *
 * @author Sergii Leschenko
 */
@Listeners({EverrestJetty.class, MockitoTestNGListener.class})
public class OrganizationResourcesServiceTest {
    @SuppressWarnings("unused") //is declared for deploying by everrest-assured
    private ApiExceptionMapper mapper;

    @SuppressWarnings("unused") //is declared for deploying by everrest-assured
    private CheJsonProvider jsonProvider = new CheJsonProvider(new HashSet<>());

    @Mock
    private OrganizationResourcesManager        organizationResourcesManager;
    @Mock
    private OrganizationResourcesLimitValidator validator;

    @InjectMocks
    private OrganizationResourcesService service;

    @Test
    public void shouldSetOrganizationResourcesLimit() throws Exception {
        final OrganizationResourcesLimitDto toCreate = createOrganizationResourcesLimit();

        given().auth()
               .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .contentType("application/json")
               .body(toCreate)
               .when()
               .expect().statusCode(204)
               .post(SECURE_PATH + "/organization/resource");

        verify(organizationResourcesManager).setResourcesLimit(toCreate);
        verify(validator).check(toCreate);
    }

    @Test
    public void shouldReturnOrganizationResourcesLimits() throws Exception {
        final OrganizationResourcesLimitDto limit = createOrganizationResourcesLimit();
        final List<OrganizationResourcesLimitDto> limits = singletonList(limit);
        doReturn(new Page<>(limits, 1, 1, 3))
                .when(organizationResourcesManager).getResourcesLimits(any(), anyInt(), anyLong());

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .expect().statusCode(200)
                                         .get(SECURE_PATH + "/organization/resource/organization123?maxItems=1&skipCount=1");

        final List<OrganizationResourcesLimitDto> fetchedLimits = unwrapDtoList(response, OrganizationResourcesLimitDto.class);
        assertEquals(fetchedLimits.size(), 1);
        assertTrue(fetchedLimits.contains(limit));
        verify(organizationResourcesManager).getResourcesLimits("organization123", 1, 1L);
    }

    @Test
    public void shouldRemoveOrganizationResourcesLimit() throws Exception {
        given().auth()
               .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .contentType("application/json")
               .when()
               .expect().statusCode(204)
               .delete(SECURE_PATH + "/organization/resource/organization123");

        verify(organizationResourcesManager).remove("organization123");
    }

    private static <T> List<T> unwrapDtoList(Response response, Class<T> dtoClass) {
        return DtoFactory.getInstance().createListDtoFromJson(response.body().print(), dtoClass)
                         .stream()
                         .collect(toList());
    }

    private OrganizationResourcesLimitDto createOrganizationResourcesLimit() {
        return DtoFactory.newDto(OrganizationResourcesLimitDto.class)
                         .withOrganizationId("organization123")
                         .withResources(singletonList(DtoFactory.newDto(ResourceDto.class)
                                                                .withType("test")
                                                                .withAmount(1020)
                                                                .withUnit("unit")));
    }
}
