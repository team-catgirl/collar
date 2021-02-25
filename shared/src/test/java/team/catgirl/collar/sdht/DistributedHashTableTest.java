package team.catgirl.collar.sdht;

import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import team.catgirl.collar.security.TokenGenerator;

import java.util.Set;
import java.util.UUID;

public class DistributedHashTableTest {
    private DistributedHashTable table;

    @Before
    public void setup() {
        table = new InMemoryDistributedHashTable();
    }

    @Test
    public void hashTableOperations() {
        UUID namespace = UUID.randomUUID();
        byte[] bytes = TokenGenerator.byteToken(256);
        UUID author = UUID.randomUUID();
        Content content = Content.from(bytes, author);
        UUID contentId = UUID.randomUUID();
        Record record = content.toRecord(new Key(namespace, contentId));

        Assert.assertTrue("content self validates", content.isValid());
        Assert.assertTrue("content validates against record", content.isValid(record));

        Content addedContent = table.putIfAbsent(record, content).orElse(null);
        Assert.assertEquals("content can be added", addedContent, content);

        Assert.assertFalse("Can put into hash table again", table.putIfAbsent(record, content).isPresent());
        Assert.assertTrue("Record exists", table.get(record).isPresent());

        Assert.assertEquals("has record", ImmutableSet.of(record), table.records());
        Assert.assertEquals("has record in namespace", ImmutableSet.of(record), table.records(namespace));
        Assert.assertEquals("does not have a record in random namespace", ImmutableSet.of(), table.records(UUID.randomUUID()));

        Content removedContent = table.remove(record).orElse(null);
        Assert.assertEquals("Content can be removed", removedContent, content);
        Assert.assertFalse("content was removed", table.remove(record).isPresent());
        Assert.assertEquals("no records in hash table", ImmutableSet.of(), table.records());
        Assert.assertEquals("no records in namespace", ImmutableSet.of(), table.records(namespace));
    }
}
