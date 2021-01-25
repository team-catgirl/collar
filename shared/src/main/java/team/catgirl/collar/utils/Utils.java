package team.catgirl.collar.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;

public final class Utils {
    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public static void registerGPGProvider() {
        BouncyGPG.registerProvider();
    }

    private Utils() {}
}
