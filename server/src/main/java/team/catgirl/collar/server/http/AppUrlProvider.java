package team.catgirl.collar.server.http;

public interface AppUrlProvider {
    /**
     * URL for verifying the device
     * @param token to link identity and device
     * @return url
     */
    String deviceVerificationUrl(String token);
}
