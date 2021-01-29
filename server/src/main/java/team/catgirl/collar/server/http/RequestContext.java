package team.catgirl.collar.server.http;

import spark.Request;
import team.catgirl.collar.server.http.HttpException.UnauthorisedException;

import java.util.Objects;
import java.util.UUID;

public final class RequestContext {

    public static RequestContext ANON = new RequestContext(UUID.fromString("00000000-0000-0000-0000-000000000000"));

    public final UUID profileId;

    public RequestContext(UUID profileId) {
        this.profileId = profileId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestContext that = (RequestContext) o;
        return profileId.equals(that.profileId);
    }

    public void assertAnonymous() {
        if (!ANON.equals(this)) {
            throw new UnauthorisedException("caller must be anonymous");
        }
    }

    public void assertIsUser() {
        if (ANON.equals(this)) {
            throw new UnauthorisedException("caller must not be anonymous");
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(profileId);
    }

    public static RequestContext from(Request req) {
        return req.attribute("requestContext");
    }
}
