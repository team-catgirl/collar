package team.catgirl.collar.security.mojang;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthenticationJSON {
    @JsonProperty("username")
    public String username;

    @JsonProperty("password")
    public String password;

    @JsonProperty("agent")
    public agent agent_ = new agent();


    public AuthenticationJSON(String u, String p){username=u;password=p;}
}

class agent {
    public String name = "Minecraft";
    public int version = 1;
    public agent(){}
}
