package team.catgirl.collar.sdht;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Objects;

/**
 * SDHT record used to referring to content across nodes
 */
public final class Record {
    @JsonProperty("key")
    public final Key key;
    @JsonProperty("checksum")
    public final byte[] checksum;

    public Record(@JsonProperty("key") Key key,
                  @JsonProperty("checksum") byte[] checksum) {
        this.key = key;
        this.checksum = checksum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Record record = (Record) o;
        return key.equals(record.key) && Arrays.equals(checksum, record.checksum);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(key);
        result = 31 * result + Arrays.hashCode(checksum);
        return result;
    }
}
