package team.catgirl.collar.sdht;

import team.catgirl.collar.sdht.cipher.ContentCipher;
import team.catgirl.collar.sdht.events.*;
import team.catgirl.collar.security.ClientIdentity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class DistributedHashTable {

    protected final Publisher publisher;
    protected final Supplier<ClientIdentity> owner;
    protected final ContentCipher cipher;
    protected final DistributedHashTableListener listener;

    public DistributedHashTable(Publisher publisher, Supplier<ClientIdentity> owner, ContentCipher cipher, DistributedHashTableListener listener) {
        this.publisher = publisher;
        this.owner = owner;
        this.cipher = cipher;
        this.listener = listener;
    }

    /**
     * Sync's the hashtable with all members with the namespace
     */
    public void sync(UUID namespace) {
        publisher.publish(new SyncRecordsEvent(owner.get(), namespace));
    }

    /**
     * Remove all records in the namespace from the local copy
     * @param namespace to remove
     */
    public abstract void remove(UUID namespace);

    /**
     * Remove all records from the local copy
     */
    public abstract void removeAll();

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
     * Get the content for the provided key
     * @param key to get
     * @return content or if the record was not valid, empty
     */
    public abstract Optional<Content> get(Key key);

    /**
     * Add the content to the hash table
     * @param key to add
     * @param content to add
     * @return content added
     */
    public abstract Optional<Content> put(Key key, Content content);

    /**
     * Remove the content at key
     * @param key addressing the content
     * @return content removed
     */
    public abstract Optional<Content> delete(Key key);

    /**
     * Process incoming state change events
     * @param e event
     */
    public void process(AbstractSDHTEvent e) {
        if (e instanceof CreateEntryEvent) {
            CreateEntryEvent event = (CreateEntryEvent) e;
            Content content = cipher.decrypt(event.sender, event.record.key.namespace, event.content);
            add(event.record, content);
        } else if (e instanceof DeleteRecordEvent) {
            DeleteRecordEvent event = (DeleteRecordEvent) e;
            remove(event.delete);
        } else if (e instanceof SyncRecordsEvent) {
            SyncRecordsEvent event = (SyncRecordsEvent) e;
            Set<Record> records = records(event.namespace);
            if (!records.isEmpty()) {
                publisher.publish(new PublishRecordsEvent(owner.get(), records, event.sender));
            }
        } else if (e instanceof PublishRecordsEvent) {
            PublishRecordsEvent event = (PublishRecordsEvent) e;
            event.records.forEach(record -> publisher.publish(new SyncContentEvent(owner.get(), event.recipient, record)));
        } else if (e instanceof SyncContentEvent) {
            SyncContentEvent event = (SyncContentEvent) e;
            Optional<Content> content = get(event.record.key);
            if (content.isPresent()) {
                byte[] bytes = cipher.crypt(owner.get(), event.record.key.namespace, content.get());
                publisher.publish(new CreateEntryEvent(owner.get(), event.recipient, event.record, bytes));
            } else {
                publisher.publish(new CreateEntryEvent(owner.get(), event.recipient, event.record, null));
            }
        }
    }

    /**
     * Add the record and it's content to the local copy
     * @param record to add
     * @param content to add
     */
    protected abstract void add(Record record, Content content);

    /**
     * Remove the record from the local copy
     * @param delete to delete
     */
    protected abstract void remove(Record delete);
}