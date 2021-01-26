package team.catgirl.collar.client.security.signal;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.libsignal.util.KeyHelper;
import team.catgirl.collar.client.HomeDirectory;
import team.catgirl.collar.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public final class ClientIdentityKeyStore implements IdentityKeyStore {

    private final File file;
    private final ReentrantReadWriteLock lock;
    private final State state;

    private ClientIdentityKeyStore(State state, File file) {
        this.state = state;
        this.file = file;
        this.lock = new ReentrantReadWriteLock();
    }

    public static ClientIdentityKeyStore from(HomeDirectory homeDirectory) throws IOException {
        File file = new File(homeDirectory.security(), "identityKeyStore.json");
        State state;
        if (file.exists()) {
           state = Utils.createObjectMapper().readValue(file, State.class);
        } else {
            IdentityKeyPair identityKeyPair = KeyHelper.generateIdentityKeyPair();
            int registrationId = KeyHelper.generateRegistrationId(false);
            state = new State(registrationId, identityKeyPair.serialize(), identityKeyPair.getPublicKey().getFingerprint(), new HashMap<>());
            writeState(file, state);
        }
        return new ClientIdentityKeyStore(state, file);
    }

    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        ReadLock readLock = lock.readLock();
        try {
            readLock.lockInterruptibly();
            return new IdentityKeyPair(state.identityKeyPair);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("cant load identity key pair", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getLocalRegistrationId() {
        ReadLock readLock = lock.readLock();
        try {
            readLock.lockInterruptibly();
            return this.state.localRegistrationId;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void saveIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
        WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lockInterruptibly();
            this.state.trusted.put(StateKey.from(address), State.from(identityKey));
            writeState(file, state);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save state", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
        ReadLock readLock = lock.readLock();
        try {
            readLock.lockInterruptibly();
            IdentityState identityState = this.state.trusted.get(StateKey.from(address));
            return identityState != null && identityState.fingerprint.equals(identityKey.getFingerprint());
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        } finally {
            readLock.unlock();
        }
    }

    private static void writeState(File file, State state) throws IOException {
        Utils.createObjectMapper().writeValue(file, state);
    }

    private static final class State {
        @JsonProperty("localRegistrationId")
        public final int localRegistrationId;
        @JsonProperty("identityKeyPair")
        public final byte[] identityKeyPair;
        @JsonProperty("identityFingerPrint")
        public final String identityFingerPrint;
        @JsonProperty("trusted")
        public final Map<StateKey, IdentityState> trusted;

        public State(
                @JsonProperty("localRegistrationId") int localRegistrationId,
                @JsonProperty("identityKeyPair") byte[] identityKeyPair,
                @JsonProperty("identityFingerPrint") String identityFingerPrint,
                @JsonProperty("trusted") Map<StateKey, IdentityState> trusted) {
            this.localRegistrationId = localRegistrationId;
            this.identityKeyPair = identityKeyPair;
            this.identityFingerPrint = identityFingerPrint;
            this.trusted = trusted;
        }

        public static IdentityState from(IdentityKey identityKey) {
            return new IdentityState(identityKey.getPublicKey().serialize(), identityKey.getFingerprint());
        }
    }

    private static final class IdentityState {
        public final byte[] publicKey;
        public final String fingerprint;

        public IdentityState(
                @JsonProperty("publicKey") byte[] publicKey,
                @JsonProperty("fingerprint") String fingerprint) {
            this.publicKey = publicKey;
            this.fingerprint = fingerprint;
        }
    }
}
