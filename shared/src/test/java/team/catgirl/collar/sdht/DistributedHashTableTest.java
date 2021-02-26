package team.catgirl.collar.sdht;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import team.catgirl.collar.sdht.events.AbstractSDHTEvent;
import team.catgirl.collar.sdht.events.Publisher;
import team.catgirl.collar.sdht.memory.InMemoryDistributedHashTable;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.TokenGenerator;

import java.util.UUID;
import java.util.function.Supplier;

public class DistributedHashTableTest {
    private DistributedHashTable table;
    private PublisherImpl publisher;
    private DistributedHashTableListenerImpl dhtListener;

    @Before
    public void setup() {
        publisher = new PublisherImpl();
        dhtListener = new DistributedHashTableListenerImpl();
        table = new InMemoryDistributedHashTable(publisher, () -> new ClientIdentity(UUID.randomUUID(), null, 1), dhtListener);
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

        Content removedContent = table.delete(record.key).orElse(null);
        Assert.assertEquals("Content can be removed", removedContent, content);
        Assert.assertFalse("content was removed", table.delete(record.key).isPresent());
        Assert.assertEquals("no records in hash table", ImmutableSet.of(), table.records());
        Assert.assertEquals("no records in namespace", ImmutableSet.of(), table.records(namespace));
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
