package src.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * This class provides utility methods for logging and thread management.
 * It is a final class and cannot be instantiated.
 */
public final class Common {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * Throws an IllegalStateException if an attempt is made.
     */
    private Common() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Returns a Logger instance associated with the class name provided.
     * The Logger's configuration is read from a 'logging.properties' file.
     *
     * @param <T>       the type of the class for which the Logger is to be obtained
     * @param className the Class object from which the class name will be obtained
     * @return a Logger instance associated with the class name
     * @throws RuntimeException if there is an IOException when reading the 'logging.properties' file
     */
    public static <T> Logger getLogger(Class<T> className) {
        InputStream stream = className.getClassLoader().getResourceAsStream("logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Logger.getLogger(className.getName());
    }

    /**
     * Returns the name of the current thread.
     * If the thread name is empty, it returns the thread ID as a string.
     *
     * @return the name of the current thread, or the thread ID if the name is empty
     */
    public static String getThreadName() {
        return Thread.currentThread().getName().trim().isEmpty()
                ? String.valueOf(Thread.currentThread().threadId())
                : Thread.currentThread().getName();
    }
}
