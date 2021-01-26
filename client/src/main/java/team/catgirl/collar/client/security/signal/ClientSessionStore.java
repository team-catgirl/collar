package team.catgirl.collar.client.security.signal;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SessionStore;
import team.catgirl.collar.client.HomeDirectory;
import team.catgirl.collar.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ClientSessionStore implements SessionStore {

    private final File file;
    private final State state;
    private final ReentrantReadWriteLock lock;

    public ClientSessionStore(File file, State state) {
        this.file = file;
        this.state = state;
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public SessionRecord loadSession(SignalProtocolAddress address) {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            if (containsSession(address)) {
                readLock.lockInterruptibly();
                try {
                    byte[] bytes = state.sessions.get(StateKey.from(address));
                    if (bytes == null || bytes.length == 0) {
                        throw new IllegalStateException("could not read state");
                    }
                    return new SessionRecord(bytes);
                } catch (IOException e) {
                    throw new IllegalStateException("could not read state", e);
                } finally {
                    readLock.unlock();
                }
            } else {
                return new SessionRecord();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Integer> getSubDeviceSessions(String name) {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lockInterruptibly();
            return state.sessions.keySet().stream()
                    .filter(key -> key.name.equals(name) && key.deviceId != 1)
                    .map(stateKey -> stateKey.deviceId).collect(Collectors.toList());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }  finally {
            readLock.unlock();
        }
    }

    @Override
    public void storeSession(SignalProtocolAddress address, SessionRecord record) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lockInterruptibly();
            StateKey key = StateKey.from(address);
            state.nameKeyRelationship.put(address.getName(), key);
            state.sessions.put(key, record.serialize());
            writeState(file, state);
        } catch (InterruptedException e) {
            throw new RuntimeException();
        } catch (IOException e) {
            throw new IllegalStateException("Could not write state", e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean containsSession(SignalProtocolAddress address) {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lockInterruptibly();
            return state.nameKeyRelationship.containsKey(address.getName());
        } catch (InterruptedException e) {
            throw new RuntimeException();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void deleteSession(SignalProtocolAddress address) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lockInterruptibly();
            StateKey key = StateKey.from(address);
            state.nameKeyRelationship.remove(address.getName(), key);
            state.sessions.remove(key);
            writeState(file, state);
        } catch (InterruptedException e) {
            throw new RuntimeException();
        } catch (IOException e) {
            throw new IllegalStateException("Could not write state", e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void deleteAllSessions(String name) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lockInterruptibly();
            StateKey key = state.nameKeyRelationship.get(name);
            state.nameKeyRelationship.remove(name, key);
            state.sessions.remove(key);
            writeState(file, state);
        } catch (InterruptedException e) {
            throw new RuntimeException();
        } catch (IOException e) {
            throw new IllegalStateException("Could not write state", e);
        } finally {
            writeLock.unlock();
        }
    }

    private static class State {
        @JsonProperty("nameKeyRelationship")
        public final Map<String, StateKey> nameKeyRelationship;
        @JsonProperty("sessions")
        public final Map<StateKey, byte[]> sessions;

        public State(@JsonProperty("nameKeyRelationship") Map<String, StateKey> nameKeyRelationship, @JsonProperty("sessions") Map<StateKey, byte[]> sessions) {
            this.nameKeyRelationship = nameKeyRelationship;
            this.sessions = sessions;
        }
    }


    private static void writeState(File file, State state) throws IOException {
        Utils.createObjectMapper().writeValue(file, state);
    }

    public static ClientSessionStore from(HomeDirectory homeDirectory) throws IOException {
        File file = new File(homeDirectory.security(), "clientSessionStore.json");
        State state;
        if (file.exists()) {
            state = Utils.createObjectMapper().readValue(file, State.class);
        } else {
            state = new State(new HashMap<>(), new HashMap<>());
            writeState(file, state);
        }
        return new ClientSessionStore(file, state);
    }
}
