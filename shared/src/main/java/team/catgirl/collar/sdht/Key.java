package team.catgirl.collar.sdht;

import com.fasterxml.jackson.annotation.JsonProperty;
import team.catgirl.collar.sdht.memory.InMemoryDistributedHashTable;

import java.util.Objects;
import java.util.UUID;

/**
 * Key for {@link Content} within the {@link InMemoryDistributedHashTable}
 */
public final class Key {
    @JsonProperty("ns")
    public final UUID namespace;
    @JsonProperty("id")
    public final UUID id;

    public Key(@JsonProperty("ns") UUID namespace,
               @JsonProperty("id") UUID id) {
        this.namespace = namespace;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Key key = (Key) o;
        return namespace.equals(key.namespace) && id.equals(key.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, id);
    }

    @Override
    public String toString() {
        return namespace + ":" + id;
    }
}
