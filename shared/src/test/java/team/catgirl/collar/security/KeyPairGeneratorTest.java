package team.catgirl.collar.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import team.catgirl.collar.security.keys.KeyPair;
import team.catgirl.collar.security.keys.KeyPairGenerator;
import team.catgirl.collar.utils.Utils;

import java.util.Arrays;
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
        Assert.assertFalse(Arrays.equals(keyPair.publicKey.bytes, keyPair.privateKey.bytes));

        String serialized = mapper.writeValueAsString(keyPair);
        KeyPair deserialized = mapper.readValue(serialized, KeyPair.class);
        Assert.assertArrayEquals(keyPair.publicKey.fingerPrint, deserialized.publicKey.fingerPrint);
        Assert.assertArrayEquals(keyPair.publicKey.bytes, deserialized.publicKey.bytes);
        Assert.assertArrayEquals(keyPair.privateKey.bytes, deserialized.privateKey.bytes);
    }
}
