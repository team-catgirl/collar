package team.catgirl.collar.sdht;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public abstract class DistributedHashTable {

    /**
     * Return all of the records in the DHT
     * @return records
     */
    public abstract Set<Record> records();

    /**
     * Return all of the records in the DHT belonging to the provided namespace
     * @param namespace to use
     * @return records
     */
    public abstract Set<Record> records(UUID namespace);

    /**
     * Get the content for the provided record
     * @param record to get
     * @return content or if the record was not valid, empty
     */
    public abstract Optional<Content> get(Record record);

    /**
     * Add the content to the hash table
     * @param record to add
     * @param content to add
     * @return content added
     */
    public abstract Optional<Content> putIfAbsent(Record record, Content content);

    /**
     * Remove the content at key
     * @param record addressing the content
     * @return content removed
     */
    public abstract Optional<Content> remove(Record record);

    /**
     * Remove all content from the table
     */
    public abstract void removeAll();
}
