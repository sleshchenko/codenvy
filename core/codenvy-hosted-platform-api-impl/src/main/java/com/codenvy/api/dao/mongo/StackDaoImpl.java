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
package com.codenvy.api.dao.mongo;

import com.codenvy.api.dao.mongo.util.StackAcl;
import com.codenvy.api.workspace.server.stack.StackAction;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

import org.bson.conversions.Bson;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.acl.AclEntryImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackImpl;
import org.eclipse.che.api.workspace.server.spi.StackDao;
import org.eclipse.che.commons.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.codenvy.api.dao.mongo.MongoUtil.handleWriteConflict;
import static com.mongodb.client.model.Filters.all;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.elemMatch;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.or;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Implementation mongo storage for {@link StackImpl}
 * <p>
 * <p>Stack collection document scheme:
 * <pre>
 * {
 *  "id": "stack1qerlsufdredfdre",
 *  "name": "Java",
 *  "description": "Default Java Stack with JDK 8, Maven and Tomcat.",
 *  "scope": "general",
 *  "creator": "userId12dsd",
 *  "tags": [
 *    "Java",
 *    "Maven",
 *    "JDK",
 *    "Tomcat",
 *    "Subversion",
 *    "Ubuntu",
 *    "Git"
 *  ],
 *  "workspaceConfig" : {
 *      "id" : "workspace123",
 *      "name" : "my-workspace",
 *      "description" : "This is workspace description",
 *      "owner" : "user123",
 *      "defaultEnv" : "dev-env",
 *      "commands" : [
 *          {
 *              "name" : "mci",
 *              "commandLine" : "maven clean install",
 *              "type" : "maven",
 *              "attributes" : [
 *                  {
 *                      "name" : "attribute1",
 *                      "value" : "value1"
 *                  }
 *               ]
 *          }
 *      ],
 *      "projects" : [
 *          {
 *              "name" : "my-project",
 *              "path" : "/path/to/project",
 *              "description" : "This is project description",
 *              "type" : "project-type",
 *              "source" : {
 *                  "type" : "storage-type",
 *                  "location" : "storage-location",
 *                  "parameters" : [
 *                      {
 *                          "name" : "parameter1",
 *                          "value" : "parameter-value"
 *                      }
 *                  ]
 *              },
 *              "modules": [
 *                  {
 *                      "name":"my-module",
 *                      "path":"/path/to/project/my-module",
 *                      "type":"maven",
 *                      "mixins" : [ "mixinType1", "mixinType2" ],
 *                      "description" : "This is module description",
 *                      "attributes" : [
 *                          {
 *                              "name" : "module-attribute-1",
 *                              "value" : [ "value1", "value2" ]
 *                          }
 *                      ]
 *                  }
 *              ],
 *              "mixins" : [ "mixinType1", "mixinType2" ],
 *              "attributes" : [
 *                  {
 *                      "name" : "project-attribute-1",
 *                      "value" : [ "value1", "value2" ]
 *                  }
 *              ]
 *          }
 *      ],
 *      "environments" : [
 *          {
 *              "name" : "dev-env",
 *              "recipe" : {
 *                  "type" : "dockerfile",
 *                  "script" : "FROM codenvy/jdk7\nCMD tail -f /dev/null"
 *              },
 *              "machineConfigs" : [
 *                  {
 *                      "isDev" : true,
 *                      "name" : "dev",
 *                      "type" : "machine-type",
 *                      "limits" : {
 *                          "ram" : 512
 *                      },
 *                      "source" : {
 *                          "type" : "recipe",
 *                          "location" : "recipe-url"
 *                      }
 *                  }
 *              ]
 *          }
 *      ],
 *      "attributes" : [
 *          {
 *              "name" : "attribute1",
 *              "value" : "value1"
 *          }
 *      ]
 * },
 *  "components": [
 *    {
 *      "name": "JDK",
 *      "version": "1.8.0_45"
 *    },
 *    {
 *      "name": "Maven",
 *    },
 *      "version": "3.2.2"
 *    {
 *      "name": "Tomcat",
 *      "version": "8.0.24"
 *    }
 *  ],
 *  "source": {
 *    "type": "image",
 *    "origin": "codenvy/ubuntu_jdk8"
 *  },
 *  "permissions": {
 *    "groups": [
 *      {
 *        "acl": [
 *          "read",
 *          "search"
 *        ],
 *        "name": "public"
 *      }
 *    ],
 *    "users": {}
 *  },
 *  "stackIcon": {
 *    "name": "type-java.svg",
 *    "mediaType": "image/svg+xml"
 *  }
 * }
 * </pre>
 *
 * @author Alexander Andrienko
 */
@Singleton
public class StackDaoImpl implements StackDao {

    private final MongoCollection<StackImpl> collection;
    private final MongoCollection<StackAcl>  acls;

    @Inject
    public StackDaoImpl(@Named("mongo.db.organization") MongoDatabase mongoDatabase,
                        @Named("organization.storage.db.stacks.collection") String collectionName) {
        collection = mongoDatabase.getCollection(collectionName, StackImpl.class);
        collection.createIndex(new BasicDBObject("creator", 1));
        //"organization.storage.db.stacks.acl.collection"
        this.acls = mongoDatabase.getCollection("stackAcls", StackAcl.class);
    }

    @Override
    public void create(StackImpl stack) throws ConflictException, ServerException {
        requireNonNull(stack, "Stack required");
        try {
            collection.insertOne(stack);
        } catch (MongoWriteException writeEx) {
            handleWriteConflict(writeEx, format("Stack with id '%s' already exists", stack.getId()));
        } catch (MongoException mongoEx) {
            throw new ServerException(mongoEx.getMessage(), mongoEx);
        }
    }

    @Override
    public StackImpl getById(String id) throws NotFoundException, ServerException {
        requireNonNull(id, "Stack id required");
        final FindIterable<StackImpl> findIt = collection.find(eq("_id", id));
        if (findIt.first() == null) {
            throw new NotFoundException(format("Stack with id '%s' was not found", id));
        }
        return findIt.first();
    }

    @Override
    public void remove(String id) throws ServerException {
        requireNonNull(id, "Stack id required");
        collection.findOneAndDelete(eq("_id", id));
    }

    @Override
    public void update(StackImpl update) throws NotFoundException, ServerException {
        requireNonNull(update, "Stack for updating required");
        requireNonNull(update.getId(), "Stack id required");
        try {
            if (collection.findOneAndReplace(eq("_id", update.getId()), update) == null) {
                throw new NotFoundException(format("Stack with id '%s' was not found", update.getId()));
            }
        } catch (MongoException mongoEx) {
            throw new ServerException(mongoEx.getMessage(), mongoEx);
        }
    }

    @Override
    public List<StackImpl> getByCreator(String creator, int skipCount, int maxItems) throws ServerException {
        requireNonNull(creator, "Creator required");
        try {
            return collection.find(eq("creator", creator))
                             .skip(skipCount)
                             .limit(maxItems)
                             .into(new ArrayList<>());
        } catch (MongoException mongoEx) {
            throw new ServerException("Impossible to retrieve stacks. ", mongoEx);
        }
    }

    @Override
    public List<StackImpl> searchStacks(@Nullable List<String> tags, int skipCount, int maxItems) throws ServerException {
        try {
            final Bson aclQuery = and(elemMatch("actions", in(StackAction.SEARCH.toString())),
                                      or(eq("user", "*"),
                                         eq("user", "user123")));

            final ArrayList<StackAcl> aclsResult = acls.find(aclQuery)
                                                       .into(new ArrayList<>());

            Bson tagQuery = null;
            if (tags != null && !tags.isEmpty()) {
                tagQuery = all("tags", tags);
            }

            List<StackImpl> result = new ArrayList<>();
            for (StackAcl stackAcl : aclsResult) {
                if (tags != null && !tags.isEmpty()) {
                    Bson query = eq("id", stackAcl.getStack());
                    if (tagQuery != null) {
                        query = and(query, tagQuery);
                    }
                    final StackImpl found = collection.find(query)
                                                      .first();
                    if (found != null) {
                        result.add(found);
                    }
                }
            }

            return result;
        } catch (MongoException mongoEx) {
            throw new ServerException("Impossible to retrieve stacks. ", mongoEx);
        }
    }

    @Override
    public void storeACL(String stack, AclEntryImpl acl) throws ServerException {
        try {
            acls.replaceOne(and(eq("stack", stack),
                                eq("user", acl.getUser())),
                            new StackAcl(stack, acl),
                            new UpdateOptions().upsert(true));
        } catch (MongoException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    public void removeACL(String stack, String user) throws ServerException {
        try {
            acls.deleteOne(and(eq("stack", stack),
                               eq("user", user)));
        } catch (MongoException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    public List<AclEntryImpl> getACLs(String stack) throws ServerException {
        try {
            return acls.find(eq("stack", stack))
                       .into(new ArrayList<>());
        } catch (MongoException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    public AclEntryImpl getACL(String stack, String user) throws ServerException, NotFoundException {
        AclEntryImpl found;
        try {
            found = acls.find(and(eq("stack", stack),
                                  eq("user", user)))
                        .first();
        } catch (MongoException e) {
            throw new ServerException(e.getMessage(), e);
        }

        if (found == null) {
            throw new NotFoundException(format("ACL for stack with id '%s' and user '%s' was not found", stack, user));
        }

        return found;
    }
}

