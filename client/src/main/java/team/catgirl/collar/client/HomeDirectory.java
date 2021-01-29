package team.catgirl.collar.client;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class HomeDirectory {

    private final File collarHome;

    private HomeDirectory(File collarHome) {
        this.collarHome = collarHome;
    }

    /**
     * @return directory containing all security info, such as public/private keys
     * @throws IOException if directories could not be created
     */
    public File security() throws IOException {
        File securityDir = new File(collarHome, "security");
        if (!securityDir.exists() && !securityDir.mkdirs()) {
            throw new IOException("could not make directory " + securityDir.getAbsolutePath());
        }
        return securityDir;
    }

    /**
     * @param mcHome of the minecraft client
     * @param playerId of the current player
     * @return collar home directory
     * @throws IOException if directories could not be created
     */
    public static HomeDirectory from(File mcHome, UUID playerId) throws IOException {
        File collar = new File(mcHome, "collar/" + playerId.toString());
        if (!collar.exists() && !collar.mkdirs()) {
            throw new IOException("could not make directory " + collar.getAbsolutePath());
        }
        return new HomeDirectory(collar);
    }
}
