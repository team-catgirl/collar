package team.catgirl.collar.server.http;

import com.google.common.io.BaseEncoding;
import team.catgirl.collar.api.http.RequestContext;
import team.catgirl.collar.api.profiles.Role;
import team.catgirl.collar.server.services.authentication.TokenCrypter;

import java.io.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Used for authorizing API calls
 */
public class ApiToken {

    private static final int VERSION = 2;

    public final UUID profileId;
    public final long expiresAt;
    public final Set<Role> roles;

    public ApiToken(UUID profileId, long expiresAt, Set<Role> roles) {
        this.profileId = profileId;
        this.expiresAt = expiresAt;
        this.roles = roles;
    }

    public ApiToken(UUID profileId, Set<Role> roles) {
        this.profileId = profileId;
        this.roles = roles;
        this.expiresAt = new Date().getTime() + TimeUnit.DAYS.toMillis(7);
    }

    public boolean isExpired() {
        return new Date().after(new Date(expiresAt));
    }

    public String serialize(TokenCrypter crypter) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (DataOutputStream dataStream = new DataOutputStream(outputStream)) {
                dataStream.write(VERSION);
                dataStream.writeUTF(profileId.toString());
                dataStream.writeLong(expiresAt);
                dataStream.writeInt(roles.size());
                for (Role role : roles) {
                    dataStream.writeInt(role.ordinal());
                }
            }
            return BaseEncoding.base64Url().encode(crypter.crypt(outputStream.toByteArray()));
        }
    }

    public static ApiToken deserialize(TokenCrypter crypter, String token) throws IOException {
        byte[] bytes = BaseEncoding.base64Url().decode(token);
        byte[] decryptedBytes = crypter.decrypt(bytes);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(decryptedBytes)) {
            try (DataInputStream dataStream = new DataInputStream(inputStream)) {
                int version = dataStream.read();
                String uuidAsString;
                long expiresAt;
                Set<Role> roles = new HashSet<>();
                switch (version) {
                    case 1:
                        uuidAsString = dataStream.readUTF();
                        expiresAt = dataStream.readLong();
                        roles.add(Role.PLAYER);
                        break;
                    case 2:
                        uuidAsString = dataStream.readUTF();
                        expiresAt = dataStream.readLong();
                        int rolesCount = dataStream.readInt();
                        for (int i = 0; i < rolesCount; i++) {
                            int roleId = dataStream.readInt();
                            roles.add(Role.values()[roleId]);
                        }
                        break;
                    default:
                        throw new IllegalStateException("unknown version " + version);
                }
                return new ApiToken(UUID.fromString(uuidAsString), expiresAt, roles);
            }
        }
    }

    public RequestContext fromToken() {
        return new RequestContext(profileId, roles);
    }
}
