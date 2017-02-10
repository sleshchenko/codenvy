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
package com.codenvy.resource.api.free;

import com.codenvy.resource.model.Resource;
import com.codenvy.resource.model.ResourceType;
import com.codenvy.resource.shared.dto.ResourceDto;

import org.eclipse.che.api.core.BadRequestException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * Utils for validation of {@link Resource}.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class ResourceValidator {
    private final Map<String, Set<String>> resourcesTypesToUnits;
    private final Map<String, String>      resourcesTypesToDefaultUnit;

    @Inject
    public ResourceValidator(Set<ResourceType> supportedResources) {
        this.resourcesTypesToUnits = supportedResources.stream()
                                                       .collect(toMap(ResourceType::getId, ResourceType::getSupportedUnits));
        this.resourcesTypesToDefaultUnit = supportedResources.stream()
                                                             .collect(toMap(ResourceType::getId, ResourceType::getDefaultUnit));
    }

    /**
     * Validates given {@code resource}
     *
     * <p>{@link ResourceDto#getUnit()} can be null then
     * {@link ResourceType#getDefaultUnit() default unit} of {@link ResourceDto#getType() specified type} will be set.
     *
     * @param resource
     *         resource to validate
     * @throws BadRequestException
     *         when {@code resource} is null
     * @throws BadRequestException
     *         when {@code resource} has non supported type
     * @throws BadRequestException
     *         when {@code resource} has non supported unit
     */
    public void validate(ResourceDto resource) throws BadRequestException {
        if (resource == null) {
            throw new BadRequestException("Missed resource");
        }

        final Set<String> units = resourcesTypesToUnits.get(resource.getType());

        if (units == null) {
            throw new BadRequestException("Specified resources type '" + resource.getType() + "' is not supported");
        }

        if (resource.getUnit() == null) {
            resource.setUnit(resourcesTypesToDefaultUnit.get(resource.getType()));
        } else {
            if (!units.contains(resource.getUnit())) {
                throw new BadRequestException("Specified resources type '" + resource.getType() + "' support only following units: " +
                                              units.stream()
                                                   .collect(Collectors.joining(", ")));
            }
        }

        if (resource.getAmount() < 0) {
            throw new BadRequestException("Resources with type '" + resource.getType() + "' has negative amount");
        }
    }
}
