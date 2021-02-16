package team.catgirl.collar.client.api.identity;

import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.ApiListener;
import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.security.ClientIdentity;

public interface IdentityListener extends ApiListener {
    /**
     * Fired when an identity is trusted by the client
     * @param collar client
     * @param api identity
     * @param identityStore identity store for the client
     * @param identity trusted
     */
    default void onIdentityTrusted(Collar collar, IdentityApi api, ClientIdentityStore identityStore, ClientIdentity identity) {};
}
