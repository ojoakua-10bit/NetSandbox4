package org.anon;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;

public class DBUtil implements Closeable {
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    public DBUtil() throws DBUtilException {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/netsandbox", "java", "netsandbox");
        }
        catch (SQLException e) {
            throw new DBUtilException(e.getMessage());
        }
    }

    public boolean isRegisteredUser(String username) throws DBUtilException {
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM users WHERE username='" + username + "';");
            boolean result = resultSet.next();

            statement.close();
            resultSet.close();
            return result;
        }
        catch (SQLException e) {
            throw new DBUtilException(e.getMessage());
        }
    }

    public boolean authenticateUser(String username, String password) throws DBUtilException {
        try {
            boolean result;
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM users WHERE username='" + username + "';");
            if (resultSet.next()) {
                String hashedPassword = Digestive.md5(password);
                result = hashedPassword.equals(resultSet.getString(3));
            }
            else result = false;

            resultSet.close();
            statement.close();
            return result;
        }
        catch (SQLException e) {
            throw new DBUtilException(e.getMessage());
        }
    }

    public void registerUser(String username, String password) throws DBUtilException {
        try {
            statement = connection.createStatement();
            statement.executeQuery("INSERT users (username, password) VALUES ('" + username
                    + "', '" + Digestive.md5(password) +"');");
            statement.close();
        } catch (SQLException e) {
            throw new DBUtilException(e.getMessage());
        }
    }

    public void changePassword(String username, String newPassword) throws DBUtilException {
        try {
            statement = connection.createStatement();
            statement.executeQuery("UPDATE users SET password='" + Digestive.md5(newPassword)
                    + "' WHERE username='" + username + "';");
            statement.close();
        } catch (SQLException e) {
            throw new DBUtilException(e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }
}