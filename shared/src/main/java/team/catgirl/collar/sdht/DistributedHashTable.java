package team.catgirl.collar.sdht;

import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public final class DistributedHashTable {

    private static final int MAX_RECORDS = Short.MAX_VALUE;

    private final ConcurrentMap<UUID, ConcurrentMap<UUID, Content>> namespacedData = new ConcurrentHashMap<>();

    public Set<Record> records() {
        ImmutableSet.Builder<Record> records = ImmutableSet.builder();
        namespacedData.forEach((namespaceId, contentMap) -> {
            contentMap.forEach((id, content) -> records.add(new Record(new Key(namespaceId, id), content.checksum)));
        });
        return records.build();
    }

    /**
     * Get the content for the provided record
     * @param record to get
     * @return content or if the record was not valid, empty
     */
    public Optional<Content> get(Record record) {
        ConcurrentMap<UUID, Content> contentMap = namespacedData.get(record.key.namespace);
        if (contentMap == null) {
            return Optional.empty();
        }
        Content content = contentMap.get(record.key.id);
        return content == null || !content.isValid(record) ? Optional.empty() : Optional.of(content);
    }

    /**
     * Add the content to the hash table
     * @param record to add
     * @param content to add
     * @return content added
     */
    public Optional<Content> putIfAbsent(Record record, Content content) {
        if (!content.isValid(record) || !content.isValid()) {
            return Optional.empty();
        }
        AtomicReference<Content> computedContent = new AtomicReference<>();
        namespacedData.compute(record.key.namespace, (namespace, contentMap) -> {
            contentMap = contentMap == null ? new ConcurrentHashMap<>() : contentMap;
            Content computed = contentMap.computeIfAbsent(record.key.id, contentId -> content);
            computedContent.set(computed);
            return contentMap;
        });
        return computedContent.get() == null ? Optional.empty() : Optional.of(computedContent.get());
    }

    /**
     * Remove the content at key
     * @param record addressing the content
     * @return content removed
     */
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
