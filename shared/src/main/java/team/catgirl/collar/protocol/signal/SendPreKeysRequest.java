package team.catgirl.collar.protocol.signal;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.protocol.ProtocolRequest;
import team.catgirl.collar.security.PlayerIdentity;

import java.util.List;

public final class SendPreKeysRequest extends ProtocolRequest {
    @JsonProperty("registrationId")
    public final Integer registrationId;
    @JsonProperty("publicPreKeys")
    public final List<byte[]> publicPreKeys;
    @JsonProperty("signedPreKeyId")
    public final Integer signedPreKeyId;
    @JsonProperty("signedPreKeyPublic")
    public final byte[] signedPreKeyPublic;
    @JsonProperty("signedPreKeySignature")
    public final byte[] signedPreKeySignature;
    @JsonProperty("identityKey")
    public final byte[] identityKey;

    public SendPreKeysRequest(
            @JsonProperty("identity") PlayerIdentity identity,
            @JsonProperty("registrationId") Integer registrationId,
            @JsonProperty("publicPreKeys") List<byte[]> publicPreKeys,
            @JsonProperty("signedPreKeyId") Integer signedPreKeyId,
            @JsonProperty("signedPreKeyPublic") byte[] signedPreKeyPublic,
            @JsonProperty("signedPreKeySignature") byte[] signedPreKeySignature,
            @JsonProperty("identityKey") byte[] identityKey) {
        super(identity);
        this.registrationId = registrationId;
        this.publicPreKeys = publicPreKeys;
        this.signedPreKeyId = signedPreKeyId;
        this.signedPreKeyPublic = signedPreKeyPublic;
        this.signedPreKeySignature = signedPreKeySignature;
        this.identityKey = identityKey;
    }
}
