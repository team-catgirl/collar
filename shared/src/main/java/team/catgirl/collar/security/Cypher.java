package team.catgirl.collar.security;

public interface Cypher {
    byte[] crypt(ServerIdentity serverIdentity, byte[] bytes);

    byte[] decrypt(ServerIdentity serverIdentity, byte[] bytes);
}
