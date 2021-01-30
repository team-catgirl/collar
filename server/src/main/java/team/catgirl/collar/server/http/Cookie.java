package team.catgirl.collar.server.http;

import com.google.common.io.BaseEncoding;
import spark.Request;
import spark.Response;
import team.catgirl.collar.server.services.authentication.TokenCrypter;

import java.io.*;
import java.util.Date;
import java.util.UUID;

public class Cookie {

    private static final int VERSION = 1;

    public final UUID profileId;
    public final long expiresAt;

    public Cookie(UUID profileId, long expiresAt) {
        this.profileId = profileId;
        this.expiresAt = expiresAt;
    }

    public static void remove(Response response) {
        response.cookie("identity", null);
    }

    public static Cookie from(TokenCrypter crypter, Request request) {
        String identity = request.cookie("identity");
        if (identity == null) {
            return null;
        }
        byte[] bytes; {
            try {
                bytes = crypter.decrypt(BaseEncoding.base64Url().decode(identity));
            } catch (Throwable e) {
                return null;
            }
        }
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes)) {
            try (ObjectInputStream objectStream = new ObjectInputStream(byteStream)) {
                String profileId;
                long expiresAt;
                int version = objectStream.readInt();
                switch (version) {
                    case 1:
                        profileId = objectStream.readUTF();
                        expiresAt = objectStream.readLong();
                        break;
                    default:
                        throw new IllegalStateException("unsupported version " + version);
                }
                return new Date().after(new Date(expiresAt)) ? null : new Cookie(UUID.fromString(profileId), expiresAt);
            }
        } catch (IOException e) {
            return null;
        }
    }

    public void set(TokenCrypter crypter, Response response) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            try (ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
                objectStream.writeInt(VERSION);
                objectStream.writeUTF(profileId.toString());
                objectStream.writeLong(expiresAt);
            }
            byteStream.flush();
            String encoded = BaseEncoding.base64Url().encode(crypter.crypt(byteStream.toByteArray()));
            response.cookie("identity", encoded);
        }
    }
}
