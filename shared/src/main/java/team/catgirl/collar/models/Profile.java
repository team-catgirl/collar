package team.catgirl.collar.models;

public class Profile {
    public final String id;
    public final String displayName;
    public final Friends friends;
    public final Byte[] capeImage;
    public final Byte[] profileImage;
    public final String favouriteColor;

    public Profile(String id, String displayName, Friends friends, Byte[] capeImage, Byte[] profileImage, String favouriteColor) {
        this.id = id;
        this.displayName = displayName;
        this.friends = friends;
        this.capeImage = capeImage;
        this.profileImage = profileImage;
        this.favouriteColor = favouriteColor;
    }
}
