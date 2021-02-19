package team.catgirl.collar.security;

import team.catgirl.collar.api.groups.Group;

// TODO: wrap all Throwables into a CypherException to make error handling more consistent where this is used
public interface Cypher {
    /**
     * Crypt a message to an individual identity
     * @param recipient of the message
     * @param bytes message
     * @return crypted messaged
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
     * @return crypted message
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
