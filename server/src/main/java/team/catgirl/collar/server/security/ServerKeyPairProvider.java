package team.catgirl.collar.server.security;

import team.catgirl.collar.security.KeyPair;

import java.util.function.Supplier;

public interface ServerKeyPairProvider extends Supplier<KeyPair> {
}
