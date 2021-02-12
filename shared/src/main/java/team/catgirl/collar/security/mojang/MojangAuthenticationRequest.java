package team.catgirl.collar.security.mojang;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MojangAuthenticationRequest {
    @JsonProperty("username")
    public final String username;

    @JsonProperty("password")
    public final String password;

    @JsonProperty("agent")
    public final Agent agent = new Agent();

    public MojangAuthenticationRequest(String u, String p){username=u;password=p;}
}

class Agent {
    public final String name = "Minecraft";
    public final int version = 1;
    public Agent(){}
}
