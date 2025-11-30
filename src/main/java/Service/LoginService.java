package Service;

import java.util.HashMap;
import java.util.Map;

public class LoginService {
    private Map<String, User> users;
    private User currentUser;

    public LoginService() {
        this.users = new HashMap<>();
        // Add some test users (in a real app, this would be in a database)
        users.put("user1", new User("user1", "password1"));
        users.put("user2", new User("user2", "password2"));
    }

    /**
     * Attempts to log in a user with the given credentials
     * @param username The username to log in with
     * @param password The password to verify
     * @return true if login is successful, false otherwise
     */
    public boolean login(String username, String password) {
        User user = users.get(username);
        if (user != null && user.checkPassword(password)) {
            user.setLoggedIn(true);
            currentUser = user;
            return true;
        }
        return false;
    }

    /**
     * Logs out the current user
     */
    public void logout() {
        if (currentUser != null) {
            currentUser.setLoggedIn(false);
            currentUser = null;
        }
    }

    /**
     * Creates a new user account
     * @param username The desired username
     * @param password The desired password
     * @return true if account was created successfully, false if username already exists
     */
    public boolean signUp(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return false;
        }

        if (users.containsKey(username)) {
            return false; // User already exists
        }
        users.put(username, new User(username, password));
        return true;
    }

    /**
     * Checks if a user is currently logged in
     * @return true if a user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return currentUser != null && currentUser.isLoggedIn();
    }

    /**
     * Gets the username of the currently logged-in user
     * @return The username or null if no user is logged in
     */
    public String getCurrentUser() {
        return currentUser != null ? currentUser.getUsername() : null;
    }
}