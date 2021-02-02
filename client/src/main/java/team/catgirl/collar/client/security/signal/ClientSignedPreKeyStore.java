package team.catgirl.collar.client.security.signal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyStore;
import team.catgirl.collar.client.HomeDirectory;
import team.catgirl.collar.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ClientSignedPreKeyStore implements SignedPreKeyStore {

    private final File file;
    private final State state;
    private final ReentrantReadWriteLock lock;
    private final ObjectMapper mapper;

    public ClientSignedPreKeyStore(File file, State state, ObjectMapper mapper) {
        this.file = file;
        this.state = state;
        this.mapper = mapper;
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lockInterruptibly();
            byte[] bytes = this.state.signedKeyRecords.get(signedPreKeyId);
            if (bytes == null || bytes.length == 0) {
                throw new InvalidKeyIdException("invalid keyId " + signedPreKeyId);
            }
            return new SignedPreKeyRecord(bytes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new IllegalStateException("could not load key", e);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lockInterruptibly();
            return this.state.signedKeyRecords.values().stream().map(bytes -> {
                try {
                    return new SignedPreKeyRecord(bytes);
                } catch (IOException e) {
                    throw new IllegalStateException("could not read key", e);
                }
            }).collect(Collectors.toList());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lockInterruptibly();
            this.state.signedKeyRecords.put(signedPreKeyId, record.serialize());
            writeState();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new IllegalStateException("could not save sate", e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean containsSignedPreKey(int signedPreKeyId) {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lockInterruptibly();
            return this.state.signedKeyRecords.containsKey(signedPreKeyId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void removeSignedPreKey(int signedPreKeyId) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lockInterruptibly();
            this.state.signedKeyRecords.remove(signedPreKeyId);
            writeState();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new IllegalStateException("could not save sate", e);
        } finally {
            writeLock.unlock();
        }
    }

    public static ClientSignedPreKeyStore from(HomeDirectory home, ObjectMapper mapper) throws IOException {
        File file = new File(home.security(), "signedPreKeyStore.json");
        State state;
        if (file.exists()) {
            state = Utils.createObjectMapper().readValue(file, State.class);
        } else {
            state = new State(new HashMap<>());
        }
        ClientSignedPreKeyStore clientSignedPreKeyStore = new ClientSignedPreKeyStore(file, state, mapper);
        clientSignedPreKeyStore.writeState();
        return clientSignedPreKeyStore;
    }

    private void writeState() throws IOException {
        mapper.writeValue(file, state);
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
        @JsonProperty("signedKeyRecords")
        public final Map<Integer, byte[]> signedKeyRecords;

        public State(@JsonProperty("signedKeyRecords") Map<Integer, byte[]> signedKeyRecords) {
            this.signedKeyRecords = signedKeyRecords;
        }
    }
}
