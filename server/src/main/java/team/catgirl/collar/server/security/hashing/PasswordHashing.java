package team.catgirl.collar.server.security.hashing;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * TODO: introduce salt
 */
public class PasswordHashing {
    public BCrypt.Hasher hasher() {
        return BCrypt.withDefaults();
    }

    public BCrypt.Verifyer verifyer() {
        return BCrypt.verifyer();
    }
}
