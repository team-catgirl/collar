package team.catgirl.collar.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.catgirl.collar.utils.Utils;

import java.io.IOException;
import java.util.function.Function;

public abstract class Response<T> implements Function<byte[], T> {

    public static Response<Void> noContent() {
        return new Response<Void>() {
            @Override
            public Void apply(byte[] bytes) {
                return null;
            }
        };
    }

    public static <T> Response<T> json(Class<T> tClass) {
        return new Response<T>() {
            @Override
            public T apply(byte[] bytes) {
                try {
                    return Utils.jsonMapper().readValue(bytes, tClass);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static Response<byte[]> bytes() {
        return new Response<byte[]>() {
            @Override
            public byte[] apply(byte[] bytes) {
                return bytes;
            }
        };
    }
}
