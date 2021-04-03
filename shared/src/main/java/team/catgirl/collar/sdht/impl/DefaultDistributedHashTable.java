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

    private static final int MAX_RECORDS = Short.MAX_VALUE;

    private final ConcurrentMap<UUID, ConcurrentMap<UUID, CopyOnWriteArraySet<Content>>> namespacedData;
    private final DHTNamespaceState state;

    public DefaultDistributedHashTable(Publisher publisher, Supplier<ClientIdentity> owner, ContentCipher cipher, DHTNamespaceState state, DistributedHashTableListener listener) {
        super(publisher, owner, cipher, listener);
        this.state = state;
        this.namespacedData = state.read();
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
        namespacedData.forEach((namespace, contentMap) -> {
            contentMap.forEach((id, contents) -> {
                contents.forEach(content -> {
                    records.add(new Record(new Key(namespace, id), content.checksum, content.version));
                });
            });
        });
        return records.build();
    }

    @Override
    public Set<Record> records(UUID namespace) {
        ConcurrentMap<UUID, CopyOnWriteArraySet<Content>> contentMap = namespacedData.get(namespace);
        if (contentMap == null) {
            return ImmutableSet.of();
        }
        ImmutableSet.Builder<Record> records = ImmutableSet.builder();
        contentMap.forEach((id, contents) -> {
            contents.forEach(content -> {
                records.add(new Record(new Key(namespace, id), content.checksum, content.version));
            });
        });
        return records.build();
    }

    @Override
    public Optional<Content> get(Key key) {
        ConcurrentMap<UUID, CopyOnWriteArraySet<Content>> contentMap = namespacedData.get(key.namespace);
        if (contentMap == null) {
            return Optional.empty();
        }
        CopyOnWriteArraySet<Content> contents = contentMap.get(key.id);
        if (contents == null || contents.isEmpty()) {
            return Optional.empty();
        }
        return getLatestVersion(contents);
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
                CopyOnWriteArraySet<Content> contents = new CopyOnWriteArraySet<>();
                contents.add(content);
                return contents;
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
        namespacedData.compute(record.key.namespace, (namespace, contentMap) -> {
            contentMap = contentMap == null ? new ConcurrentHashMap<>() : contentMap;
            CopyOnWriteArraySet<Content> contents = new CopyOnWriteArraySet<>();
            contents.add(content);
            contentMap.put(record.key.id, contents);
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
            CopyOnWriteArraySet<Content> contents = contentMap.get(key.id);
            if (contents != null) {
                Optional<Content> optionalRemoved = getLatestVersion(contents);
                if (optionalRemoved.isPresent()) {
                    Content removed = optionalRemoved.get();
                    removedContent.set(removed);
                    Content deleted = deletedRecord();
                    Record record = deleted.toRecord(key);
                    contents.clear();
                    contents.add(deleted);
                    publisher.publish(new DeleteRecordEvent(owner.get(), record));
                }
            }
            return contentMap.isEmpty() ? null : contentMap;
        });
        if (removedContent.get() != null && removedContent.get().state != State.DELETED) {
            Record record = removedContent.get().toRecord(key);
            publisher.publish(new DeleteRecordEvent(owner.get(), record));
            sync();
            return Optional.of(removedContent.get());
        }
        return Optional.empty();
    }

    @Override
    protected void remove(Record delete) {
        AtomicReference<Content> removedContent = new AtomicReference<>();
        namespacedData.compute(delete.key.namespace, (namespace, contentMap) -> {
            if (contentMap == null || contentMap.isEmpty()) {
                return null;
            }
            CopyOnWriteArraySet<Content> contents = contentMap.get(delete.key.id);
            if (contents == null || contents.isEmpty()) {
                return null;
            }
            contents.clear();
            contents.add(deletedRecord());
            return contentMap;
        });
        if (removedContent.get() != null) {
            sync();
            listener.onRemove(delete.key, removedContent.get());
        }
    }

    private void sync() {
        ForkJoinPool.commonPool().submit(() -> state.write(namespacedData));
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
