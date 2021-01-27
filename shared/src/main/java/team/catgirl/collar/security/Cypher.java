package team.catgirl.collar.security;

public interface Cypher {
    byte[] crypt(Identity identity, byte[] bytes);

    byte[] decrypt(Identity identity, byte[] bytes);
}
