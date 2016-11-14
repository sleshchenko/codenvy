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

import com.codenvy.resource.api.usage.ResourceServicesPermissionsChecker;
import com.google.common.collect.ImmutableSet;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.everrest.assured.EverrestJetty;
import org.everrest.core.Filter;
import org.everrest.core.GenericContainerRequest;
import org.everrest.core.RequestFilter;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jayway.restassured.RestAssured.given;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_NAME;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_PASSWORD;
import static org.everrest.assured.JettyHttpServer.SECURE_PATH;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link LicenseServicePermissionFilter}
 *
 * @author Sergii Leschenko
 */
@Listeners({EverrestJetty.class, MockitoTestNGListener.class})
public class LicenseServicePermissionFilterTest {
    @SuppressWarnings("unused")
    private static final ApiExceptionMapper MAPPER = new ApiExceptionMapper();
    @SuppressWarnings("unused")
    private static final EnvironmentFilter  FILTER = new EnvironmentFilter();

    @Mock
    private AccountManager accountManager;

    @Mock
    private Account account;

    @Mock
    private LicenseService service;

    @Mock
    private static Subject subject;

    @Mock
    private ResourceServicesPermissionsChecker checker;

    private LicenseServicePermissionFilter filter;

    @BeforeMethod
    public void setUp() throws Exception {
        when(accountManager.getById(any())).thenReturn(account);

        when(checker.getAccountType()).thenReturn("test");
        when(account.getType()).thenReturn("test");

        filter = new LicenseServicePermissionFilter(accountManager,
                                                    ImmutableSet.of(checker));
    }

    @Test
    public void shouldTestThatAllPublicMethodsAreCoveredByPermissionsFilter() throws Exception {
        //given
        final List<String> collect = Stream.of(LicenseService.class.getDeclaredMethods())
                                           .filter(method -> Modifier.isPublic(method.getModifiers()))
                                           .map(Method::getName)
                                           .collect(Collectors.toList());

        //then
        assertEquals(collect.size(), 1);
        assertTrue(collect.contains(LicenseServicePermissionFilter.GET_LICENSE_METHOD));
    }

    @Test
    public void shouldCheckPermissionsOnGettingLicense() throws Exception {
        given().auth()
               .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .expect()
               .statusCode(204)
               .when()
               .get(SECURE_PATH + "/license/account123");

        verify(checker).checkPermissions("account123");
        verify(service).getLicense("account123");
    }

    @Test(dataProvider = "coveredPaths")
    public void shouldDenyRequestWhenUserDoesNotHasPermissionsToSeeLicense(String path) throws Exception {
        doThrow(new ForbiddenException("Forbidden")).when(checker).checkPermissions(any());

        given().auth()
               .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .expect()
               .statusCode(403)
               .when()
               .get(SECURE_PATH + path);

        verify(checker).checkPermissions("account123");
    }

    @Test(dataProvider = "coveredPaths")
    public void shouldDenyRequestThereIsNotPermissionCheckerWhenUserDoesNotHasPermissionsToSeeLicense(String path) throws Exception {
        when(account.getType()).thenReturn("unknown");

        given().auth()
               .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
               .expect()
               .statusCode(403)
               .when()
               .get(SECURE_PATH + path);
    }

    @DataProvider(name = "coveredPaths")
    public Object[][] pathsProvider() {
        return new Object[][] {
                {"/license/account123"}
        };
    }

    @Filter
    public static class EnvironmentFilter implements RequestFilter {
        public void doFilter(GenericContainerRequest request) {
            EnvironmentContext.getCurrent().setSubject(subject);
        }
    }
}
