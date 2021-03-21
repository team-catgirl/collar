package team.catgirl.collar.server.security.hashing;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class PasswordHashing {

    private final byte[] salt;

    public PasswordHashing(String salt) {
        this.salt = salt.getBytes(StandardCharsets.UTF_8);
    }

    public String hash(String input) {
        byte[] hash = BCrypt.withDefaults().hash(12, salt, input.getBytes(StandardCharsets.UTF_8));
        return new String(hash, StandardCharsets.UTF_8);
    }

    public boolean verify(char[] password, char[] hash) {
        return BCrypt.verifyer().verify(password, hash).verified;
    }
}
