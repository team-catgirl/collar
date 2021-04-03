package team.catgirl.collar.sdht;

import com.google.common.collect.ImmutableSet;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import team.catgirl.collar.sdht.cipher.ContentCipher;
import team.catgirl.collar.sdht.events.AbstractSDHTEvent;
import team.catgirl.collar.sdht.events.Publisher;
import team.catgirl.collar.sdht.impl.DHTNamespaceState;
import team.catgirl.collar.sdht.impl.DefaultDistributedHashTable;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.TokenGenerator;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class DistributedHashTableTest {
    private DistributedHashTable table;
    private final ContentCipher cipher = new ContentCipher() {
        @Override
        public byte[] crypt(ClientIdentity identity, UUID namespace, Content content) {
            return content.serialize();
        }

        @Override
        public Content decrypt(ClientIdentity identity, UUID namespace, byte[] bytes) {
            return new Content(bytes);
        }

        @Override
        public boolean accepts(UUID namespace) {
            return true;
        }
    };

    @Before
    public void setup() {
        PublisherImpl publisher = new PublisherImpl();
        DistributedHashTableListenerImpl dhtListener = new DistributedHashTableListenerImpl();
        DHTNamespaceState state = new DHTNamespaceState(Files.createTempDir());
        table = new DefaultDistributedHashTable(publisher, () -> new ClientIdentity(UUID.randomUUID(), null, 1), cipher, state, dhtListener);
    }

    @Test
    public void hashTableOperations() {
        UUID namespace = UUID.randomUUID();
        byte[] bytes = TokenGenerator.byteToken(256);
        Content content = Content.from(bytes, String.class);
        UUID contentId = UUID.randomUUID();
        Record record = content.toRecord(new Key(namespace, contentId));

        Assert.assertTrue("content self validates", content.isValid());
        Assert.assertTrue("content validates against record", content.isValid(record));

        Content addedContent = table.put(record.key, content).orElse(null);
        Assert.assertEquals("content can be added", addedContent, content);

        Assert.assertFalse("Can put into hash table again", table.put(record.key, content).isPresent());
        Assert.assertTrue("Record exists", table.get(record.key).isPresent());

        Assert.assertEquals("has record", ImmutableSet.of(record), table.records());
        Assert.assertEquals("has record in namespace", ImmutableSet.of(record), table.records(namespace));
        Assert.assertEquals("does not have a record in random namespace", ImmutableSet.of(), table.records(UUID.randomUUID()));

        Optional<Content> delete = table.delete(record.key);
        Content removedContent = delete.orElseThrow(() -> new IllegalStateException("not found"));
        Assert.assertEquals("content was removed", removedContent.state, State.DELETED);
    }

    @Test
    public void contentRoundTrip() {
        String value = "hello world";
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        HashCode hashCode = Hashing.sha256().hashBytes(bytes);
        Content content = new Content(hashCode.asBytes(), bytes, String.class, new Date().getTime(), State.EXTANT);
        byte[] serialized = content.serialize();
        Content deserializedContent = new Content(serialized);

        Assert.assertEquals(content.type, deserializedContent.type);
        Assert.assertArrayEquals(content.checksum, deserializedContent.checksum);
        Assert.assertArrayEquals(content.bytes, deserializedContent.bytes);
    }

    public static final class PublisherImpl implements Publisher {

        AbstractSDHTEvent lastEvent;

        @Override
        public void publish(AbstractSDHTEvent event) {
            this.lastEvent = event;
        }
    }

    public static final class DistributedHashTableListenerImpl implements DistributedHashTableListener {
        @Override
        public void onAdd(Key key, Content content) {

        }

        @Override
        public void onRemove(Key key, Content content) {

        }
    }
}
