package team.catgirl.collar.sdht;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Hashing;
import team.catgirl.collar.utils.IO;

import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class Content {

    private static final int VERSION = 1;
    private static final int MAX_SIZE = 2000;

    /**
     * Checksum of content
     */
    @JsonProperty("checksum")
    public final byte[] checksum;

    /**
     * Content
     */
    @JsonProperty("bytes")
    public final byte[] bytes;

    @JsonProperty("type")
    public final Class<?> type;

    @JsonProperty("version")
    public final long version;

    @JsonProperty("state")
    public final State state;

    public Content(@JsonProperty("checksum") byte[] checksum,
                   @JsonProperty("bytes") byte[] bytes,
                   @JsonProperty("type") Class<?> type,
                   @JsonProperty("version") long version,
                   @JsonProperty("state") State state) {
        this.checksum = checksum;
        this.bytes = bytes;
        this.type = type;
        this.version = version;
        this.state = state;
    }

    public Content(byte[] bytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            try (DataInputStream dataStream = new DataInputStream(inputStream)) {
                dataStream.readInt();
                String clazz = dataStream.readUTF();
                type = this.getClass().getClassLoader().loadClass(clazz);
                this.checksum = IO.readBytes(dataStream);
                this.bytes = IO.readBytes(dataStream);
                this.version = dataStream.readLong();
                this.state = State.from(dataStream.readInt());
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("could not read content", e);
        }
    }

    /**
     * Checks that the content within is valid
     * @return validity
     */
    public boolean isValid() {
        if (bytes == null) {
            return true;
        }
        if (bytes.length > MAX_SIZE) {
            return false;
        }
        byte[] checksum = createChecksum(bytes);
        return Arrays.equals(this.checksum, checksum);
    }

    /**
     * Checks that the record checksum and the content checksum match
     * @param record to test
     * @return validity
     */
    public boolean isValid(Record record) {
        byte[] checksum = createChecksum(bytes);
        return Arrays.equals(record.checksum, checksum);
    }

    /**
     * Create a record based on the key and its content
     * @param key addressing the content
     * @return record
     */
    public Record toRecord(Key key) {
        return new Record(key, checksum, version);
    }

    /**
     * Create content from bytes
     * @param bytes to store
     * @param contentType type
     * @return content
     */
    public static Content from(byte[] bytes, Class<?> contentType) {
        assertSize(bytes);
        byte[] checksum = createChecksum(bytes);
        return new Content(checksum, bytes, contentType, 1, State.EXTANT);
    }

    public byte[] serialize() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (DataOutputStream dataStream = new DataOutputStream(outputStream)) {
                dataStream.writeInt(VERSION);
                dataStream.writeUTF(this.type.getName());
                IO.writeBytes(dataStream, checksum);
                IO.writeBytes(dataStream, bytes);
                dataStream.writeLong(version);
                dataStream.writeInt(state.value);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("cannot serialize content", e);
        }
    }

    /**
     * Tests if the content is old and currently deleted
     * @param content to test
     * @return if it can be pruned or not
     */
    public static boolean isDead(Content content) {
        return content.state == State.DELETED && System.currentTimeMillis() > (content.version + TimeUnit.DAYS.toMillis(30));
    }

    private static byte[] createChecksum(byte[] bytes) {
        return Hashing.sha256().hashBytes(bytes).asBytes();
    }

    private static void assertSize(byte[] bytes) {
        if (bytes.length > MAX_SIZE) {
            throw new IllegalStateException("content size exceeds " + MAX_SIZE + " bytes");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Content content = (Content) o;
        return version == content.version && Arrays.equals(checksum, content.checksum) && Arrays.equals(bytes, content.bytes) && Objects.equals(type, content.type) && state == content.state;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type, version, state);
        result = 31 * result + Arrays.hashCode(checksum);
        result = 31 * result + Arrays.hashCode(bytes);
        return result;
    }
}
