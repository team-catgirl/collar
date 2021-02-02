package team.catgirl.collar.security;

public interface Cypher {
    byte[] crypt(Identity recipient, byte[] bytes);

    byte[] decrypt(Identity sender, byte[] bytes);
}
