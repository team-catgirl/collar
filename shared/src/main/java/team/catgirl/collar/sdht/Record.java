package team.catgirl.collar.sdht;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.utils.Hex;

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
    @JsonProperty("version")
    public final long version;

    public Record(@JsonProperty("key") Key key,
                  @JsonProperty("checksum") byte[] checksum,
                  @JsonProperty("version") long version) {
        this.key = key;
        this.checksum = checksum;
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Record record = (Record) o;
        return version == record.version && key.equals(record.key) && Arrays.equals(checksum, record.checksum);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(key, version);
        result = 31 * result + Arrays.hashCode(checksum);
        return result;
    }

    @Override
    public String toString() {
        if (checksum == null) {
            return key + ":0x0:" + version;
        } else {
            return key + ":" + Hex.hexString(checksum) + ":" + version;
        }
    }
}
