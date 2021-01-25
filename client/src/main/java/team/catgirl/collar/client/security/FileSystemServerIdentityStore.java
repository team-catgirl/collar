package team.catgirl.collar.client.security;

import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class FileSystemServerIdentityStore extends AbstractServerIdentityStore {
    private final File serversDir;

    /**
     * @param baseDir e.g. minecraft home
     */
    public FileSystemServerIdentityStore(File baseDir) {
        this.serversDir = new File(baseDir,"collar/servers/");
    }

    @Override
    public ServerIdentity getIdentity(UUID server) {
        File file = new File(serversDir, server.toString() + ".json");
        if (!file.exists()) {
            return null;
        }
        try {
            return Utils.createObjectMapper().readValue(file, ServerIdentity.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ServerIdentity saveIdentity(ServerIdentity identity) {
        File file = new File(serversDir, identity.server.toString() + ".json");
        if (!file.exists()) {
            return null;
        }
        try {
            Utils.createObjectMapper().writeValue(file, ServerIdentity.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return identity;
    }
}
