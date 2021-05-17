package team.catgirl.collar.client.security.signal.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.whispersystems.libsignal.groups.SenderKeyName;
import org.whispersystems.libsignal.groups.state.SenderKeyRecord;
import org.whispersystems.libsignal.groups.state.SenderKeyStore;
import team.catgirl.collar.client.HomeDirectory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientSenderKeyStore implements SenderKeyStore {

    private final File file;
    private final ReentrantReadWriteLock lock;
    private final State state;
    private final ObjectMapper mapper;

    private ClientSenderKeyStore(State state, File file, ObjectMapper mapper) {
        this.state = state;
        this.file = file;
        this.mapper = mapper;
        this.lock = new ReentrantReadWriteLock();
    }

    public static ClientSenderKeyStore from(HomeDirectory homeDirectory, ObjectMapper mapper) throws IOException {
        File file = new File(homeDirectory.security(), "clientSenderKeyStore");
        State state;
        if (file.exists()) {
            state = mapper.readValue(file, State.class);
        } else {
            state = new State(new HashMap<>());
        }
        ClientSenderKeyStore store = new ClientSenderKeyStore(state, file, mapper);
        store.writeState();
        return store;
    }

    @Override
    public void storeSenderKey(SenderKeyName senderKeyName, SenderKeyRecord record) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lockInterruptibly();
            state.records.put(fromSenderKeyName(senderKeyName), new Record(record.serialize()));
            writeState();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("could not write state", e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public SenderKeyRecord loadSenderKey(SenderKeyName senderKeyName) {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lockInterruptibly();
            Record record = state.records.get(fromSenderKeyName(senderKeyName));
            if (record == null) {
                return new SenderKeyRecord();
            } else {
                return new SenderKeyRecord(record.bytes);
            }
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException("could not read state", e);
        } finally {
            readLock.unlock();
        }
    }

    public void clear() {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lockInterruptibly();
            state.records.clear();
            writeState();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("could not write state", e);
        } finally {
            writeLock.unlock();
        }
    }

    private static Key fromSenderKeyName(SenderKeyName senderKeyName) {
        return new Key(senderKeyName.getGroupId(), senderKeyName.getSender().getName(), senderKeyName.getSender().getDeviceId());
    }

    public void writeState() throws IOException {
        ReentrantReadWriteLock.WriteLock lock = this.lock.writeLock();
        try {
            lock.lockInterruptibly();
            mapper.writeValue(file, state);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    public void delete() throws IOException {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lockInterruptibly();
            if (!file.delete()) {
                throw new IOException("Could not delete " + file.getAbsolutePath());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            writeLock.lock();
        }
    }

    public void removeGroupSession(SenderKeyName senderKeyName) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lockInterruptibly();
            state.records.remove(fromSenderKeyName(senderKeyName));
            writeState();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            writeLock.lock();
        }
    }

    public static class Key {
        @JsonProperty("groupId")
        public final String groupId;
        @JsonProperty("name")
        public final String name;
        @JsonProperty("deviceId")
        public final Integer deviceId;

        public Key(@JsonProperty("groupId") String groupId,
                   @JsonProperty("name") String name,
                   @JsonProperty("deviceId") Integer deviceId) {
            this.groupId = groupId;
            this.name = name;
            this.deviceId = deviceId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return groupId.equals(key.groupId) && name.equals(key.name) && deviceId.equals(key.deviceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, name, deviceId);
        }
    }

    public static class Record {
        @JsonProperty("bytes")
        public final byte[] bytes;

        public Record(@JsonProperty("bytes") byte[] bytes) {
            this.bytes = bytes;
        }
    }

    public static class State {
        @JsonProperty("records")
        public final Map<Key, Record> records;

        public State(@JsonProperty("records") Map<Key, Record> records) {
            this.records = records;
        }
    }
}
