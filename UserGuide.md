# Introduction #

Calls to Facebook take time.  Sometimes they take painfully large amounts of time.  Even when Facebook is having a good day, the latency of each HTTP request quickly becomes unreasonable if you need to fetch several pieces of data.

Fortunately, Facebook provides mechanisms which allow you to combine several requests into a single call.  Unfortunately, the mechanisms are quirky, poorly documented, and interfere with the normal flow of your program code.  BatchFB solves this problem by letting you make numerous requests and queries in a natural syntax, then automatically optimizing them down to the minimum number of Facebook calls.  You never need to worry about multiquery or the Batch Graph API; the library handles this for you.

BatchFB future-proofs your code.  If Facebook implements additional batching facilities, a new version of batchfb can add these optimizations without you having to recompile your code.

In addition to getting data back from Facebook as quickly and efficiently as possible, BatchFB significantly improves the experience of working with the Facebook API from Java:

  * BatchFB maps JSON result sets to your custom types (User, Page, etc) using [Jackson](http://jackson.codehaus.org/), an extremely fast, highly customizable, widely-used parser.  By annotating your classes, you can map nearly any tangle of JSON that Facebook serves up onto strong types.

  * BatchFB unifies the three different error reporting formats that Facebook renders into a single, programmer-useful Exception hierarchy.  For example, if you try to post to a wall you don't have access to you will receive a `PermissionException`.

BatchFB is intended to be a "power-user's" interface to Facebook.  For a more novice-friendly Java library, see [RestFB](http://www.restfb.com/).

# Requirements #

  * You must include `batchfb.jar`, `jackson-core-asl-X.X.X.jar`, and `jackson-mapper-asl-X.X.X.jar` in your project.  These jars are included in the BatchFB distribution or you can acquire the [latest versions](http://wiki.fasterxml.com/JacksonDownload).  The jax-rs and jax-xc jars are not required for BatchFB.
  * If (and only if) you use the `FacebookCookie` class, you will need `commons-codec-X.X.jar`.

# The `Later<?>` Interface #

The meat of BatchFB is the `Batcher` interface and the `FacebookBatcher` implementation.  `Batcher` methods return not the value you are querying for, but a `Later<?>` object:

```java
public interface Batcher {
    ...
    public <T> Later<T> graph(String object, Class<T> type, Param... params);
    ...
    public <T> Later<List<T>> query(String fql, Class<T> type);
    ...
}
```

The `Later<?>` interface is what allows us to queue up several requests at once without issuing any actual calls to Facebook.  The interface is modeled on `java.concurrent.Future`, but doesn't throw irritating checked exceptions:

```java
public interface Later<T> {
    T get() throws FacebookException;
}
```

You can create numerous `Later<?>` objects, one for each request or query.  The first time **any** `get()` method is called, the entire batch will execute using the minimal number of calls to Facebook.

```java
Batcher batcher = new FacebookBatcher(accessToken);

Later<User> me = batcher.graph("me", User.class);
Later<User> bob = batcher.graph("bob", User.class);

// No calls to Facebook have been made yet.  The first get() on either Later<?> will trigger
// all data to be fetched in a single call using the Graph Batch API.
String myName = me.get().getName();
```

**Important**:  `Later<?>` objects which have been executed successfully are "frozen in time".  You can call `get()` repeatedly to obtain the result without triggering additional calls to Facebook or triggering the execution of subsequently created requests.  If the `get()` method produces an exception, it will **always** produce the same exception.  To repeat the call you must create a new request.

There is an exception to this:  Requests which produce `IOFacebookException` (indicating an error establishing the http connection to Facebook) will be retried when you call `get()` additional times.

# Mapping Results To Objects #

BatchFB uses [Jackson](http://jackson.codehaus.org/) to parse JSON results from Facebook and map them to Java objects.  There are a wide variety of ways to map JSON onto Java objects, so BatchFB does not attempt to hide Jackson from the user - in fact, you will need some level of familiarity with Jackson in order to use BatchFB to its fullest.

This section will try to provide a gentle introduction to Jackson.  It is not complete documentation; you can find that at the [Jackson website](http://wiki.fasterxml.com/JacksonDocumentation).  After reading this section, you may find it helpful to follow the [Jackson tutorial](http://wiki.fasterxml.com/JacksonInFiveMinutes).

## `JsonNode` ##

The simplest way to access Facebook data is to use `FacebookBatcher` methods that return Jackson's [ArrayNode](http://jackson.codehaus.org/1.5.3/javadoc/org/codehaus/jackson/node/ArrayNode.html) and [JsonNode](http://jackson.codehaus.org/1.5.3/javadoc/org/codehaus/jackson/JsonNode.html).  These are simple generic objects that work much like XML DOM nodes:

```java
Batcher batcher = new FacebookBatcher(accessToken);

Later<JsonNode> me = batcher.graph("me");
Later<ArrayNode> folks = batcher.query("SELECT name FROM user WHERE uid IN (123,456,789)");

String myName = me.get().get("name").getValueAsText();

for (JsonNode someone: folks.get()) {
    String name = someone.get("name").getValueAsText();
    ...
}
```

If you just need to select out a single field, these methods can be handy.  Note that there are two ways of navigating a tree of `JsonNode`s:

  * `JsonNode.get("blah")` will return a `JsonNode` for the dictionary key "blah", or null if there is no such structure in the original JSON.
  * `JsonNode.path("blah")` will always return a `JsonNode`, even if there is no "blah" in the source JSON.  The node will be a `MissingNode` which returns null for all the accessor methods like `getValueAsText()`.

The `path()` method is handy when the structure of the JSON is unknown or ambiguous:

```java
// Source JSON looks like this:  { name:"Fred", age:40 }
JsonNode user = fetchUser();

// These calls produce indistinguishable results:
String name1 = user.path("name").getValueAsText();
String name2 = user.get("name").getValueAsText();

// Navigating to nonexistant nodes will have different effects:
String religion1 = user.path("religion").getValueAsText();  // produces null
String religion2 = user.get("religion").getValueAsText(); // throws NullPointerException because get("religion") returned null
```

Also be aware of the difference between:

  * `JsonNode.getTextValue()`, which obtains the text of a node only if the node holds text.  Calling this method on a numeric node will return null.
  * `JsonNode.getValueAsText()`, which will produce the string version of the node content, no matter what kind of node it is.

You should generally use `getValueAsText()` whenever you want the text of a node.  Note that calling `toString()` on a `JsonNode` will usually produce JSON.

## Mapping With Annotations ##

Untyped `JsonNode`s and simple data binding can be handy for simple fetches but aren't particularly convenient when dealing with most of Facebook.  Usually you want results in the form of nice typed objects like `User`, `Page`, `Album`, `Event`, etc.

The bad news is that BatchFB does not provide these classes for you.  Facebook updates the structure of these objects frequently and we don't want to release new versions of BatchFB every two weeks.

The good news is that it is **very** easy to create these classes yourself, specially tailored to your business logic.

Jackson is designed to efficiently perform [data binding](http://wiki.fasterxml.com/JacksonDataBinding) to your POJO classes.  It does a reasonable job of guessing how to map properties out-of-the-box, but you can control the process by [annotating](http://wiki.fasterxml.com/JacksonAnnotations) your classes.  You can even control mapping to third-party classes that you don't have source code for using [mix-in annotations](http://wiki.fasterxml.com/JacksonMixInAnnotations)!

The Jackson documentation provides full details, but here is an example:

```java
public class User {
    /** Fields are mapped as-is */
    String name;

    /** Property setters work the way you would expect */
    public void setReligion(String value) {...}

    /** You can map arbitrary fields using annotations. This works on setters too. */
    @JsonProperty("relationship_status")
    String relationshipStatus;

    /** Nested objects are mapped correctly */
    @JsonProperty("current_location")
    public CurrentLocation currentLocation;
}
```

Using your classes is straightforward:

```java
Batcher batcher = new FacebookBatcher(accessToken);
Later<User> me = batcher.graph("me", User.class);
String myName = me.get().getName();
```

A bit of trivia for those paying close attention:  The Jackson methods that return `JsonNode` are really just calling the mapper with `JsonNode.class` as the desired output type!

## Generics and `TypeReference` ##

**Note**:  This is for very advanced users, you will probably not need to use the information in this section.

Mapping JSON to generified classes such as `List<User>` presents special problems.  Because of the way generics are implemented in Java, the generic type (in this case `User`) is eliminated from the class in a process called _erasure_.  This means you can't do something like this in Java:

```java
// Doesn't work!  List<Comment>.class is invalid Java syntax.
Later<List<Comment>> comments = batcher.graph(
    "something/producing/list", List<Comment>.class, new Param("object_id", 98423808305));
```

However, there are some ways of working around type erasure.  For instance, if you create a class that extends a generic class with concrete types, the new class can be introspected to discover the generics.  For example:

```java
class CommentList implements ArrayList<Comment> { /* nothing */ }
```

Java can introspect `CommentList` and discover that it is a list of `Comment` objects, so Jackson can populate your list with the correct objects while deserializing JSON.  While this is a workable approach, it would be unwieldy to have to create subclasses for every possible generic collection that you want to map.

Fortunately, Jackson provides a `TypeReference` class which makes this process syntactically more pleasant.  You can fetch comments using this call:

```java
// Works!
Later<List<Comment>> comments = batcher.graph(
    "something/producing/list", new TypeReference<List<Comment>(){}, new Param("object_id", 98423808305));
```

Notice that `new TypeReference<List<Comment>(){} ` (with the extra "{}") creates an inline anonymous class which extends your List and thus preserves the generic type information.  The `Batcher` provides provides methods that let you map to Jackson's `TypeReference`s as well as `Class`es.

If you are familiar with the Facebook Graph API, you may be thinking to yourself:  Wait!  There **aren't** any graph endpoints that produce a raw list!  This is true, and in fact you will never use typereferences of collection classes directly when working with Facebook's API.  However, this is the mechanism you will use if you use generic holder classes, such as `Paged` - see Edges and Paging below.

## The `ObjectMapper` ##

The core Jackson class involved in mapping JSON to Java objects is the [ObjectMapper](http://jackson.codehaus.org/1.5.3/javadoc/org/codehaus/jackson/map/ObjectMapper.html).  The `FacebookBatcher` creates an instance of `ObjectMapper` and uses it for all JSON parsing.  You won't generally need to call methods on the `ObjectMapper`, but there is an extensive number of configuration options available.  You can obtain the `ObjectMapper` easily:

```java
Batcher batcher = new FacebookBatcher(accessToken);
batcher.getMapper().configure(DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS, true);
```

# Edges and Paging #

Facebook Graph API calls that target edges (eg me/feed, me/events, etc) return results in a paged format that looks like this:

```json
{
    data: [ {...item1...}, {...item2...} ],
    paging: {
        previous: "somelongurl",
        next: "someotherlongurl"
    }
}
```

BatchFB provides two ways to work with this data.

## The `Paged` class ##

BatchFB provides a `Paged` class that makes it easier to map these classes.  Assuming you have an Event class already, you can fetch your events like this:

```java
Batcher batcher = new FacebookBatcher(accessToken);

Later<Paged<Event>> events = batcher.graph("me/events", new TypeReference<Paged<Event>>(){});
String firstEventName = events.get().getData().get(0).getName();
```

Note the use of Jackson's `TypeReference` to preserve generic type information for the mapping process.

## The `paged()` method ##

By calling the `Batcher.paged()` method, you get back a `PagedLater<?>` object which allows you to automatically enqueue calls to the previous and next pages:

```java
Batcher batcher = new FacebookBatcher(accessToken);

PagedLater<Post> feed = batcher.paged("me/home", Post.class);
for (Post post: feed.get()) {
    ...
}

feed = feed.next();
if (feed != null) {
    for (Post post: feed.get()) {
        ...
    }
}
```

You get the picture.  When you call `PagedLater<?>.previous()` or `PagedLater<?>.next()`, a request for the previous or next page is enqueued and a new `PagedLater<?>` is returned.  If there are no more pages, you will get null.

The `PagedLater<?>.get()` method always returns a `List` of the type of object you are paging across.

# Exceptions #

Error handling is somewhat erratic in Facebook's APIs.  There are three different error formats produced by the Graph API, and some errors which are programmatically useful (say, making a call to which the application does not permission) are poorly designated.  BatchFB attempts to address these issues by throwing a unified set of exceptions that distinguish the conditions that you are actually interested in as an application programmer.

## When Exceptions Are Thrown ##

First of all, you should be aware of when BatchFB will throw an exception due to Facebook error:
  * BatchFB does **not** throw exceptions when you create a `Later<?>` object.
  * BatchFB does **not** throw exceptions when it receives an error from the network or from Facebook.
  * BatchFB **does** throw exceptions when you call `Later<?>.get()` if there was an error obtaining that particular piece of data.

An example will illustrate the point:

```java
Batcher batcher = new FacebookBatcher(accessToken);

// queryFirst() just returns the first result
Later<User> dude = batcher.queryFirst("SELECT name FROM user WHERE uid = 12345", User.class);

// Uh oh, "Swimming" migrated from one ID to another ID
Later<Page> swimming = batcher.graph("114267748588304", Page.class);

// The first call to get() from any Later<?> will execute the current batch.

// This will produce the correct answer
String name = dude.get().getName();

// You can call this over and over without incurring new calls to FB
name = dude.get().getName();

// This will throw a PageMigratedException
try {
    swimming.get();
} catch (PageMigratedException ex) {
    log.error("Page id " + ex.getOldId() + " migrated to " + ex.getNewId());
}

// You can call this over and over and you will always get the same exception. BatchFB will
// not make new calls to Facebook.  You must recreate the request if you want to retry.
try {
    swimming.get();
} catch (PageMigratedException ex) {
    log.error("It will always produce this same exception!");
}
```

Note that if there is a network error, you will get a `IOFacebookException` and each call to `get()` will retry the call to Facebook.  Permanent errors that affect multiple requests batched into a single call (say, an error for the Graph Batch API call itself) will be thrown by every `Later<?>.get()` in the batch.

## Exception Hierarchy ##

All exceptions thrown by BatchFB extend `FacebookException`, which is a type of `RuntimeException`.  These are the exceptions currently thrown and what produces them:

  * `FacebookException` - root of the hierarchy, also thrown when an otherwise unrecognized error occurs.
    * `IOFacebookException` - thrown when there is a network error, a bad HTTP status code, or a problem parsing the JSON.
    * `OAuthException` - thrown when there is a problem with authentication (typically token expired)
    * `PermissionException` - thrown when you do something you're not allowed to, like post to a wall without having been granted extended permission.
    * `PageMigratedException` - thrown when requesting an ID that was migrated to another ID.

Because Facebook does not document error conditions and periodically changes them, the exact mapping of errors to exceptions is not an exact science.  Please report any undesirable behavior in BatchFB's issue tracker.  Feel free to request additional conditions; for example, you might wish to distinguish between HTTP errors and JSON parsing errors.

# Using BatchFB With Google Web Toolkit #

The BatchFB jar includes a GWT module so that the exceptions (and just the exceptions) can be used in client-side code.  For more detail, see [BatchFBWithGWT](BatchFBWithGWT.md).

# Performance Notes #

Facebook's batching mechanisms allow 20 graph requests to be batched at once.  A multiquery counts as a single graph request, so a single fetch may include:

  * 20 graph requests
OR
  * 19 graph requests and any number of FQL requests

BatchFB manages this for you so that you do not need to count requests yourself.  If you overflow the batch limit, BatchFB will issue multiple fetches.

You may find that large batches cause problems on platforms with short urlfetch timeout limits like Appengine.  You can call `FacebookBatcher.setMaxBatchSize()` to reduce the size of a group to something that completes in shorter time.  When parallel fetching is implemented, this may be a performance optimization - smaller batches executing in parallel may complete faster than a single large batch.  We shall see.