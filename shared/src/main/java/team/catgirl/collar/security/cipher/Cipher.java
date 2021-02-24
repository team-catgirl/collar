package team.catgirl.collar.security.cipher;

import team.catgirl.collar.api.groups.Group;
import team.catgirl.collar.security.Identity;

// TODO: wrap all Throwables into a CypherException to make error handling more consistent where this is used
public interface Cipher {

    /**
     * @param bytes to crypt
     * @return encrypted bytes
     */
    byte[] crypt(byte[] bytes);

    /**
     * Crypt a message that can only be decrypted by this user
     * @param bytes to decrypt
     * @return decrypted bytes
     */
    byte[] decrypt(byte[] bytes);

    /**
     * Crypt a message to an individual identity
     * @param recipient of the message
     * @param bytes message
     * @return encrypted messaged
     */
    byte[] crypt(Identity recipient, byte[] bytes);

    /**
     * Decrypt a message from an individual identity
     * @param sender of the message
     * @param bytes crypted message
     * @return decrypted message
     */
    byte[] decrypt(Identity sender, byte[] bytes);

    /**
     * Crypt a message to a group
     * @param sender who is sending the group message
     * @param recipient group of the message
     * @param bytes of the message
     * @return encrypted message
     */
    byte[] crypt(Identity sender, Group recipient, byte[] bytes);

    /**
     * Decrypt a message from a group
     * @param sender who sent the group message
     * @param group group of the message
     * @param bytes of the message
     * @return decrypted message
     */
    byte[] decrypt(Identity sender, Group group, byte[] bytes);
}
