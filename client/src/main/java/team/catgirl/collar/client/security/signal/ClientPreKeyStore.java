package team.catgirl.collar.client.security.signal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.PreKeyStore;
import team.catgirl.collar.client.HomeDirectory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class ClientPreKeyStore implements PreKeyStore {

    private final File file;
    private final ReentrantReadWriteLock lock;
    private final State state;
    private final ObjectMapper mapper;

    public ClientPreKeyStore(File file, State state, ObjectMapper mapper) {
        this.file = file;
        this.state = state;
        this.mapper = mapper;
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lockInterruptibly();
            byte[] bytes = state.preKeyRecords.get(preKeyId);
            if (bytes == null || bytes.length == 0) {
                throw new InvalidKeyIdException("no key with id " + preKeyId );
            }
            return new PreKeyRecord(bytes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new IllegalStateException("could not deserialize key " + preKeyId, e);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void storePreKey(int preKeyId, PreKeyRecord record) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lockInterruptibly();
            state.preKeyRecords.put(preKeyId, record.serialize());
            writeState();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new IllegalStateException("could not save state", e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean containsPreKey(int preKeyId) {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lockInterruptibly();
            byte[] bytes = state.preKeyRecords.get(preKeyId);
            return bytes != null && bytes.length != 0;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void removePreKey(int preKeyId) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lockInterruptibly();
            state.preKeyRecords.remove(preKeyId);
            writeState();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new IllegalStateException("could not save state", e);
        } finally {
            writeLock.unlock();
        }
    }

    public static ClientPreKeyStore from(HomeDirectory home, ObjectMapper mapper) throws IOException {
        File file = new File(home.security(), "clientPreKeyStore");
        State state;
        if (file.exists()) {
            state = mapper.readValue(file, State.class);
        } else {
            state = new State(new HashMap<>());
        }
        ClientPreKeyStore clientPreKeyStore = new ClientPreKeyStore(file, state, mapper);
        clientPreKeyStore.writeState();
        return clientPreKeyStore;
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

    private static class State {
        @JsonProperty("preKeyRecords")
        public final Map<Integer, byte[]> preKeyRecords;

        public State(@JsonProperty("preKeyRecords") Map<Integer, byte[]> preKeyRecords) {
            this.preKeyRecords = preKeyRecords;
        }
    }
}
