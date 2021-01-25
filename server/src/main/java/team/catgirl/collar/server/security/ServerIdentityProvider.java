package team.catgirl.collar.server.security;

import team.catgirl.collar.security.ServerIdentity;

public interface ServerIdentityProvider {
    ServerIdentity getIdentity();
}
