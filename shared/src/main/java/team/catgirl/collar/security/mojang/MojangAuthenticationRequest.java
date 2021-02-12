package team.catgirl.collar.security.mojang;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MojangAuthenticationRequest {
    @JsonProperty("username")
    public String username;

    @JsonProperty("password")
    public String password;

    @JsonProperty("agent")
    public Agent agent_ = new Agent();


    public MojangAuthenticationRequest(String u, String p){username=u;password=p;}
}

class Agent {
    public String name = "Minecraft";
    public int version = 1;
    public Agent(){}
}
