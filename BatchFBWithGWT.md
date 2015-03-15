NOTE:  BatchFB is a server-side framework.  You cannot use BatchFB to make Facebook calls from GWT clients.

BatchFB produces application-meaningful exceptions, some of which are useful to pass on to GWT clients through GWT-RPC.  For example, if a user attempts an action that posts to his/her wall but the application does not have the _stream\_publish_ extended permission, the resulting `PermissionException` should bubble all the way up to the client so the user can be prompted to grant the new permission.

To make this easier, BatchFB provides its exception hierarchy as a GWT module.  By including this module and declaring `throws FacebookException` on your GWT-RPC methods, your client can process errors without laboriously catching and rethrowing application-specific exceptions on the server.

First, inherit BatchFB's GWT module in your Module.gwt.xml:

```xml
    <inherits name="com.googlecode.batchfb.BatchFB" />
```

Now, declare `throws FacebookException` on the relevant GWT-RPC methods.  You must explicitly declare the throws cause even though `FacebookException` is a `RuntimeException`; this informs the GWT compiler that it should include the exception code in the RPC layer rather than interpreting the exception as `UndeclaredThrowableException`.

```java
public interface MyGWTService {
    public void postToMyWall(String message) throws FacebookException
}
```

Of course, your client code should handle the `FacebookException` (or `PermissionException`).