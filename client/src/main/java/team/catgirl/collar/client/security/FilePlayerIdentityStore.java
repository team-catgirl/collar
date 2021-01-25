package team.catgirl.collar.client.security;

import org.jetbrains.annotations.NotNull;
import team.catgirl.collar.security.KeyPair;
import team.catgirl.collar.security.KeyPairGenerator;
import team.catgirl.collar.security.KeyPairGeneratorException;
import team.catgirl.collar.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class FilePlayerIdentityStore implements PlayerIdentityStore {

    private final File profilesDir;

    /**
     * Create a new FileIdentity Provider for the user
     * @param baseDir
     */
    public FilePlayerIdentityStore(File baseDir) {
        this.profilesDir = new File(baseDir,"collar/profiles/");
    }

    @Override
    public KeyPair keyPair(UUID player) throws IOException {
        File file = getKeyPairFile(player);
        if (!file.exists()) {
            return null;
        }
        return Utils.createObjectMapper().readValue(file, KeyPair.class);
    }

    @Override
    public KeyPair createKeyPair(UUID player) throws KeyPairGeneratorException, IOException {
        KeyPair keyPair = KeyPairGenerator.generateKeyPair(player);
        save(player, keyPair);
        return keyPair;
    }

    private void save(UUID player, KeyPair keyPair) throws IOException {
        File file = getKeyPairFile(player);
        Utils.createObjectMapper().writeValue(file, keyPair);
    }

    @NotNull
    private File getKeyPairFile(UUID player) {
        return new File(profilesDir, player.toString() + "/keypairs.json");
    }
}
