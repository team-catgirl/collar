package team.catgirl.collar.server.services.entities;

import com.google.common.collect.ImmutableSet;
import team.catgirl.collar.api.entities.Entity;
import team.catgirl.collar.api.entities.EntityType;
import team.catgirl.collar.protocol.entities.UpdateEntitiesRequest;
import team.catgirl.collar.security.mojang.MinecraftPlayer;
import team.catgirl.collar.server.protocol.BatchProtocolResponse;
import team.catgirl.collar.server.services.groups.GroupService;
import team.catgirl.collar.server.session.SessionManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EntitiesService {
    private final SessionManager sessions;
    private final GroupService groups;
    private final ConcurrentHashMap<EntityKey, EntityRecord> entityRecords = new ConcurrentHashMap<>();

    public EntitiesService(SessionManager sessions, GroupService groups) {
        this.sessions = sessions;
        this.groups = groups;
    }

    public BatchProtocolResponse updateEntities(UpdateEntitiesRequest req) {
        MinecraftPlayer player = sessions.findPlayer(req.identity)
                .orElseThrow(() -> new IllegalStateException("could not find player for " + req.identity));

        Map<UUID, Entity> updatedEntities = req.entities.stream().filter(entity -> entity.type == EntityType.PLAYER).collect(Collectors.toMap(entity -> entity.id, entity -> entity));
        Map<UUID, Set<MinecraftPlayer>> entityPresenceUpdates = new HashMap<>();
        entityRecords.keySet().forEach(entityKey -> {
            entityRecords.compute(entityKey, (key, record) -> {
                Entity entity = updatedEntities.get(key.entity);
                if (entity != null) {
                    if (record == null) {
                        return new EntityRecord(player.server, entity, Set.of(player.id));
                    } else {
                        Set<UUID> playersReporting = new HashSet<>(record.playersReporting);
                        playersReporting.add(player.id);
                        entityPresenceUpdates.put(entity.id, playersReporting.stream().map(uuid -> new MinecraftPlayer(uuid, key.server)).collect(Collectors.toSet()));
                        return new EntityRecord(player.server, entity, playersReporting);
                    }
                } else {
                    if (record == null) {
                        return null;
                    } else {
                        Set<UUID> playersReporting = new HashSet<>(record.playersReporting);
                        playersReporting.remove(player.id);
                        entityPresenceUpdates.put(record.entity.id, playersReporting.stream().map(uuid -> new MinecraftPlayer(uuid, key.server)).collect(Collectors.toSet()));
                        return new EntityRecord(player.server, record.entity, playersReporting);
                    }
                }
            });
        });
        return groups.updateNearbyGroups(entityPresenceUpdates);
    }

    public static final class EntityKey {
        public final String server;
        public final UUID entity;

        public EntityKey(String server, UUID entity) {
            this.server = server;
            this.entity = entity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntityKey entityKey = (EntityKey) o;
            return server.equals(entityKey.server) && entity.equals(entityKey.entity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(server, entity);
        }
    }

    public static final class EntityRecord {
        public final Entity entity;
        public final Set<UUID> playersReporting;

        public EntityRecord(String server, Entity entity, Set<UUID> playersReporting) {
            this.entity = entity;
            this.playersReporting = playersReporting;
        }
    }
}
