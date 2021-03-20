package team.catgirl.collar.client.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Horribly necessary as Minecraft does not ship the unlimited strength JCE policy files
 * This is needed for the signal crypto to work at all
 * See https://stackoverflow.com/questions/1179672/how-to-avoid-installing-unlimited-strength-jce-policy-files-when-deploying-an/44056166
 */
public class Crypto {
    private static final Logger LOGGER = Logger.getLogger(Crypto.class.getName());
    private static final int RESTRICTED_CRYPTO_MIN_REVISION = 161;

    public static void removeCryptographyRestrictions() {
        if (!isRestrictedCryptography()) {
            LOGGER.fine("Cryptography restrictions removal not needed");
            return;
        }
        try {
            /*
             * Do the following, but with reflection to bypass access checks:
             *
             * JceSecurity.isRestricted = false;
             * JceSecurity.defaultPolicy.perms.clear();
             * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
             */
            final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
            final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
            final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");

            final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
            isRestrictedField.setAccessible(true);
            final Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(isRestrictedField, isRestrictedField.getModifiers() & ~Modifier.FINAL);
            isRestrictedField.set(null, false);

            final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
            defaultPolicyField.setAccessible(true);
            final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);

            final Field perms = cryptoPermissions.getDeclaredField("perms");
            perms.setAccessible(true);
            ((Map<?, ?>) perms.get(defaultPolicy)).clear();

            final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            defaultPolicy.add((Permission) instance.get(null));

            LOGGER.fine("Successfully removed cryptography restrictions");
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to remove cryptography restrictions", e);
        }
    }

    private static boolean isRestrictedCryptography() {
        final String name = System.getProperty("java.runtime.name");
        final String ver = System.getProperty("java.version");
        if (!name.equals("Java(TM) SE Runtime Environment") && !ver.startsWith("1.8.0")) {
            return false;
        }
        final int revision = Integer.parseInt(ver.substring(ver.indexOf("_") + 1));
        return revision < RESTRICTED_CRYPTO_MIN_REVISION;
    }
}
