package team.catgirl.collar.sdht;

import java.util.Arrays;

public enum State {
    /** Exists **/
    EXTANT(0),
    /** Deleted **/
    DELETED(1);

    public final int value;

    State(int value) {
        this.value = value;
    }

    /**
     * Create state from value integer
     * @param i value
     * @return state
     */
    public static State from(int i) {
        return Arrays.stream(values()).filter(state -> state.value == i)
                .findFirst().orElseThrow(() -> new IllegalStateException("could not find State with value " + i));
    }
}
