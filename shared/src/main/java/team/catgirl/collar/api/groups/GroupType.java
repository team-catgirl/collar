package team.catgirl.collar.api.groups;

public enum GroupType {
    /**
     * Created and managed by the players
     */
    PLAYER,
    /**
     * Groups that use seen entities to infer players nearby the member. Entirely managed by the server.
     */
    NEARBY
}
