package team.catgirl.collar.sdht;

import com.google.common.collect.ImmutableSet;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public final class InMemoryDistributedHashTable implements DistributedHashTable {

    private static final int MAX_RECORDS = Short.MAX_VALUE;

    private final ConcurrentMap<UUID, ConcurrentMap<UUID, Content>> namespacedData = new ConcurrentHashMap<>();

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
    public Optional<Content> get(Record record) {
        ConcurrentMap<UUID, Content> contentMap = namespacedData.get(record.key.namespace);
        if (contentMap == null) {
            return Optional.empty();
        }
        Content content = contentMap.get(record.key.id);
        return content == null || !content.isValid(record) ? Optional.empty() : Optional.of(content);
    }

    @Override
    public Optional<Content> putIfAbsent(Record record, Content content) {
        if (!content.isValid(record) || !content.isValid()) {
            return Optional.empty();
        }
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
            contentMap.computeIfAbsent(record.key.id, contentId -> content);
            return contentMap;
        });
        return computedContent.get() == null ? Optional.empty() : Optional.of(computedContent.get());
    }

    @Override
    public Optional<Content> remove(Record record) {
        AtomicReference<Content> removedContent = new AtomicReference<>();
        namespacedData.compute(record.key.namespace, (namespaceId, contentMap) -> {
            if (contentMap == null) {
                return null;
            }
            Content content = contentMap.get(record.key.id);
            if (content != null && content.isValid(record)) {
                Content removed = contentMap.remove(record.key.id);
                removedContent.set(removed);
            }
            return contentMap.isEmpty() ? null : contentMap;
        });
        return removedContent.get() == null ? Optional.empty() : Optional.of(removedContent.get());
    }
}
