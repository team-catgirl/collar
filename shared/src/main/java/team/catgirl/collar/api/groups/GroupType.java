package team.catgirl.collar.api.groups;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum GroupType {
    /**
     * A persistent {@link #PARTY}. Always exists until the admin deletes it.
     */
    GROUP,
    /**
     * Created and managed by the players who are members
     * The party is deleted when there are no players in the party
     */
    @JsonEnumDefaultValue
    PARTY,
    /**
     * Groups that use seen entities to infer players nearby the member. Entirely managed by the server.
     */
    NEARBY;
}
