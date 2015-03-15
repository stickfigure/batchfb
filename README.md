# batchfb

BatchFB is a Java library that provides a power-user's interface to Facebook's Graph API.

  * Automatically groups requests into the minimum number of remote calls to Facebook:
    * FQL queries are combined into a single multiquery.
    * FQL and Graph requests are combined into a single Graph Batch API request.
  * Provides a unified, coherent, and programmer-friendly set of exceptions from Graph and FQL calls.
  * Deeply integrates with [Jackson](http://jackson.codehaus.org/) to map Facebook results to your typesafe objects.

Additional bonus for Google App Engine users:  Oversized batches are automatically split and executed in parallel, rather than serially.  GAE is not required to use BatchFB.

Before you read the [UserGuide](UserGuide.md), here is a quick example:

```java
/** You write your own Jackson user mapping for the pieces you care about */
public class User {
    long uid;
    @JsonProperty("first_name") String firstName;
    String pic_square;
    String timezone;
}

Batcher batcher = new FacebookBatcher(accessToken);

Later<User> me = batcher.graph("me", User.class);
Later<User> mark = batcher.graph("markzuckerberg", User.class);
Later<List<User>> myFriends = batcher.query(
    "SELECT uid, first_name, pic_square FROM user WHERE uid IN" +
    "(SELECT uid2 FROM friend WHERE uid1 = " + myId + ")", User.class);
Later<User> bob = batcher.queryFirst("SELECT timezone FROM user WHERE uid = " + bobsId, User.class);
PagedLater<Post> feed = batcher.paged("me/feed", Post.class);

// No calls to Facebook have been made yet.  The following get() will execute the
// whole batch as a single Facebook call.
String timezone = bob.get().timezone;

// You can just get simple values forcing immediate execution of the batch at any time.
User ivan = batcher.graph("ivan", User.class).get();
```

**Note**:  BatchFB requires that you understand both the Facebook API and Jackson's [annotation-based configuration](http://wiki.fasterxml.com/JacksonAnnotations).
For a more novice-friendly Java interface to Facebook, try [RestFB](http://www.restfb.com).

Now, read the [UserGuide](UserGuide.md).

You may also be interested in the [ReleaseNotes](ReleaseNotes.md).
