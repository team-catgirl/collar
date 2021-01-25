package team.catgirl.collar.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import team.catgirl.collar.utils.Utils;

import java.util.UUID;

public class KeyPairGeneratorTest {

    @BeforeClass
    public static void register() {
        Utils.registerGPGProvider();
    }

    @Test
    public void generateKeyPair() throws Exception {
        ObjectMapper mapper = Utils.createObjectMapper();
        KeyPair keyPair = KeyPairGenerator.generateKeyPair(UUID.randomUUID());
        Assert.assertNotNull(keyPair.publicKey.bytes);
        Assert.assertNotNull(keyPair.publicKey.fingerPrint);
        Assert.assertNotNull(keyPair.privateKey.bytes);
        Assert.assertNotEquals(keyPair.publicKey, keyPair.privateKey);

        String serialized = mapper.writeValueAsString(keyPair);
        KeyPair deserialized = mapper.readValue(serialized, KeyPair.class);
        Assert.assertEquals(keyPair.publicKey.fingerPrint, deserialized.publicKey.fingerPrint);
        Assert.assertEquals(keyPair.publicKey.bytes, deserialized.publicKey.bytes);
        Assert.assertEquals(keyPair.privateKey.bytes, deserialized.privateKey.bytes);
    }
}
