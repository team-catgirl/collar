package team.catgirl.collar.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import okhttp3.OkHttpClient;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import team.catgirl.collar.api.session.Player;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.StringTokenizer;
import java.util.UUID;

public final class Utils {

    private static final OkHttpClient http = new OkHttpClient();
    private static final ObjectMapper JSON_MAPPER;
    private static final ObjectMapper MESSAGE_PACK_MAPPER;

    static {
        SimpleModule keys = new SimpleModule();
        keys.addKeyDeserializer(Player.class, new KeyDeserializer() {
            @Override
            public Object deserializeKey(String key, DeserializationContext ctxt) {
                StringTokenizer tokenizer = new StringTokenizer(key, ":");
                String profileId = tokenizer.nextToken();
                String minecraftId = tokenizer.nextToken();
                String minecraftServer = tokenizer.nextToken();
                return new Player(UUID.fromString(profileId), new MinecraftPlayer(UUID.fromString(minecraftId), minecraftServer));
            }
        });
        keys.addKeySerializer(Player.class, new JsonSerializer<Player>() {
            @Override
            public void serialize(Player value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeFieldName(value.profile + ":" + value.minecraftPlayer.id + ":" + value.minecraftPlayer.id);
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
            SECURERANDOM = SecureRandom.getInstanceStrong();
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

    public static OkHttpClient http() { return http; }

    private Utils() {}
}
