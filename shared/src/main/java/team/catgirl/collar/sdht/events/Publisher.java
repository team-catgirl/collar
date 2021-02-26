package team.catgirl.collar.sdht.events;

public interface Publisher {
    void publish(AbstractSDHTEvent event);
}
