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
import com.codenvy.organization.spi.impl.OrganizationImpl;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.api.core.rest.CheJsonProvider;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.assured.EverrestJetty;
import org.everrest.core.Filter;
import org.everrest.core.GenericContainerRequest;
import org.everrest.core.RequestFilter;
import org.everrest.core.resource.GenericResourceMethod;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.codenvy.organization.api.permissions.OrganizationPermissionsFilter.MANAGE_ORGANIZATIONS_ACTION;
import static com.jayway.restassured.RestAssured.given;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_NAME;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_PASSWORD;
import static org.everrest.assured.JettyHttpServer.SECURE_PATH;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link OrganizationResourceServicePermissionsFilterTest}
 *
 * @author Sergii Leschenko
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class OrganizationResourceServicePermissionsFilterTest {
    @SuppressWarnings("unused")
    private static final ApiExceptionMapper MAPPER        = new ApiExceptionMapper();
    @SuppressWarnings("unused")
    private static final EnvironmentFilter  FILTER        = new EnvironmentFilter();
    @SuppressWarnings("unused")
    private static final CheJsonProvider    JSON_PROVIDER = new CheJsonProvider(new HashSet<>());

    private static final String ORGANIZATION        = "org123";
    private static final String PARENT_ORGANIZATION = "parentOrg123";

    @Mock
    private OrganizationResourcesService service;

    @Mock
    private OrganizationManager manager;

    @Mock
    private static Subject subject;

    @InjectMocks
    @Spy
    private OrganizationResourceServicePermissionsFilter permissionsFilter;

    @BeforeMethod
    public void setUp() throws Exception {
        when(manager.getById(ORGANIZATION)).thenReturn(new OrganizationImpl(ORGANIZATION, "testOrg", PARENT_ORGANIZATION));
        when(manager.getById(PARENT_ORGANIZATION)).thenReturn(new OrganizationImpl(PARENT_ORGANIZATION, "parentOrg", null));

        when(subject.hasPermission(anyString(), anyString(), anyString())).thenReturn(true);
        doNothing().when(permissionsFilter).checkManageResourcesPermission(any(), anyString(), anyString());
    }

    @Test
    public void shouldTestThatAllPublicMethodsAreCoveredByPermissionsFilter() throws Exception {
        //given
        final List<String> collect = Stream.of(OrganizationResourcesService.class.getDeclaredMethods())
                                           .filter(method -> Modifier.isPublic(method.getModifiers()))
                                           .map(Method::getName)
                                           .collect(Collectors.toList());

        //then
        assertEquals(collect.size(), 3);
        assertTrue(collect.contains(OrganizationResourceServicePermissionsFilter.SET_SUBORGANIZATION_RESOURCES_LIMIT_METHOD));
        assertTrue(collect.contains(OrganizationResourceServicePermissionsFilter.GET_RESOURCES_REDISTRIBUTION_INFORMATION_METHOD));
        assertTrue(collect.contains(OrganizationResourceServicePermissionsFilter.REMOVE_SUBORGANIZATION_RESOURCES_LIMIT_METHOD));
    }

    @Test
    public void shouldCheckManageResourcesPermissionsOnSettingResourcesLimitForSuborganization() throws Exception {
        final OrganizationResourcesLimitDto resourcesLimit = DtoFactory.newDto(OrganizationResourcesLimitDto.class)
                                                                       .withOrganizationId(ORGANIZATION);
        given().auth()
               .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .contentType(MediaType.APPLICATION_JSON)
               .body(resourcesLimit)
               .expect()
               .statusCode(204)
               .when()
               .post(SECURE_PATH + "/organization/resource");

        verify(service).setResourcesLimit(resourcesLimit);
        verify(permissionsFilter).checkManageResourcesPermission(any(), eq(null), eq(ORGANIZATION));
    }

    @Test
    public void shouldNotCheckPermissionsOnSettingResourcesLimitForSuborganizationWithNullBody() throws Exception {
        given().auth()
               .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .contentType(MediaType.APPLICATION_JSON)
               .expect()
               .statusCode(204)
               .when()
               .post(SECURE_PATH + "/organization/resource");

        verify(service).setResourcesLimit(null);
        verify(subject, never()).hasPermission(anyString(), anyString(), anyString());
    }

    @Test
    public void shouldCheckManageResourcesPermissionsOnRemovingResourcesLimitForSuborganization() throws Exception {
        given().auth()
               .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .expect()
               .statusCode(204)
               .when()
               .delete(SECURE_PATH + "/organization/resource/" + ORGANIZATION);

        verify(service).removeResourcesLimit(ORGANIZATION);
        verify(permissionsFilter).checkManageResourcesPermission(any(), eq(null), eq(ORGANIZATION));
    }

    @Test
    public void shouldCheckManageResourcesPermissionsOnGettingResourcesLimitsWhenUserDoesNotHaveManageOrgazationsPermission()
            throws Exception {
        when(subject.hasPermission(SystemDomain.DOMAIN_ID, null, MANAGE_ORGANIZATIONS_ACTION)).thenReturn(false);

        given().auth()
               .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .expect()
               .statusCode(200)
               .when()
               .get(SECURE_PATH + "/organization/resource/" + PARENT_ORGANIZATION);

        verify(service).getResourcesLimits(PARENT_ORGANIZATION, anyInt(), anyLong());
        verify(subject).hasPermission(SystemDomain.DOMAIN_ID, null, OrganizationPermissionsFilter.MANAGE_ORGANIZATIONS_ACTION);
        verify(permissionsFilter).checkManageResourcesPermission(any(), eq(PARENT_ORGANIZATION), eq(null));
    }

    @Test
    public void shouldNotCheckManageResourcesPermissionsOnGettingResourcesLimitsWhenUserHasManageOrganizationsPermission()
            throws Exception {
        when(subject.hasPermission(SystemDomain.DOMAIN_ID, null, MANAGE_ORGANIZATIONS_ACTION)).thenReturn(true);

        given().auth()
               .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .expect()
               .statusCode(200)
               .when()
               .get(SECURE_PATH + "/organization/resource/" + PARENT_ORGANIZATION);

        verify(service).getResourcesLimits(PARENT_ORGANIZATION, anyInt(), anyLong());
        verify(subject).hasPermission(SystemDomain.DOMAIN_ID, null, MANAGE_ORGANIZATIONS_ACTION);
        verify(permissionsFilter, never()).checkManageResourcesPermission(any(), anyString(), anyString());
    }


    @Test(expectedExceptions = ForbiddenException.class,
          expectedExceptionsMessageRegExp = "The user does not have permission to perform this operation")
    public void shouldThrowForbiddenExceptionWhenRequestedUnknownMethod() throws Exception {
        final GenericResourceMethod mock = mock(GenericResourceMethod.class);
        Method unknownMethod = OrganizationResourcesService.class.getMethod("getServiceDescriptor");
        when(mock.getMethod()).thenReturn(unknownMethod);

        permissionsFilter.filter(mock, new Object[] {});
    }

    @Test(expectedExceptions = ForbiddenException.class)
    public void shouldThrowForbiddenExceptionOnTryingToCheckPermissionsOnParentLevelWhenRequestedOrganizationIsRoot() throws Exception {
        doCallRealMethod().when(permissionsFilter).checkManageResourcesPermission(any(), anyString(), anyString());

        permissionsFilter.checkManageResourcesPermission(subject, null, PARENT_ORGANIZATION);
    }

    @Test(expectedExceptions = ForbiddenException.class)
    public void shouldThrowForbiddenExceptionOnTryingToCheckPermissionsOnParentLevelWhenUserDoesNotHavePermission() throws Exception {
        when(subject.hasPermission(OrganizationDomain.DOMAIN_ID, PARENT_ORGANIZATION, OrganizationDomain.MANAGE_RESOURCES))
                .thenReturn(false);
        doCallRealMethod().when(permissionsFilter).checkManageResourcesPermission(any(), anyString(), anyString());

        permissionsFilter.checkManageResourcesPermission(subject, null, ORGANIZATION);

        verify(manager).getById(ORGANIZATION);
        verify(subject).hasPermission(OrganizationDomain.DOMAIN_ID, PARENT_ORGANIZATION, OrganizationDomain.MANAGE_RESOURCES);
    }

    @Test(expectedExceptions = ForbiddenException.class)
    public void shouldThrowForbiddenExceptionOnTryingToCheckPermissionsOnRequestedOrganizationLevelWhenUserDoesNotHavePermission()
            throws Exception {
        when(subject.hasPermission(OrganizationDomain.DOMAIN_ID, PARENT_ORGANIZATION, OrganizationDomain.MANAGE_RESOURCES))
                .thenReturn(false);
        doCallRealMethod().when(permissionsFilter).checkManageResourcesPermission(any(), anyString(), anyString());

        permissionsFilter.checkManageResourcesPermission(subject, PARENT_ORGANIZATION, null);

        verify(manager).getById(PARENT_ORGANIZATION);
        verify(subject).hasPermission(OrganizationDomain.DOMAIN_ID, PARENT_ORGANIZATION, OrganizationDomain.MANAGE_RESOURCES);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldThrowNotFoundExceptionExceptionOnTryingToCheckPermissionsForNonExistenceOrganization()
            throws Exception {
        doCallRealMethod().when(permissionsFilter).checkManageResourcesPermission(any(), anyString(), anyString());
        doThrow(new NotFoundException("")).when(manager).getById(anyString());

        permissionsFilter.checkManageResourcesPermission(subject, PARENT_ORGANIZATION, null);

        verify(manager).getById(PARENT_ORGANIZATION);
        verify(subject).hasPermission(OrganizationDomain.DOMAIN_ID, PARENT_ORGANIZATION, OrganizationDomain.MANAGE_RESOURCES);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldThrowNotFoundExceptionExceptionOnTryingToCheckPermissionsForNonExistenceSuborganization()
            throws Exception {
        doCallRealMethod().when(permissionsFilter).checkManageResourcesPermission(any(), anyString(), anyString());
        doThrow(new NotFoundException("")).when(manager).getById(anyString());

        permissionsFilter.checkManageResourcesPermission(subject, null, ORGANIZATION);

        verify(manager).getById(ORGANIZATION);
        verify(subject).hasPermission(OrganizationDomain.DOMAIN_ID, PARENT_ORGANIZATION, OrganizationDomain.MANAGE_RESOURCES);
    }

    @Filter
    public static class EnvironmentFilter implements RequestFilter {
        public void doFilter(GenericContainerRequest request) {
            EnvironmentContext.getCurrent().setSubject(subject);
        }
    }
}
