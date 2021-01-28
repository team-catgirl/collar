package team.catgirl.collar.server.mongo;

import com.mongodb.client.MongoDatabase;
import org.junit.After;
import org.junit.Before;

public abstract class DatabaseTest {
    protected MongoDatabase db;

    @Before
    public void setupDb() {
        db = Mongo.getTestingDatabase();
    }

    @After
    public void tearDownDb() {
        db.drop();
    }
}
