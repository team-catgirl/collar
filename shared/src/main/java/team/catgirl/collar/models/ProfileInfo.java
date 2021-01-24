package team.catgirl.collar.models;

/**
 * Public version of {@link Profile}
 */
public class ProfileInfo {
    public final String id;
    public final String displayName;
    public final Byte[] capeImage;
    public final Byte[] profileImage;

    public ProfileInfo(String id, String displayName, Byte[] capeImage, Byte[] profileImage) {
        this.id = id;
        this.displayName = displayName;
        this.capeImage = capeImage;
        this.profileImage = profileImage;
    }

    public static ProfileInfo from(Profile profile) {
        return new ProfileInfo(profile.id, profile.displayName, profile.capeImage, profile.profileImage);
    }
}
