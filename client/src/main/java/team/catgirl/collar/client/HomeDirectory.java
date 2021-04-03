package team.catgirl.collar.client;

import java.io.File;
import java.io.IOException;

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
        return createDirectory("security");
    }

    public File profile() throws IOException {
        return createDirectory("profile");
    }

    public File dhtState() throws IOException {
        return createDirectory("dht");
    }

    private File createDirectory(String dht) throws IOException {
        File dir = new File(collarHome, dht);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("could not make directory " + dir.getAbsolutePath());
        }
        return dir;
    }

    /**
     * @param mcHome of the minecraft client
     * @param hostName of the collar server
     * @return collar home directory
     * @throws IOException if directories could not be created
     */
    public static HomeDirectory from(File mcHome, String hostName) throws IOException {
        File collar = new File(mcHome, "collar/" + hostName);
        if (!collar.exists() && !collar.mkdirs()) {
            throw new IOException("could not make directory " + collar.getAbsolutePath());
        }
        return new HomeDirectory(collar);
    }
}
