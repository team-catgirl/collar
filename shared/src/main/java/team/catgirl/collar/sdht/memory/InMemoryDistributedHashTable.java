package team.catgirl.collar.sdht.memory;

import com.google.common.collect.ImmutableSet;
import team.catgirl.collar.sdht.*;
import team.catgirl.collar.sdht.Record;
import team.catgirl.collar.sdht.cipher.ContentCipher;
import team.catgirl.collar.sdht.events.CreateEntryEvent;
import team.catgirl.collar.sdht.events.DeleteRecordEvent;
import team.catgirl.collar.sdht.events.Publisher;
import team.catgirl.collar.security.ClientIdentity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class InMemoryDistributedHashTable extends DistributedHashTable {

    private static final Logger LOGGER = Logger.getLogger(InMemoryDistributedHashTable.class.getName());

    private static final int MAX_RECORDS = Short.MAX_VALUE;

    private final ConcurrentMap<UUID, ConcurrentMap<UUID, Content>> namespacedData = new ConcurrentHashMap<>();

    public InMemoryDistributedHashTable(Publisher publisher, Supplier<ClientIdentity> owner, ContentCipher cipher, DistributedHashTableListener listener) {
        super(publisher, owner, cipher, listener);
    }

    @Override
    public void remove(UUID namespace) {
        namespacedData.remove(namespace);
    }

    @Override
    public void removeAll() {
        namespacedData.clear();
    }

    @Override
    public Set<Record> records() {
        ImmutableSet.Builder<Record> records = ImmutableSet.builder();
        namespacedData.forEach((namespaceId, contentMap) -> {
            contentMap.forEach((id, content) -> records.add(new Record(new Key(namespaceId, id), content.checksum)));
        });
        return records.build();
    }

    @Override
    public Set<Record> records(UUID namespace) {
        ConcurrentMap<UUID, Content> contentMap = namespacedData.get(namespace);
        if (contentMap == null) {
            return ImmutableSet.of();
        }
        ImmutableSet.Builder<Record> records = ImmutableSet.builder();
        contentMap.forEach((id, content) -> records.add(new Record(new Key(namespace, id), content.checksum)));
        return records.build();
    }

    @Override
    public Optional<Content> get(Key key) {
        ConcurrentMap<UUID, Content> contentMap = namespacedData.get(key.namespace);
        if (contentMap == null) {
            return Optional.empty();
        }
        Content content = contentMap.get(key.id);
        return content == null || !content.isValid() ? Optional.empty() : Optional.of(content);
    }

    @Override
    public Optional<Content> put(Key key, Content content) {
        if (!content.isValid()) {
            return Optional.empty();
        }
        Record record = content.toRecord(key);
        if (namespacedData.size() > MAX_RECORDS) {
            throw new IllegalStateException("Maximum namespaces exceeds " + MAX_RECORDS);
        }
        AtomicReference<Content> computedContent = new AtomicReference<>();
        namespacedData.compute(record.key.namespace, (namespace, contentMap) -> {
            contentMap = contentMap == null ? new ConcurrentHashMap<>() : contentMap;
            if (contentMap.size() > MAX_RECORDS) {
                throw new IllegalStateException("namespace " + namespace + " exceeded maximum records " + MAX_RECORDS);
            }
            if (!contentMap.containsKey(record.key.id)) {
                computedContent.set(content);
            }
            contentMap.computeIfAbsent(record.key.id, uuid -> {
                computedContent.set(content);
                return content;
            });
            return contentMap;
        });
        if (computedContent.get() != null) {
            publisher.publish(new CreateEntryEvent(owner.get(), null, record, this.cipher.crypt(owner.get(), key.namespace, computedContent.get())));
            return Optional.of(computedContent.get());
        }
        return Optional.empty();
    }

    @Override
    protected void add(Record record, Content content) {
        if (!content.isValid(record)) {
            LOGGER.log(Level.SEVERE, "Record " + record + " did not match the content");
            return;
        }
        namespacedData.compute(record.key.namespace, (namespace, contentMap) -> {
            contentMap = contentMap == null ? new ConcurrentHashMap<>() : contentMap;
            contentMap.put(record.key.id, content);
            return contentMap;
        });
        listener.onAdd(record.key, content);
    }

    @Override
    public Optional<Content> delete(Key key) {
        AtomicReference<Content> removedContent = new AtomicReference<>();
        namespacedData.compute(key.namespace, (namespaceId, contentMap) -> {
            if (contentMap == null) {
                return null;
            }
            Content content = contentMap.get(key.id);
            if (content != null) {
                Content removed = contentMap.remove(key.id);
                removedContent.set(removed);
                if (removed != null) {
                    Record record = content.toRecord(key);
                    publisher.publish(new DeleteRecordEvent(owner.get(), record));
                }
            }
            return contentMap.isEmpty() ? null : contentMap;
        });
        if (removedContent.get() != null) {
            Record record = removedContent.get().toRecord(key);
            publisher.publish(new DeleteRecordEvent(owner.get(), record));
            return Optional.of(removedContent.get());
        }
        return Optional.empty();
    }

    @Override
    protected void remove(Record delete) {
        AtomicReference<Content> removedContent = new AtomicReference<>();
        namespacedData.compute(delete.key.namespace, (namespace, contentMap) -> {
            if (contentMap == null) {
                return null;
            }
            Content removed = contentMap.remove(delete.key.id);
            removedContent.set(removed);
            if (contentMap.isEmpty()) {
                return null;
            }
            return contentMap;
        });
        if (removedContent.get() != null) {
            listener.onRemove(delete.key, removedContent.get());
        }
    }
}
