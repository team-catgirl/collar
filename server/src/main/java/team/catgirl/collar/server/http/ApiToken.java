package team.catgirl.collar.server.http;

import com.google.common.io.BaseEncoding;
import team.catgirl.collar.server.services.authentication.TokenCrypter;

import java.io.*;
import java.util.UUID;

/**
 * Used for authorizing API calls
 */
public class ApiToken {

    private static final int VERSION = 1;

    public final UUID profileId;
    public final long expiresAt;

    public ApiToken(UUID profileId, long expiresAt) {
        this.profileId = profileId;
        this.expiresAt = expiresAt;
    }

    public String serialize(TokenCrypter crypter) throws IOException {
        try (ByteArrayOutputStream boos = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(boos)) {
                oos.write(VERSION);
                oos.writeUTF(profileId.toString());
                oos.writeLong(expiresAt);
            }
            return BaseEncoding.base64Url().encode(crypter.crypt(boos.toByteArray()));
        }
    }

    public static ApiToken deserialize(TokenCrypter crypter, String token) throws IOException {
        byte[] bytes = BaseEncoding.base64Url().decode(token);
        try (ByteArrayInputStream bbis = new ByteArrayInputStream(crypter.decrypt(bytes))) {
            try (ObjectInputStream bis = new ObjectInputStream(bbis)) {
                int version = bis.read();
                String uuidAsString;
                long expiresAt;
                switch (version) {
                    case 1:
                        uuidAsString = bis.readUTF();
                        expiresAt = bis.readLong();
                        break;
                    default:
                        throw new IllegalStateException("unknown version " + version);
                }
                return new ApiToken(UUID.fromString(uuidAsString), expiresAt);
            }
        }
    }

    public RequestContext fromToken() {
        return new RequestContext(profileId);
    }
}
