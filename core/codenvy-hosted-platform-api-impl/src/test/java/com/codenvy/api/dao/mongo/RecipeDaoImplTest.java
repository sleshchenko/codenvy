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



import com.codenvy.api.dao.mongo.recipe.RecipeDaoImpl;

import static com.codenvy.api.dao.mongo.MongoUtil.asDBList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Tests for {@link RecipeDaoImpl}
 *
 * @author Eugene Voevodin
 */
public class RecipeDaoImplTest extends BaseDaoTest {
/*
TODO Fix tests
    private RecipeDaoImpl recipeDao;

    @BeforeMethod
    public void setUp() throws Exception {
        setUp("recipes");
        recipeDao = new RecipeDaoImpl(db, "recipes");
    }
    @Test
    public void shouldBeAbleToGetRecipeById() throws Exception {
        final Group group = new GroupImpl("workspace/admin", "workspace123", asList("read"));
        final Map<String, List<String>> users = singletonMap("user123", asList("read", "write"));
        final RecipeImpl example = new RecipeImpl().withId("recipe123")
                                                   .withName("name")
                                                   .withCreator("someone")
                                                   .withScript("script content")
                                                   .withType("script-type")
                                                   .withTags(asList("tag1", "tag2"))
                                                   .withPermissions(new PermissionsImpl(users, asList(group)));
        collection.save(recipeDao.asDBObject(example));

        final ManagedRecipe recipe = recipeDao.getById(example.getId());

        assertEquals(recipe, example);
    }

    @Test(expectedExceptions = NotFoundException.class,
          expectedExceptionsMessageRegExp = "Recipe with id 'fake' was not found")
    public void shouldThrowNotFoundExceptionWhenRecipeWithGivenIdWasNotFound() throws Exception {
        recipeDao.getById("fake");
    }

    @Test
    public void shouldBeAbleToCreateNewRecipe() throws Exception {
        final Group group = new GroupImpl("workspace/admin", "workspace123", asList("read"));
        final Map<String, List<String>> users = singletonMap("user123", asList("read", "write"));
        final RecipeImpl example = new RecipeImpl().withId("recipe123")
                                                   .withName("name")
                                                   .withCreator("someone")
                                                   .withScript("script content")
                                                   .withType("script-type")
                                                   .withTags(asList("tag1", "tag2"))
                                                   .withPermissions(new PermissionsImpl(users, asList(group)));

        recipeDao.create(example);

        final DBObject dbObj = collection.findOne(example.getId());
        assertEquals(FROM_OBJECT_TO_RECIPE_FUNCTION.apply(dbObj), example);
    }

    @Test(expectedExceptions = ConflictException.class,
          expectedExceptionsMessageRegExp = "Recipe with id 'recipe123' already exists")
    public void shouldThrowConflictExceptionWhenRecipeWithSameIdAsGivenRecipeIdAlreadyExists() throws Exception {
        collection.save(new BasicDBObject("_id", "recipe123"));

        recipeDao.create(new RecipeImpl().withId("recipe123"));
    }

    @Test
    public void shouldBeAbleToGetRecipesByCreator() throws Exception {
        final RecipeImpl example = new RecipeImpl().withId("recipe123")
                                                   .withName("name")
                                                   .withCreator("someone")
                                                   .withScript("script content")
                                                   .withType("script-type")
                                                   .withTags(asList("tag1", "tag2"));
        final RecipeImpl example2 = copy(example).withId("recipe234");
        final RecipeImpl example3 = copy(example).withId("recipe345").withCreator("other-user");
        collection.save(recipeDao.asDBObject(example));
        collection.save(recipeDao.asDBObject(example2));
        collection.save(recipeDao.asDBObject(example3));

        List<ManagedRecipe> recipes = recipeDao.getByCreator("someone", 0, 10);
        assertEquals(new HashSet<>(recipes), new HashSet<>(asList(example, copy(example2))));

        recipes = recipeDao.getByCreator("other-user", 0, 10);
        assertEquals(recipes, asList(example3));
    }

    @Test
    public void shouldBeAbleToGetRecipesByCreatorWithLimitAndSkipCount() throws Exception {
        final RecipeImpl example = new RecipeImpl().withId("recipe123")
                                                   .withName("name")
                                                   .withCreator("someone")
                                                   .withScript("script content")
                                                   .withType("script-type")
                                                   .withTags(asList("tag1", "tag2"));
        for (int i = 0; i < 15; i++) {
            collection.save(recipeDao.asDBObject(copy(example).withId(Integer.toString(i))));
        }

        final List<ManagedRecipe> recipes = recipeDao.getByCreator("someone", 5, 10);
        assertEquals(recipes.size(), 10);
        final Set<String> ids = FluentIterable.from(recipes)
                                              .transform(new Function<ManagedRecipe, String>() {
                                                  @Nullable
                                                  @Override
                                                  public String apply(ManagedRecipe recipe) {
                                                      return recipe.getId();
                                                  }
                                              }).toSet();
        assertEquals(ids, new HashSet<>(asList("5", "6", "7", "8", "9", "10", "11", "12", "13", "14")));
    }

    @Test
    public void shouldReturnEmptyRecipesListWhenFoundRecipesCountIsFewerThanSkipCount() throws Exception {
        final RecipeImpl example = new RecipeImpl().withId("recipe123")
                                                   .withName("name")
                                                   .withCreator("someone")
                                                   .withScript("script content")
                                                   .withType("script-type")
                                                   .withTags(asList("tag1", "tag2"));
        collection.save(recipeDao.asDBObject(example));

        final List<ManagedRecipe> recipes = recipeDao.getByCreator("someone", 5, 10);

        assertEquals(recipes.size(), 0);
    }

    @Test
    public void shouldBeRemoveRecipeById() throws Exception {
        final RecipeImpl example = new RecipeImpl().withId("recipe123")
                                                   .withName("name")
                                                   .withCreator("someone")
                                                   .withScript("script content")
                                                   .withType("script-type")
                                                   .withTags(asList("tag1", "tag2"));
        collection.save(recipeDao.asDBObject(example));

        recipeDao.remove(example.getId());

        assertNull(collection.findOne(example.getId()));
    }

    @Test
    public void shouldBeAbleToUpdateRecipe() throws Exception {
        final Group group = new GroupImpl("workspace/admin", "workspace123", asList("read"));
        final Map<String, List<String>> users = new HashMap<>();
        users.put("user123", asList("read", "write"));
        final RecipeImpl example = new RecipeImpl().withId("recipe123")
                                                   .withName("name")
                                                   .withCreator("someone")
                                                   .withScript("script content")
                                                   .withType("script-type")
                                                   .withTags(new ArrayList<>(asList("tag1", "tag2")))
                                                   .withPermissions(new PermissionsImpl(users, new ArrayList<>(asList(group))));
        collection.save(recipeDao.asDBObject(example));
        //preparing update
        example.withName("new-name")
               .withType("new-type")
               .withScript("new-script")
               .withTags(asList("new-tags"));
        example.getPermissions().getUsers().put("user234", asList("read"));
        final PermissionsImpl permissions = (PermissionsImpl)example.getPermissions();
        permissions.getGroups().add(new GroupImpl("workspace/developer", "workspace123", asList("read")));

        recipeDao.update(example);

        assertEquals(FROM_OBJECT_TO_RECIPE_FUNCTION.apply(collection.findOne(example.getId())), example);
    }

    @Test(expectedExceptions = NotFoundException.class,
          expectedExceptionsMessageRegExp = "Recipe with id 'fake' was not found")
    public void shouldThrowNotFoundExceptionIfRecipeWithIdInUpdateWasNotFound() throws Exception {
        recipeDao.update(new RecipeImpl().withId("fake"));
    }

    @Test
    public void shouldBeAbleToSearchRecipesByTagsAndType() throws Exception {
        final Group group = new GroupImpl("public", "null", asList("read", "search"));
        final RecipeImpl example = new RecipeImpl().withId("recipe123")
                                                   .withName("name")
                                                   .withCreator("someone")
                                                   .withScript("script content")
                                                   .withType("script-type")
                                                   .withTags(asList("tag1", "tag2"))
                                                   .withPermissions(new PermissionsImpl(null, asList(group)));
        final RecipeImpl example2 = copy(example).withId("recipe234")
                                                 .withTags(asList("tag1", "tag2", "tag3"));
        final RecipeImpl example3 = copy(example).withId("recipe345")
                                                 .withTags(asList("tag1"));
        final RecipeImpl example4 = copy(example).withId("recipe456")
                                                 .withType("another type")
                                                 .withPermissions(null);
        collection.save(recipeDao.asDBObject(example));
        collection.save(recipeDao.asDBObject(example2));
        collection.save(recipeDao.asDBObject(example3));
        collection.save(recipeDao.asDBObject(example4));

        final List<ManagedRecipe> recipes = recipeDao.search(asList("tag1", "tag2"), "script-type", 0, 10);

        assertEquals(new HashSet<>(recipes), new HashSet<>(asList(example, example2)));
    }

    @Test
    public void shouldBeAbleToSearchRecipesByTags() throws Exception {
        final Group group = new GroupImpl("public", "null", asList("read", "search"));
        final RecipeImpl example = new RecipeImpl().withId("recipe123")
                                                   .withName("name")
                                                   .withCreator("someone")
                                                   .withScript("script content")
                                                   .withType("script-type")
                                                   .withTags(asList("tag1", "tag2"))
                                                   .withPermissions(new PermissionsImpl(null, asList(group)));
        final RecipeImpl example2 = copy(example).withId("recipe234")
                                                 .withTags(asList("tag1", "tag2", "tag3"));
        final RecipeImpl example3 = copy(example).withId("recipe345")
                                                 .withTags(asList("tag1"));
        final RecipeImpl example4 = copy(example).withId("recipe456")
                                                 .withType("another type");
        collection.save(recipeDao.asDBObject(example));
        collection.save(recipeDao.asDBObject(example2));
        collection.save(recipeDao.asDBObject(example3));
        collection.save(recipeDao.asDBObject(example4));

        final List<ManagedRecipe> recipes = recipeDao.search(asList("tag1", "tag2"), null, 0, 10);

        assertEquals(new HashSet<>(recipes), new HashSet<>(asList(example, example2, example4)));
    }

    @Test
    public void shouldConvertRecipeToDBObject() {
        final Group group = new GroupImpl("workspace/admin", "workspace", asList("read", "write"));
        final RecipeImpl example = new RecipeImpl().withId("recipe123")
                                                   .withName("name")
                                                   .withCreator("someone")
                                                   .withScript("script content")
                                                   .withType("script-type")
                                                   .withTags(asList("tag1", "tag2"))
                                                   .withPermissions(new PermissionsImpl(singletonMap("user-id", singletonList("read")),
                                                                                        singletonList(group)));

        final BasicDBObject recipeObj = recipeDao.asDBObject(example);

        assertEquals(recipeObj.getString("_id"), example.getId());
        assertEquals(recipeObj.getString("name"), example.getName());
        assertEquals(recipeObj.getString("creator"), example.getCreator());
        assertEquals(recipeObj.getString("script"), example.getScript());
        assertEquals(recipeObj.getString("type"), example.getType());
        assertEquals(recipeObj.get("tags"), asDBList(example.getTags()));

        final BasicDBObject permissionsObj = (BasicDBObject)recipeObj.get("permissions");
        final BasicDBObject usersObj = (BasicDBObject)permissionsObj.get("users");
        assertEquals(usersObj.size(), 1);
        assertEquals(usersObj.get("user-id"), asDBList(singletonList("read")));

        final BasicDBList groupsObj = (BasicDBList)permissionsObj.get("groups");
        assertEquals(groupsObj.size(), 1);

        final BasicDBObject groupObj = (BasicDBObject)groupsObj.get(0);
        assertEquals(groupObj.getString("name"), "workspace/admin");
        assertEquals(groupObj.getString("unit"), "workspace");
        assertEquals(groupObj.get("acl"), asDBList(asList("read", "write")));
    }

    @Test
    public void shouldConvertDBObjectToRecipe() {
        final BasicDBObject dbObject = new BasicDBObject().append("_id", "recipe123")
                                                          .append("name", "name123")
                                                          .append("creator", "creator")
                                                          .append("script", "script")
                                                          .append("type", "type")
                                                          .append("tags", asDBList(asList("tag1", "tag2")));
        final BasicDBList groups = asDBList(singletonList(new BasicDBObject().append("name", "workspace/admin")
                                                                             .append("unit", "workspace")
                                                                             .append("acl", asDBList(asList("read", "write")))));
        dbObject.append("permissions", new BasicDBObject().append("users", new BasicDBObject("userId", asDBList(asList("read", "write"))))
                                                          .append("groups", groups));

        final ManagedRecipe recipe = FROM_OBJECT_TO_RECIPE_FUNCTION.apply(dbObject);
        assertNotNull(recipe);
        assertEquals(recipe.getId(), "recipe123");
        assertEquals(recipe.getName(), "name123");
        assertEquals(recipe.getCreator(), "creator");
        assertEquals(recipe.getScript(), "script");
        assertEquals(recipe.getType(), "type");
        assertEquals(recipe.getTags(), asList("tag1", "tag2"));
        assertEquals(recipe.getPermissions().getUsers(), singletonMap("userId", asList("read", "write")));
        assertEquals(recipe.getPermissions().getGroups(), singletonList(new GroupImpl("workspace/admin",
                                                                                      "workspace",
                                                                                      asList("read", "write"))));
    }

    private RecipeImpl copy(RecipeImpl recipe) {
        return new RecipeImpl().withId(recipe.getId())
                               .withCreator(recipe.getCreator())
                               .withType(recipe.getType())
                               .withScript(recipe.getScript())
                               .withTags(recipe.getTags())
                               .withPermissions(recipe.getPermissions());
    }
    */
}
