package team.catgirl.collar.server.security.hashing;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class TokenHashing {
    public byte[] encode(byte[] bytes) {
        return BCrypt.withDefaults().hash(12, bytes);
    }
}
