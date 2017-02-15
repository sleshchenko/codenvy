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
package com.codenvy.organization.spi.impl;

import com.codenvy.organization.shared.model.OrganizationResources;
import com.codenvy.resource.model.Resource;
import com.codenvy.resource.spi.impl.ResourceImpl;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Data object for {@link OrganizationResources}.
 *
 * @author Sergii Leschenko
 */
@Entity(name = "OrganizationResources")
@NamedQueries(
        {
                @NamedQuery(name = "OrganizationResources.get",
                            query = "SELECT r " +
                                    "FROM OrganizationResources r " +
                                    "WHERE r.organizationId = :organizationId"),
                @NamedQuery(name = "OrganizationResources.getByParent",
                            query = "SELECT r " +
                                    "FROM OrganizationResources r " +
                                    "WHERE r.organization.parent = :parent"),
                @NamedQuery(name = "OrganizationResources.getCountByParent",
                            query = "SELECT COUNT(r) " +
                                    "FROM OrganizationResources r " +
                                    "WHERE r.organization.parent = :parent")
        }
)
@Table(name = "organization_resources")
public class OrganizationResourcesImpl implements OrganizationResources {
    @Id
    @Column(name = "organization_id")
    private String organizationId;

    @PrimaryKeyJoinColumn
    private OrganizationImpl organization;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "organization_resources_reserved",
               joinColumns = @JoinColumn(name = "organization_resources_id"),
               inverseJoinColumns = @JoinColumn(name = "resource_id"))
    private List<ResourceImpl> reservedResources;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "organization_resources_caps",
               joinColumns = @JoinColumn(name = "organization_resources_id"),
               inverseJoinColumns = @JoinColumn(name = "resource_id"))
    private List<ResourceImpl> resourcesCaps;

    public OrganizationResourcesImpl() {
    }

    public OrganizationResourcesImpl(OrganizationResources organizationResource) {
        this(organizationResource.getOrganizationId(),
             organizationResource.getReservedResources(),
             organizationResource.getResourcesCap());
    }

    public OrganizationResourcesImpl(String organizationId,
                                     List<? extends Resource> reservedResources,
                                     List<? extends Resource> resourcesCaps) {
        this.organizationId = organizationId;
        if (resourcesCaps != null) {
            this.resourcesCaps = resourcesCaps.stream()
                                              .map(ResourceImpl::new)
                                              .collect(Collectors.toList());
        }
        if (reservedResources != null) {
            this.reservedResources = reservedResources.stream()
                                                      .map(ResourceImpl::new)
                                                      .collect(Collectors.toList());
        }
    }

    @Override
    public String getOrganizationId() {
        return organizationId;
    }

    @Override
    public List<ResourceImpl> getReservedResources() {
        if (reservedResources == null) {
            reservedResources = new ArrayList<>();
        }
        return reservedResources;
    }

    @Override
    public List<ResourceImpl> getResourcesCap() {
        if (resourcesCaps == null) {
            resourcesCaps = new ArrayList<>();
        }
        return resourcesCaps;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OrganizationResourcesImpl)) {
            return false;
        }
        final OrganizationResourcesImpl that = (OrganizationResourcesImpl)obj;
        return Objects.equals(organizationId, that.organizationId)
               && Objects.equals(organization, that.organization)
               && getResourcesCap().equals(that.getResourcesCap())
               && getReservedResources().equals(that.getReservedResources());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(organizationId);
        hash = 31 * hash + Objects.hashCode(organization);
        hash = 31 * hash + getResourcesCap().hashCode();
        hash = 31 * hash + getReservedResources().hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "OrganizationResourcesImpl{" +
               "organizationId='" + organizationId + '\'' +
               ", organization=" + organization +
               ", reservedResources=" + getReservedResources() +
               ", resourcesCap=" + getResourcesCap() +
               '}';
    }
}
