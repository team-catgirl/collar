package team.catgirl.collar.server.services.groups;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import team.catgirl.collar.api.groups.*;
import team.catgirl.collar.api.profiles.PublicProfile;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.api.http.RequestContext;
import team.catgirl.collar.api.profiles.ProfileService;
import team.catgirl.collar.server.services.profiles.ProfileServiceServer;
import team.catgirl.collar.server.session.SessionManager;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public final class GroupStore {

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_MEMBERS = "members";
    private static final String FIELD_MEMBER_ROLE = "role";
    private static final String FIELD_MEMBER_STATE = "state";
    private static final String FIELD_MEMBER_PROFILE_ID = "profileId";

    private final ProfileService profiles;
    private final SessionManager sessions;
    private final MongoCollection<Document> docs;

    public GroupStore(ProfileService profiles, SessionManager sessions, MongoDatabase database) {
        this.profiles = profiles;
        this.sessions = sessions;
        this.docs = database.getCollection("groups");
    }

    /**
     * Upsert group into the store
     * @param group to store
     */
    public void upsert(Group group) {
        Document document = mapToDocument(group);
        UpdateResult result = docs.replaceOne(eq(FIELD_ID, group.id), document, new ReplaceOptions().upsert(true));
        if (!result.wasAcknowledged()) {
            throw new IllegalStateException("group " + group.id + " could not be upserted");
        }
    }

    /**
     * Get group by id
     * @param groupId to get
     * @return group
     */
    public Optional<Group> findGroup(UUID groupId) {
        Document first = docs.find(eq(FIELD_ID, groupId)).first();
        return first == null ? Optional.empty() : Optional.of(mapFromDocument(first));
    }

    public Stream<Group> findGroups(Set<UUID> uuids) {
        MongoCursor<Group> iterator = docs.find(in(FIELD_ID, uuids)).map(this::mapFromDocument).batchSize(100).iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    public Stream<Group> findGroupsContaining(Player player) {
        MongoCursor<Group> iterator = docs.find(eq(FIELD_MEMBERS + "." + FIELD_MEMBER_PROFILE_ID, player.profile)).map(this::mapFromDocument).batchSize(100).iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    public Stream<Group> findGroupsContaining(PublicProfile profile) {
        MongoCursor<Group> iterator = docs.find(eq(FIELD_MEMBERS + "." + FIELD_MEMBER_PROFILE_ID, profile.id)).map(this::mapFromDocument).batchSize(100).iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    public Optional<Group> addMembers(UUID id, List<MemberSource> memberSources, MembershipRole role, MembershipState state) {
        List<Document> members = mapToMembersList(memberSources.stream().map(source -> new Member(source.player, source.profile, role, state)).collect(Collectors.toList()));
        UpdateResult result = docs.updateOne(eq(FIELD_ID, id), pushEach(FIELD_MEMBERS, members));
        if (!result.wasAcknowledged() && result.getModifiedCount() != 1) {
            throw new IllegalStateException("failed to add members to group " + id);
        }
        return findGroup(id);
    }

    public Optional<Group> updateMember(UUID id, UUID profile, MembershipRole role, MembershipState state) {
        UpdateOptions updateOptions = new UpdateOptions().arrayFilters(List.of(new Document("item." + FIELD_MEMBER_PROFILE_ID, profile)));
        UpdateResult result = docs.updateOne(eq(FIELD_ID, id), set(FIELD_MEMBERS + ".$[item]", mapMember(profile, role, state)), updateOptions);
        if (!result.wasAcknowledged() && result.getModifiedCount() != 1) {
            throw new IllegalStateException("could not remove member " + profile + " from group " + id);
        }
        return findGroup(id);
    }

    public Optional<Group> removeMember(UUID id, UUID profile) {
        UpdateResult result = docs.updateOne(eq(FIELD_ID, id), pull(FIELD_MEMBERS, new Document(Map.of(FIELD_MEMBER_PROFILE_ID, profile))));
        if (!result.wasAcknowledged() && result.getModifiedCount() != 1) {
            throw new IllegalStateException("could not remove member " + profile + " from group " + id);
        }
        return findGroup(id);
    }

    /**
     * Delete a group
     * @param group to delete
     * @return deleted
     */
    public boolean delete(UUID group) {
        DeleteResult result = docs.deleteOne(eq(FIELD_ID, group));
        if (!result.wasAcknowledged()) {
            throw new IllegalStateException("group " + group + " could not be deleted");
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

    private Group mapFromDocument(Document doc) {
        Set<Member> members = doc.getList(FIELD_MEMBERS, Document.class, new ArrayList<>()).stream()
                .map(this::mapMemberFrom)
                .collect(Collectors.toSet());
        UUID groupId = doc.get(FIELD_ID, UUID.class);
        GroupType groupType = GroupType.valueOf(doc.getString(FIELD_TYPE));
        String name = doc.getString(FIELD_NAME);
        return new Group(groupId, name, groupType, members);
    }

    static Document mapToDocument(Group group) {
        Map<String, Object> doc = new HashMap<>();
        doc.put(FIELD_ID, group.id);
        doc.put(FIELD_NAME, group.name);
        doc.put(FIELD_TYPE, group.type.name());
        doc.put(FIELD_MEMBERS, mapToMembersList(group.members));
        return new Document(doc);
    }

    private static List<Document> mapToMembersList(Collection<Member> values) {
        return values.stream()
                .map(member -> new Document(mapMember(member)))
                .collect(Collectors.toList());
    }

    private Member mapMemberFrom(Document document) {
        UUID profileId = document.get(FIELD_MEMBER_PROFILE_ID, UUID.class);
        Player player = sessions.findPlayerByProfile(profileId).orElse(new Player(profileId, null));
        MembershipRole role = MembershipRole.valueOf(document.getString(FIELD_MEMBER_ROLE));
        MembershipState state = MembershipState.valueOf(document.getString(FIELD_MEMBER_STATE));
        PublicProfile profile = profiles.getProfile(RequestContext.SERVER, ProfileServiceServer.GetProfileRequest.byId(profileId)).profile.toPublic();
        return new Member(player, profile, role, state);
    }

    private static Map<String, Object> mapMember(Member member) {
        return Map.of(
                FIELD_MEMBER_ROLE, member.membershipRole.name(),
                FIELD_MEMBER_STATE, member.membershipState.name(),
                FIELD_MEMBER_PROFILE_ID, member.player.profile
        );
    }

    private static Map<String, Object> mapMember(UUID profile, MembershipRole role, MembershipState state) {
        return Map.of(
                FIELD_MEMBER_ROLE, role.name(),
                FIELD_MEMBER_STATE, state.name(),
                FIELD_MEMBER_PROFILE_ID, profile
        );
    }
}
