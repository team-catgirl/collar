package team.catgirl.collar.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

public final class Utils {

    private static final ObjectMapper JSON_MAPPER;
    private static final ObjectMapper MESSAGE_PACK_MAPPER;

    static {
        SimpleModule keys = new SimpleModule();
        keys.addKeyDeserializer(MinecraftPlayer.class, new KeyDeserializer() {
            @Override
            public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
                String id = key.substring(0, key.lastIndexOf(":"));
                String server = key.substring(key.lastIndexOf(":") + 1);
                return new MinecraftPlayer(UUID.fromString(id), server);
            }
        });
        keys.addKeySerializer(MinecraftPlayer.class, new JsonSerializer<MinecraftPlayer>() {
            @Override
            public void serialize(MinecraftPlayer value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeFieldName(value.id.toString() + ":" + value.server);
            }
        });

        JSON_MAPPER = new ObjectMapper()
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .registerModules(keys);

        MESSAGE_PACK_MAPPER = new ObjectMapper(new MessagePackFactory())
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .registerModules(keys);
    }

    public static final SecureRandom SECURERANDOM;

    static {
        try {
            SECURERANDOM = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ObjectMapper jsonMapper() {
        return JSON_MAPPER;
    }

    public static ObjectMapper messagePackMapper() {
        return MESSAGE_PACK_MAPPER;
    }

    public static SecureRandom secureRandom() {
        return SECURERANDOM;
    }

    private Utils() {}
}
