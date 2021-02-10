package team.catgirl.collar.client.api.location;

import team.catgirl.collar.api.location.Location;
import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.features.ApiListener;
import team.catgirl.collar.security.mojang.MinecraftPlayer;

public interface LocationListener extends ApiListener {
    default void onLocationUpdated(Collar collar, LocationApi locationApi, MinecraftPlayer player, Location location) {};
}
