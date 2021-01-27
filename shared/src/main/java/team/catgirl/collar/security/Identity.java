package team.catgirl.collar.security;

import java.util.UUID;

public interface Identity {
    UUID id();
    byte[] preKeyBundle();
}
