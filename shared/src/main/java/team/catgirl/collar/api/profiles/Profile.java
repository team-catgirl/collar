package team.catgirl.collar.api.profiles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;
import java.util.UUID;

public final class Profile {
    @JsonProperty("id")
    public final UUID id;
    @JsonProperty("roles")
    public final Set<Role> roles;
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
                   Set<Role> roles,
                   String email,
                   String name,
                   String hashedPassword,
                   Boolean emailVerified,
                   byte[] privateIdentityToken,
                   TexturePreference cape,
                   Set<UUID> knownAccounts) {
        this.id = id;
        this.roles = roles;
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
                   @JsonProperty("roles") Set<Role> roles,
                   @JsonProperty("email") String email,
                   @JsonProperty("name") String name,
                   @JsonProperty("emailVerified") Boolean emailVerified,
                   @JsonProperty("cape") TexturePreference cape,
                   @JsonProperty("knownAccounts") Set<UUID> knownAccounts) {
        this.id = id;
        this.roles = roles;
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
