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
package com.codenvy.organization.spi.impl;

import com.codenvy.organization.shared.model.OrganizationResourcesLimit;
import com.codenvy.resource.model.Resource;
import com.codenvy.resource.spi.impl.ResourceImpl;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Data object for {@link OrganizationResourcesLimit}.
 *
 * @author Sergii Leschenko
 */
@Entity(name = "OrganizationResourcesLimit")
@NamedQueries(
        {
                @NamedQuery(name = "OrganizationResourcesLimit.get",
                            query = "SELECT limit " +
                                    "FROM OrganizationResourcesLimit limit " +
                                    "WHERE limit.organizationId = :organizationId"),
                @NamedQuery(name = "OrganizationResourcesLimit.getByParent",
                            query = "SELECT limit " +
                                    "FROM OrganizationResourcesLimit limit " +
                                    "WHERE limit.organization.parent = :parent"),
                @NamedQuery(name = "OrganizationResourcesLimit.getCountByParent",
                            query = "SELECT COUNT(limit) " +
                                    "FROM OrganizationResourcesLimit limit " +
                                    "WHERE limit.organization.parent = :parent")
        }
)
public class OrganizationResourcesLimitImpl implements OrganizationResourcesLimit {
    @Id
    private String organizationId;

    @PrimaryKeyJoinColumn
    private OrganizationImpl organization;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable
    private List<ResourceImpl> resources;

    public OrganizationResourcesLimitImpl() {
    }

    public OrganizationResourcesLimitImpl(OrganizationResourcesLimit freeResourcesLimit) {
        this(freeResourcesLimit.getOrganizationId(),
             freeResourcesLimit.getResources());
    }

    public OrganizationResourcesLimitImpl(String organizationId,
                                          List<? extends Resource> resources) {
        this.organizationId = organizationId;
        if (resources != null) {
            this.resources = resources.stream()
                                      .map(ResourceImpl::new)
                                      .collect(Collectors.toList());
        }
    }

    @Override
    public String getOrganizationId() {
        return organizationId;
    }

    @Override
    public List<ResourceImpl> getResources() {
        if (resources == null) {
            resources = new ArrayList<>();
        }
        return resources;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganizationResourcesLimitImpl)) return false;
        OrganizationResourcesLimitImpl that = (OrganizationResourcesLimitImpl)o;
        return Objects.equals(organizationId, that.organizationId) &&
               Objects.equals(getResources(), that.getResources());
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizationId, getResources());
    }

    @Override
    public String toString() {
        return "OrganizationResourcesLimitImpl{" +
               "organizationId='" + organizationId + '\'' +
               ", resources=" + resources +
               '}';
    }

    public void setResources(List<ResourceImpl> totalResources) {
        this.resources = totalResources;
    }
}
