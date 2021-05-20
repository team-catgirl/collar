package team.catgirl.collar.sdht;

import team.catgirl.collar.sdht.cipher.ContentCipher;
import team.catgirl.collar.sdht.events.*;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.cipher.CipherException;
import team.catgirl.collar.utils.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

public abstract class DistributedHashTable {

    protected final Publisher publisher;
    protected final Supplier<ClientIdentity> owner;
    protected final ContentCipher cipher;
    protected final DistributedHashTableListener listener;
    protected final ConcurrentHashMap<Record, List<ClientIdentity>> recordSources = new ConcurrentHashMap<>();
    protected final Queue<Record> pendingRecords = new ConcurrentLinkedDeque<>();

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
            Content content = null;
            try {
                content = cipher.decrypt(event.sender, event.record.key.namespace, event.content);
            } catch (CipherException ex) {
                throw new IllegalStateException(ex);
            }
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
            event.records.forEach(record -> {
                recordSources.compute(record, (theRecord, clientIdentities) -> {
                    clientIdentities = clientIdentities == null ? new ArrayList<>() : clientIdentities;
                    clientIdentities.add(event.sender);
                    return clientIdentities;
                });
                pendingRecords.offer(record);
            });
        } else if (e instanceof SyncContentEvent) {
            SyncContentEvent event = (SyncContentEvent) e;
            Optional<Content> content = get(event.record.key);
            if (content.isPresent()) {
                byte[] bytes;
                try {
                    bytes = cipher.crypt(owner.get(), event.record.key.namespace, content.get());
                } catch (CipherException ex) {
                    throw new IllegalStateException(ex);
                }
                publisher.publish(new CreateEntryEvent(owner.get(), event.sender, event.record, bytes));
            } else {
                publisher.publish(new CreateEntryEvent(owner.get(), event.sender, event.record, null));
            }
        }
    }

    /**
     * Processes a single Record that is pending to be downloaded
     */
    public void processPendingRecords() {
        Record record = pendingRecords.poll();
        if (record == null) {
            return;
        }
        List<ClientIdentity> identities = recordSources.remove(record);
        if (identities == null) {
            return;
        }
        // Pick a random identity that has the record and ask it for the contents
        ClientIdentity identity = identities.get(Utils.secureRandom().nextInt(identities.size()));
        publisher.publish(new SyncContentEvent(owner.get(), identity, record));
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
