package team.catgirl.collar.server.http;

public interface AppUrlProvider {
    String deviceVerificationUrl(String token);
}
