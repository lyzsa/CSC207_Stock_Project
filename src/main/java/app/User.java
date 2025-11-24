package App;

public class User {
    private final String username;
    private String password; // In a real application, this should be hashed
    private boolean isLoggedIn;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.isLoggedIn = false;
    }

    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }
}