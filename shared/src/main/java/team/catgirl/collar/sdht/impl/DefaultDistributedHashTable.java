package team.catgirl.collar.sdht.impl;

import com.google.common.collect.ImmutableSet;
import team.catgirl.collar.sdht.*;
import team.catgirl.collar.sdht.Record;
import team.catgirl.collar.sdht.cipher.ContentCipher;
import team.catgirl.collar.sdht.events.CreateEntryEvent;
import team.catgirl.collar.sdht.events.DeleteRecordEvent;
import team.catgirl.collar.sdht.events.Publisher;
import team.catgirl.collar.security.ClientIdentity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DefaultDistributedHashTable extends DistributedHashTable {

    private static final Logger LOGGER = Logger.getLogger(DefaultDistributedHashTable.class.getName());

    private static final int MAX_NAMESPACES = Short.MAX_VALUE;
    private static final int MAX_RECORDS = Short.MAX_VALUE;

    private final ConcurrentMap<UUID, ConcurrentMap<UUID, Content>> dhtContent;
    private final DHTNamespaceState state;

    public DefaultDistributedHashTable(Publisher publisher, Supplier<ClientIdentity> owner, ContentCipher cipher, DHTNamespaceState state, DistributedHashTableListener listener) {
        super(publisher, owner, cipher, listener);
        this.state = state;
        this.dhtContent = state.read();
        pruneAllNamespaces();
    }

    @Override
    public void remove(UUID namespace) {
        dhtContent.remove(namespace);
    }

    @Override
    public void removeAll() {
        dhtContent.clear();
    }

    @Override
    public Set<Record> records() {
        ImmutableSet.Builder<Record> records = ImmutableSet.builder();
        dhtContent.forEach((namespaceId, contentMap) -> {
            contentMap.forEach((id, content) -> records.add(new Record(new Key(namespaceId, id), content.checksum, content.version)));
        });
        return records.build();
    }

    @Override
    public Set<Record> records(UUID namespace) {
        ConcurrentMap<UUID, Content> contentMap = dhtContent.get(namespace);
        if (contentMap == null) {
            return ImmutableSet.of();
        }
        ImmutableSet.Builder<Record> records = ImmutableSet.builder();
        contentMap.forEach((id, content) -> records.add(new Record(new Key(namespace, id), content.checksum, content.version)));
        return records.build();
    }

    @Override
    public Optional<Content> get(Key key) {
        ConcurrentMap<UUID, Content> contentMap = dhtContent.get(key.namespace);
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
        if (dhtContent.size() > MAX_NAMESPACES) {
            pruneAllNamespaces();
        }
        if (dhtContent.size() > MAX_NAMESPACES) {
            throw new IllegalStateException("Maximum namespaces exceeds " + MAX_NAMESPACES);
        }
        AtomicReference<Content> computedContent = new AtomicReference<>();
        dhtContent.compute(record.key.namespace, (namespace, contentMap) -> {
            contentMap = contentMap == null ? new ConcurrentHashMap<>() : contentMap;
            if (contentMap.size() > MAX_RECORDS) {
                pruneNamespace(contentMap);
            }
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
            sync();
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
        dhtContent.compute(record.key.namespace, (namespace, contentMap) -> {
            contentMap = contentMap == null ? new ConcurrentHashMap<>() : contentMap;
            contentMap.put(record.key.id, content);
            return contentMap;
        });
        sync();
        listener.onAdd(record.key, content);
    }

    @Override
    public Optional<Content> delete(Key key) {
        AtomicReference<Content> removedContent = new AtomicReference<>();
        dhtContent.compute(key.namespace, (namespaceId, contentMap) -> {
            if (contentMap == null) {
                return null;
            }
            Content content = contentMap.get(key.id);
            if (content != null) {
                Content removed = contentMap.get(key.id);
                Content deleted = deletedRecord();
                Record record = deleted.toRecord(key);
                contentMap.put(key.id, deleted);
                removedContent.set(deleted);
                if (removed != null) {
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
        dhtContent.compute(delete.key.namespace, (namespace, contentMap) -> {
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
            sync();
            listener.onRemove(delete.key, removedContent.get());
        }
    }

    private void sync() {
        ForkJoinPool.commonPool().submit(() -> state.write(dhtContent));
    }

    private void pruneAllNamespaces() {
        dhtContent.values().forEach(map -> map.keySet().forEach(namespaceId -> pruneNamespace(map)));
    }

    /**
     * Prunes the namespace of dead entries
     * @param namespaceContents of namespace
     */
    private void pruneNamespace(ConcurrentMap<UUID, Content> namespaceContents) {
        namespaceContents.keySet().forEach(uuid -> namespaceContents.computeIfPresent(uuid, (uuid1, content) -> Content.isDead(content) ? null : content));
    }

    private static Optional<Content> getLatestVersion(CopyOnWriteArraySet<Content> contents) {
        return contents.stream().filter(Content::isValid).max(Comparator.comparingLong(o -> o.version));
    }

    private static long newVersion() {
        return new Date().getTime();
    }

    private static Content deletedRecord() {
        return new Content(null, null, null, newVersion(), State.DELETED);
    }
}
