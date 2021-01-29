package team.catgirl.collar.server.services.authentication;


import org.jasypt.util.binary.AES256BinaryEncryptor;

public class TokenCrypter {

    private final AES256BinaryEncryptor encryptor;

    public TokenCrypter(String password) {
        encryptor = new AES256BinaryEncryptor();
        encryptor.setPassword(password);
    }

    public byte[] decrypt(byte[] bytes) {
        return encryptor.decrypt(bytes);
    }

    public byte[] crypt(byte[] bytes) {
        return encryptor.encrypt(bytes);
    }
}
