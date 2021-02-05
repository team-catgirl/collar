package team.catgirl.collar.server.http;

public interface AppUrlProvider {

    /**
     * @return homepage of the collar web app
     */
    String homeUrl();

    /**
     * @return user login page
     */
    String loginUrl();

    /**
     * @return user sign up page
     */
    String signupUrl();

    /**
     * URL for verifying the device
     * @param token to link identity and device
     * @return url
     */
    String deviceVerificationUrl(String token);
}
