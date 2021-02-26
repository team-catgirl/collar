package team.catgirl.collar.client.sdht;

import team.catgirl.collar.client.Collar;
import team.catgirl.collar.client.api.ApiListener;
import team.catgirl.collar.sdht.Content;
import team.catgirl.collar.sdht.Key;

public interface SDHTListener extends ApiListener {
    default void onRecordAdded(Collar collar, SDHTApi sdhtApi, Key key, Content content) {};
    default void onRecordRemoved(Collar collar, SDHTApi sdhtApi, Key key, Content content) {};
}
