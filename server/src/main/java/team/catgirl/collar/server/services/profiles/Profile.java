package team.catgirl.collar.server.services.profiles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.api.profiles.TexturePreference;

import java.util.Set;
import java.util.UUID;

public final class Profile {
    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("email")
    public final String email;
    @JsonProperty("name")
    public final String name;
    @JsonIgnore
    public final String hashedPassword;
    @JsonProperty("emailVerified")
    public final Boolean emailVerified;
    @JsonProperty("privateIdentityToken")
    public final byte[] privateIdentityToken;
    @JsonProperty("cape")
    public final TexturePreference cape;
    @JsonProperty("knownAccounts")
    public final Set<UUID> knownAccounts;

    public Profile(UUID id,
                   String email,
                   String name,
                   String hashedPassword,
                   Boolean emailVerified,
                   byte[] privateIdentityToken,
                   TexturePreference cape,
                   Set<UUID> knownAccounts) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.hashedPassword = hashedPassword;
        this.emailVerified = emailVerified;
        this.privateIdentityToken = privateIdentityToken;
        this.cape = cape;
        this.knownAccounts = knownAccounts;
    }

    @JsonCreator
    public Profile(@JsonProperty("id") UUID id,
                   @JsonProperty("email") String email,
                   @JsonProperty("name") String name,
                   @JsonProperty("name") Boolean emailVerified,
                   @JsonProperty("cape") TexturePreference cape,
                   @JsonProperty("knownAccounts") Set<UUID> knownAccounts) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.emailVerified = emailVerified;
        this.cape = cape;
        this.knownAccounts = knownAccounts;
        this.privateIdentityToken = null;
        this.hashedPassword = null;
    }

    @JsonIgnore
    public PublicProfile toPublic() {
        return new PublicProfile(this.id, this.name, cape);
    }
}
