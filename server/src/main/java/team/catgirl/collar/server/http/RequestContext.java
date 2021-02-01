package team.catgirl.collar.server.http;

import spark.Request;
import team.catgirl.collar.api.http.HttpException.UnauthorisedException;

import java.util.Objects;
import java.util.UUID;

public final class RequestContext {

    public static RequestContext ANON = new RequestContext(UUID.fromString(  "00000000-0000-0000-0000-000000000000"));

    public static RequestContext SERVER = new RequestContext(UUID.fromString("99999999-9999-9999-9999-999999999999"));

    public final UUID owner;

    public RequestContext(UUID owner) {
        this.owner = owner;
    }

    public void assertAnonymous() {
        if (!ANON.equals(this)) {
            throw new UnauthorisedException("caller must be anonymous");
        }
    }

    public void assertNotAnonymous() {
        if (ANON.equals(this)) {
            throw new UnauthorisedException("caller must not be anonymous");
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestContext that = (RequestContext) o;
        return owner.equals(that.owner);
    }

    public static RequestContext from(Request req) {
        return req.attribute("requestContext");
    }
}
