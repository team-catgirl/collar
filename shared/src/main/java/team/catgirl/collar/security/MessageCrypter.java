package team.catgirl.collar.security;

import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.algorithms.DefaultPGPAlgorithmSuites;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;
import org.bouncycastle.openpgp.PGPException;
import team.catgirl.collar.security.Principal;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public final class MessageCrypter {

    private final KeyringConfig keyringConfig;

    public MessageCrypter(KeyringConfig keyringConfig) {
        this.keyringConfig = keyringConfig;
    }

    public byte[] encryptBytes(Principal sender, Principal recipient, byte[] message) throws IOException {
        return encryptBytes(sender, ImmutableList.of(recipient), message);
    }

    public byte[] encryptBytes(Principal sender, List<Principal> recipients, byte[] message) throws IOException {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(result)) {
                try (OutputStream outputStream = BouncyGPG
                        .encryptToStream()
                        .withConfig(keyringConfig)
                        .selectUidByAnyUidPart()
                        .setReferenceDateForKeyValidityTo(Instant.MAX)
                        .withAlgorithms(DefaultPGPAlgorithmSuites.strongSuite())
                        .toRecipients(recipients.stream().map(Principal::getName).collect(Collectors.joining()))
                        .andSignWith(sender.getName())
                        .binaryOutput()
                        .andWriteTo(bufferedOutputStream)) {
                    outputStream.write(message);
                } catch (NoSuchAlgorithmException | SignatureException | PGPException | NoSuchProviderException e) {
                    e.printStackTrace();
                }
            }
            return result.toByteArray();
        }
    }

    public String encryptString(Principal sender, Principal recipient, String message) throws IOException {
        return encryptString(sender, ImmutableList.of(recipient), message);
    }

    public String encryptString(Principal sender, List<Principal> recipients, String message) throws IOException {
        byte[] bytes = encryptBytes(sender, recipients, message.getBytes(StandardCharsets.UTF_8));
        return BaseEncoding.base64().encode(bytes);
    }

}
