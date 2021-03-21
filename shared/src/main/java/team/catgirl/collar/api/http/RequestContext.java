package team.catgirl.collar.api.http;

import com.google.common.collect.ImmutableSet;
import team.catgirl.collar.api.http.HttpException.UnauthorisedException;
import team.catgirl.collar.api.profiles.Profile;
import team.catgirl.collar.api.profiles.Role;
import team.catgirl.collar.security.ClientIdentity;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class RequestContext {

    public static RequestContext ANON = new RequestContext(UUID.fromString("00000000-0000-0000-0000-000000000000"), ImmutableSet.of());
    public static RequestContext SERVER = new RequestContext(UUID.fromString("99999999-9999-9999-9999-999999999999"), ImmutableSet.of());

    public final UUID owner;
    public final Set<Role> roles;

    public RequestContext(UUID owner, Set<Role> roles) {
        this.owner = owner;
        this.roles = roles;
    }

    public static RequestContext from(UUID profileId) {
        return new RequestContext(profileId, ImmutableSet.of(Role.PLAYER));
    }

    public static RequestContext from(ClientIdentity identity) {
        return new RequestContext(identity.owner, ImmutableSet.of(Role.PLAYER));
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

    public void assertCallerIs(UUID uuid) {
        if (!this.owner.equals(uuid)) {
            throw new UnauthorisedException("caller did not match profile id in request");
        }
    }

    public void assertHasRole(Role role) {
        if (!hasRole(role)) {
            throw new UnauthorisedException("caller did not have role " + role);
        }
    }

    public boolean hasRole(Role role) {
        return this.roles.contains(role);
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

    public boolean callerIs(UUID id) {
        return this.owner.equals(id);
    }
}
