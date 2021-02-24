package team.catgirl.collar.security;

import java.security.*;
import java.security.PublicKey;
import java.util.UUID;

public class ProfileIdentity {

    public final UUID profile;
    public final PublicKey publicKey;
    public final PrivateKey privateKey;

    public ProfileIdentity(UUID profile, PublicKey publicKey, PrivateKey privateKey) {
        this.profile = profile;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public ProfileIdentity create(UUID profile) {
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("AES/CBC/PKCS5PADDING");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        generator.initialize(256);
        KeyPair keyPair = generator.genKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        return new ProfileIdentity(profile, publicKey, privateKey);
    }
}
