package team.catgirl.collar.api.profiles;

public enum Role {
    /**
     * A regular player
     */
    PLAYER(1),
    TRIAL(2),
    PAID(2),
    DELEGATED(2),
    /**
     * A based player
     */
    BASED(998),
    /**
     * Owns the service
     */
    ADMINISTRATOR(999);

    public final int level;

    Role(int level) {
        this.level = level;
    }
}
