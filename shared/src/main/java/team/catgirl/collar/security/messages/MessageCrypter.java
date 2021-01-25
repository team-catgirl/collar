package team.catgirl.collar.security.messages;

import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.algorithms.DefaultPGPAlgorithmSuites;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.util.io.Streams;
import team.catgirl.collar.security.Identity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class MessageCrypter {

    private final KeyringConfig keyringConfig;

    public MessageCrypter(KeyringConfig keyringConfig) {
        this.keyringConfig = keyringConfig;
    }

    public String encryptString(String message, Identity sender, Identity recipient) throws MessageCrypterException {
        ArrayList<Identity> identities = new ArrayList<>();
        if (recipient != null) {
            identities.add(recipient);
        }
        return encryptStringWithMultipleRecipients(message, sender, identities);
    }

    public String encryptStringWithMultipleRecipients(String message, Identity sender, List<Identity> recipients) throws MessageCrypterException {
        byte[] bytes = encryptBytesWithMultipleRecipients(message.getBytes(StandardCharsets.UTF_8), sender, recipients);
        return BaseEncoding.base64().encode(bytes);
    }

    public byte[] encryptBytes(byte[] message, Identity sender, Identity recipient) throws MessageCrypterException {
        return encryptBytesWithMultipleRecipients(message, sender, ImmutableList.of(recipient));
    }

    public byte[] encryptBytesWithMultipleRecipients(byte[] message, Identity sender, List<Identity> recipients) throws MessageCrypterException {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(result)) {
                try (OutputStream outputStream = BouncyGPG
                        .encryptToStream()
                        .withConfig(keyringConfig)
                        .selectUidByAnyUidPart()
                        .setReferenceDateForKeyValidityTo(Instant.MAX)
                        .withAlgorithms(DefaultPGPAlgorithmSuites.strongSuite())
                        .toRecipients(recipients.stream().map(Identity::getName).collect(Collectors.joining()))
                        .andSignWith(sender.getName())
                        .binaryOutput()
                        .andWriteTo(bufferedOutputStream)) {
                    outputStream.write(message);
                } catch (NoSuchAlgorithmException | SignatureException | PGPException | NoSuchProviderException e) {
                    throw new MessageCrypterException("Problem encoding message", e);
                }
            }
            return result.toByteArray();
        } catch (IOException e) {
            throw new MessageCrypterException("IO problem", e);
        }
    }

    public String decryptString(String message, Identity sender) throws MessageCrypterException {
        byte[] bytes = decryptBytes(BaseEncoding.base64().decode(message), sender);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] decryptBytes(byte[] message, Identity sender) throws MessageCrypterException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (InputStream cipherTextStream = new ByteArrayInputStream(message)) {
                try (BufferedOutputStream bufferedOut = new BufferedOutputStream(output)) {
                    try (InputStream plaintextStream = BouncyGPG
                            .decryptAndVerifyStream()
                            .withConfig(keyringConfig)
                            .andRequireSignatureFromAllKeys(sender.getName())
                            .fromEncryptedInputStream(cipherTextStream)) {
                        Streams.pipeAll(plaintextStream, bufferedOut);
                    } catch (PGPException | NoSuchProviderException e) {
                        throw new MessageCrypterException("Problem decoding message", e);
                    }
                }
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new MessageCrypterException("IO problem", e);
        }
    }
}
