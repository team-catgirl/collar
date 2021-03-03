package team.catgirl.collar.api.groups;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum GroupType {
    /**
     * A persistent group of players
     */
    GROUP,
    /**
     * A temporary group of players
     */
    @JsonEnumDefaultValue
    PARTY,
    /**
     * A location proximity group
     */
    NEARBY;
}
