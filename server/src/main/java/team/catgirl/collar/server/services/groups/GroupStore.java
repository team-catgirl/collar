package team.catgirl.collar.server.services.groups;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.api.groups.GroupType;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public final class GroupStore {

    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_SERVER = "server";

    private final MongoCollection<Document> docs;

    public GroupStore(MongoDatabase database) {
        this.docs = database.getCollection("groups");
    }

    /**
     * Upsert group into the store
     * @param group to store
     */
    public void upsert(Group group) {
        Document document = mapToDocument(group);
        UpdateResult result = docs.updateOne(eq(FIELD_ID, group.id), document, new UpdateOptions().upsert(true));
        if (!result.wasAcknowledged()) {
            throw new IllegalStateException("group " + group.id + " could not be upserted");
        }
    }

    /**
     * Get group by id
     * @param groupId to get
     * @return group
     */
    public Optional<Group> getGroup(String groupId) {
        Document first = docs.find(eq(FIELD_ID, groupId)).first();
        return first == null ? Optional.empty() : Optional.of(mapFromDocument(first));
    }

    /**
     * Stream all groups from store
     * @return group stream
     */
    public Stream<Group> findGroups() {
        MongoCursor<Group> iterator = docs.find().map(GroupStore::mapFromDocument).batchSize(100).iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    /**
     * Delete a group
     * @param group to delete
     * @return deleted
     */
    public boolean delete(Group group) {
        DeleteResult result = docs.deleteOne(eq(FIELD_ID, group.id));
        if (!result.wasAcknowledged()) {
            throw new IllegalStateException("group " + group.id + " could not be deleted");
        }
        return result.getDeletedCount() == 1;
    }

    /**
     * Delete all groups matching type
     * @param groupType to delete
     * @return number of groups deleted
     */
    public long delete(GroupType groupType) {
        DeleteResult result = docs.deleteMany(and(eq(FIELD_TYPE, groupType.name())));
        if (!result.wasAcknowledged()) {
            throw new IllegalStateException("groups with type " + groupType + " could not be deleted");
        }
        return result.getDeletedCount();
    }

    @NotNull
    static Group mapFromDocument(Document doc) {
        return null;
    }

    static Document mapToDocument(Group group) {
        Map<String, Object> doc = new HashMap<>();
        doc.put(FIELD_ID, group.id);
        doc.put(FIELD_TYPE, group.type.name());
        doc.put(FIELD_SERVER, group.server);
        return null;
    }
}
