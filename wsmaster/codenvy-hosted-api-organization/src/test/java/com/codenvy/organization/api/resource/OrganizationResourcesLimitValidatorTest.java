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
import com.codenvy.resource.api.free.ResourceValidator;
import com.codenvy.resource.shared.dto.ResourceDto;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;

/**
 * Tests for {@link OrganizationResourcesLimitValidator}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class OrganizationResourcesLimitValidatorTest {
    @Mock
    private ResourceValidator resourceValidator;

    @InjectMocks
    private OrganizationResourcesLimitValidator limitValidator;

    @Test(expectedExceptions = BadRequestException.class,
          expectedExceptionsMessageRegExp = "Missed organization resources limit description.")
    public void shouldThrowBadRequestExceptionWhenOrganizationResourcesIsNull() throws Exception {
        //when
        limitValidator.check(null);
    }

    @Test(expectedExceptions = BadRequestException.class,
          expectedExceptionsMessageRegExp = "Missed organization id.")
    public void shouldThrowBadRequestExceptionWhenOrganizationIdIsMissed() throws Exception {
        //when
        limitValidator.check(DtoFactory.newDto(OrganizationResourcesLimitDto.class)
                                       .withResources(singletonList(DtoFactory.newDto(ResourceDto.class)
                                                                              .withType("test")
                                                                              .withUnit("mb")
                                                                              .withAmount(1230))));
    }

    @Test(expectedExceptions = BadRequestException.class,
          expectedExceptionsMessageRegExp = "invalid resource")
    public void shouldRethrowBadRequestExceptionWhenThereIsAnyInvalidResource() throws Exception {
        //given
        Mockito.doNothing()
               .doThrow(new BadRequestException("invalid resource"))
               .when(resourceValidator)
               .check(any());

        //when
        limitValidator.check(DtoFactory.newDto(OrganizationResourcesLimitDto.class)
                                       .withOrganizationId("organization123")
                                       .withResources(Arrays.asList(DtoFactory.newDto(ResourceDto.class)
                                                                              .withType("test")
                                                                              .withUnit("mb")
                                                                              .withAmount(1230),
                                                                    DtoFactory.newDto(ResourceDto.class)
                                                                              .withType("test2")
                                                                              .withUnit("mb")
                                                                              .withAmount(3214))));
    }

    @Test
    public void shouldNotThrowAnyExceptionWhenOrganizationResourcesLimitIsValid() throws Exception {
        //when
        limitValidator.check(DtoFactory.newDto(OrganizationResourcesLimitDto.class)
                                       .withOrganizationId("organization123")
                                       .withResources(singletonList(DtoFactory.newDto(ResourceDto.class)
                                                                              .withType("test")
                                                                              .withUnit("mb")
                                                                              .withAmount(1230))));
    }
}
