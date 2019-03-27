package org.anon;

public class DBUtilException extends Exception {
    public DBUtilException(String s) {
        super("A problem has occurred while connecting to database: " + s);
    }
}
