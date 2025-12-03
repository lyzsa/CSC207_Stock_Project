package use_case.watchlist;

import data_access.FileUserDataAccessObject;
import entity.User;
import entity.UserFactory;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WatchlistInteractorTest {
    @Test
    void successAddTest() {
        JSONObject object = new JSONObject();
        WatchlistInputData inputData = new WatchlistInputData("Bob", object);

        UserFactory factory = new UserFactory();
        User bob = factory.create("Bob", "password");

        WatchlistUserDataAccessInterface test = new FileUserDataAccessObject("test1.csv", factory);
        test.save(bob);

        WatchlistOutputBoundary successPresenter = new WatchlistOutputBoundary() {
            @Override
            public void prepareSuccessView(String username, WatchlistOutputData data) {
                assertEquals("Bob", username);
                assertTrue(data.getWatchlist().contains(object));
                assertEquals(object, data.getWatchlist().get(0));
            }

            @Override
            public void prepareFailView(String error) {
                fail("Failed to add to watchlist.");
            }
        };

        WatchlistInputBoundary interactor =
                new WatchlistInteractor(test, successPresenter);

        interactor.execute(inputData);
    }

    @Test
    void failUserDoesntExistTest() {
        JSONObject object = new JSONObject();
        UserFactory factory = new UserFactory();
        WatchlistInputData inputData = new WatchlistInputData("Bob", object);

        WatchlistUserDataAccessInterface test = new FileUserDataAccessObject("test2.csv", factory);

        WatchlistOutputBoundary failurePresenter = new WatchlistOutputBoundary() {
            @Override
            public void prepareSuccessView(String username, WatchlistOutputData data) {
                fail("User doesn't exist, shouldn't work.");
            }

            @Override
            public void prepareFailView(String error) {
                assertEquals("User doesn't exist.", error);
            }
        };

        WatchlistInputBoundary interactor =
                new WatchlistInteractor(test, failurePresenter);

        interactor.execute(inputData);
    }

    @Test
    void successMultipleItemsTest() {
        JSONObject object1 = new JSONObject();
        JSONObject object2 = new JSONObject();
        WatchlistInputData inputData = new WatchlistInputData("Bob", object1);

        UserFactory factory = new UserFactory();
        User bob = factory.create("Bob", "password");
        bob.getWatchlist().add(object2);
        WatchlistUserDataAccessInterface test = new FileUserDataAccessObject("test3.csv", factory);
        test.save(bob);

        WatchlistOutputBoundary successPresenter = new WatchlistOutputBoundary() {
            @Override
            public void prepareSuccessView(String username, WatchlistOutputData data) {
                assertEquals("Bob", username);
                assertEquals(2, data.getWatchlist().size());
                assertTrue(data.getWatchlist().contains(object1));
                assertTrue(data.getWatchlist().contains(object2));
            }

            @Override
            public void prepareFailView(String error) {
                fail("Failed to contain both.");
            }
        };

        WatchlistInputBoundary interactor =
                new WatchlistInteractor(test, successPresenter);

        interactor.execute(inputData);
    }
}
