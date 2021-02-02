package team.catgirl.collar.client.security.signal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SessionStore;
import team.catgirl.collar.client.HomeDirectory;
import team.catgirl.collar.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ClientSessionStore implements SessionStore {

    private final File file;
    private final State state;
    private final ReentrantReadWriteLock lock;
    private final ObjectMapper mapper;

    public ClientSessionStore(File file, State state, ObjectMapper mapper) {
        this.file = file;
        this.state = state;
        this.lock = new ReentrantReadWriteLock();
        this.mapper = mapper;
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
                    .filter(key -> key.name.equals(name))
                    .map(stateKey -> stateKey.deviceId)
                    .collect(Collectors.toList());
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
            state.sessions.put(key, record.serialize());
            writeState();
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
            StateKey key = StateKey.from(address);
            return state.sessions.containsKey(key);
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
            state.sessions.remove(key);
            writeState();
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
            for (StateKey stateKey : new ArrayList<>(state.sessions.keySet())) {
                if (stateKey.name.equals(name)) {
                    state.sessions.remove(stateKey);
                }
            }
            writeState();
        } catch (InterruptedException e) {
            throw new RuntimeException();
        } catch (IOException e) {
            throw new IllegalStateException("Could not write state", e);
        } finally {
            writeLock.unlock();
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

    private static class State {
        @JsonProperty("sessions")
        public final Map<StateKey, byte[]> sessions;

        public State(@JsonProperty("sessions") Map<StateKey, byte[]> sessions) {
            this.sessions = sessions;
        }
    }


    private void writeState() throws IOException {
        mapper.writeValue(file, state);
    }

    public static ClientSessionStore from(HomeDirectory homeDirectory, ObjectMapper mapper) throws IOException {
        File file = new File(homeDirectory.security(), "clientSessionStore.json");
        State state;
        if (file.exists()) {
            state = Utils.createObjectMapper().readValue(file, State.class);
        } else {
            state = new State(new HashMap<>());
        }
        ClientSessionStore clientSessionStore = new ClientSessionStore(file, state, mapper);
        clientSessionStore.writeState();
        return clientSessionStore;
    }
}
