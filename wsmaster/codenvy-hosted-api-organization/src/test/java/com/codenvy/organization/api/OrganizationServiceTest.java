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
package com.codenvy.organization.api;

import com.codenvy.organization.shared.dto.OrganizationDto;
import com.codenvy.organization.shared.model.Organization;
import com.codenvy.organization.spi.impl.OrganizationImpl;
import com.jayway.restassured.response.Response;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.api.core.rest.CheJsonProvider;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.SubjectImpl;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.assured.EverrestJetty;
import org.everrest.core.Filter;
import org.everrest.core.GenericContainerRequest;
import org.everrest.core.RequestFilter;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link OrganizationService}.
 *
 * @author Sergii Leschenko
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class OrganizationServiceTest {

    private static final String CURRENT_USER_ID = "user123";

    @SuppressWarnings("unused") //is declared for deploying by everrest-assured
    private ApiExceptionMapper mapper;

    @SuppressWarnings("unused") //is declared for deploying by everrest-assured
    private EnvironmentFilter filter;

    @SuppressWarnings("unused") //is declared for deploying by everrest-assured
    private CheJsonProvider jsonProvider = new CheJsonProvider(new HashSet<>());

    @Mock
    private OrganizationManager orgManager;

    @Mock
    private OrganizationLinksInjector linksInjector;

    @Mock
    private OrganizationValidator validator;

    @InjectMocks
    private OrganizationService service;

    @BeforeMethod
    public void setUp() throws Exception {
        when(linksInjector.injectLinks(any(), any()))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
    }

    @Test
    public void shouldCreateOrganization() throws Exception {
        when(orgManager.create(any()))
                .thenAnswer(invocationOnMock -> new OrganizationImpl((Organization)invocationOnMock.getArguments()[0]));

        final OrganizationDto toCreate = createOrganization();

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .body(toCreate)
                                         .when()
                                         .expect().statusCode(201)
                                         .post(SECURE_PATH + "/organization");

        final OrganizationDto createdOrganization = unwrapDto(response, OrganizationDto.class);
        assertEquals(createdOrganization, toCreate);
        verify(linksInjector).injectLinks(any(), any());
        verify(orgManager).create(eq(toCreate));
    }

    @Test
    public void shouldThrowBadRequestWhenCreatingNonValidOrganization() throws Exception {
        doThrow(new BadRequestException("non valid organization")).when(validator).checkOrganization(any());

        final OrganizationDto toCreate = createOrganization();

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .body(toCreate)
                                         .when()
                                         .expect().statusCode(400)
                                         .post(SECURE_PATH + "/organization");

        final ServiceError error = unwrapDto(response, ServiceError.class);
        assertEquals(error.getMessage(), "non valid organization");
        verify(validator).checkOrganization(toCreate);
    }

    @Test
    public void shouldUpdateOrganization() throws Exception {
        when(orgManager.update(anyString(), any()))
                .thenAnswer(invocationOnMock -> new OrganizationImpl((Organization)invocationOnMock.getArguments()[1]));

        final OrganizationDto toUpdate = createOrganization();

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .body(toUpdate)
                                         .when()
                                         .expect()
                                         .statusCode(200)
                                         .post(SECURE_PATH + "/organization/organization123");

        final OrganizationDto createdOrganization = unwrapDto(response, OrganizationDto.class);
        assertEquals(createdOrganization, toUpdate);
        verify(linksInjector).injectLinks(any(), any());
        verify(orgManager).update(eq("organization123"), eq(toUpdate));
    }

    @Test
    public void shouldThrowBadRequestWhenUpdatingNonValidOrganization() throws Exception {
        doThrow(new BadRequestException("non valid organization")).when(validator).checkOrganization(any());

        final OrganizationDto toUpdate = createOrganization();

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .body(toUpdate)
                                         .when()
                                         .expect()
                                         .statusCode(400)
                                         .post(SECURE_PATH + "/organization/organization123");

        final ServiceError error = unwrapDto(response, ServiceError.class);
        assertEquals(error.getMessage(), "non valid organization");
        verify(validator).checkOrganization(toUpdate);
    }

    @Test
    public void shouldRemoveOrganization() throws Exception {
        given().auth()
               .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .contentType("application/json")
               .when()
               .expect()
               .statusCode(204)
               .delete(SECURE_PATH + "/organization/organization123");

        verify(orgManager).remove(eq("organization123"));
    }

    @Test
    public void shouldGetOrganizationById() throws Exception {
        final OrganizationDto toFetch = DtoFactory.newDto(OrganizationDto.class)
                                                  .withId("organization123")
                                                  .withName("MyOrganization")
                                                  .withParent("parentOrg123");

        when(orgManager.getById(eq("organization123"))).thenReturn(new OrganizationImpl(toFetch));

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .expect()
                                         .statusCode(200)
                                         .get(SECURE_PATH + "/organization/organization123");

        final OrganizationDto fetchedOrganization = unwrapDto(response, OrganizationDto.class);
        assertEquals(fetchedOrganization, toFetch);
        verify(orgManager).getById(eq("organization123"));
        verify(linksInjector).injectLinks(any(), any());
    }

    @Test
    public void shouldFindOrganizationByName() throws Exception {
        final OrganizationDto toFetch = DtoFactory.newDto(OrganizationDto.class)
                                                  .withId("organization123")
                                                  .withName("MyOrganization")
                                                  .withParent("parentOrg123");

        when(orgManager.getByName(eq("MyOrganization"))).thenReturn(new OrganizationImpl(toFetch));

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .expect()
                                         .statusCode(200)
                                         .get(SECURE_PATH + "/organization/find?name=MyOrganization");

        final OrganizationDto fetchedOrganization = unwrapDto(response, OrganizationDto.class);
        assertEquals(fetchedOrganization, toFetch);
        verify(orgManager).getByName(eq("MyOrganization"));
        verify(linksInjector).injectLinks(any(), any());
    }

    @Test
    public void shouldThrowBadRequestExceptionWhenFindingOrganizationWithoutName() throws Exception {
        given().auth()
               .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .contentType("application/json")
               .when()
               .expect()
               .statusCode(400)
               .get(SECURE_PATH + "/organization/find");
    }

    @Test
    public void shouldGetChildOrganizations() throws Exception {
        final OrganizationDto toFetch = DtoFactory.newDto(OrganizationDto.class)
                                                  .withId("organization123")
                                                  .withName("MyOrganization")
                                                  .withParent("parentOrg123");

        doReturn(new Page<>(singletonList(new OrganizationImpl(toFetch)), 0, 1, 1))
                .when(orgManager).getByParent(anyString(), anyInt(), anyInt());

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .expect()
                                         .statusCode(200)
                                         .get(SECURE_PATH + "/organization/parentOrg123/organizations?skipCount=0&maxItems=1");

        final List<OrganizationDto> organizationDtos = unwrapDtoList(response, OrganizationDto.class);
        assertEquals(organizationDtos.size(), 1);
        assertEquals(organizationDtos.get(0), toFetch);
        verify(orgManager).getByParent("parentOrg123", 1, 0);
        verify(linksInjector).injectLinks(any(), any());
    }

    @Test
    public void shouldGetOrganizationsByCurrentUserIfParameterIsNotSpecified() throws Exception {
        final OrganizationDto toFetch = DtoFactory.newDto(OrganizationDto.class)
                                                  .withId("organization123")
                                                  .withName("MyOrganization")
                                                  .withParent("parentOrg123");

        doReturn(new Page<>(singletonList(new OrganizationImpl(toFetch)), 0, 1, 1))
                .when(orgManager).getByMember(anyString(), anyInt(), anyInt());

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .expect()
                                         .statusCode(200)
                                         .get(SECURE_PATH + "/organization?skipCount=0&maxItems=1");

        final List<OrganizationDto> organizationDtos = unwrapDtoList(response, OrganizationDto.class);
        assertEquals(organizationDtos.size(), 1);
        assertEquals(organizationDtos.get(0), toFetch);
        verify(orgManager).getByMember(CURRENT_USER_ID, 1, 0);
        verify(linksInjector).injectLinks(any(), any());
    }

    @Test
    public void shouldGetOrganizationsBySpecifiedUser() throws Exception {
        final OrganizationDto toFetch = DtoFactory.newDto(OrganizationDto.class)
                                                  .withId("organization123")
                                                  .withName("MyOrganization")
                                                  .withParent("parentOrg123");

        doReturn(new Page<>(singletonList(new OrganizationImpl(toFetch)), 0, 1, 1))
                .when(orgManager).getByMember(anyString(), anyInt(), anyInt());

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .expect()
                                         .statusCode(200)
                                         .get(SECURE_PATH + "/organization?user=user789&skipCount=0&maxItems=1");

        final List<OrganizationDto> organizationDtos = unwrapDtoList(response, OrganizationDto.class);
        assertEquals(organizationDtos.size(), 1);
        assertEquals(organizationDtos.get(0), toFetch);
        verify(orgManager).getByMember("user789", 1, 0);
        verify(linksInjector).injectLinks(any(), any());
    }

    private static <T> T unwrapDto(Response response, Class<T> dtoClass) {
        return DtoFactory.getInstance().createDtoFromJson(response.body().print(), dtoClass);
    }

    private static <T> List<T> unwrapDtoList(Response response, Class<T> dtoClass) {
        return DtoFactory.getInstance().createListDtoFromJson(response.body().print(), dtoClass)
                         .stream()
                         .collect(toList());
    }

    private OrganizationDto createOrganization() {
        return DtoFactory.newDto(OrganizationDto.class)
                         .withId("organization123")
                         .withName("MyOrganization")
                         .withParent("parentOrg123");
    }

    @Filter
    public static class EnvironmentFilter implements RequestFilter {
        public void doFilter(GenericContainerRequest request) {
            EnvironmentContext.getCurrent().setSubject(new SubjectImpl("userName", CURRENT_USER_ID, "token", false));
        }
    }
}
