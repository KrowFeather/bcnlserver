package zxylearn.bcnlserver.common;

import java.util.Map;

public class UserContext {
    private static final ThreadLocal<Map<String, String>> USER_INFO = new ThreadLocal<>();

    public static void setUser(Map<String, String> userMap) {
        USER_INFO.set(userMap);
    }

    public static String getUserId() {
        return USER_INFO.get() != null ? USER_INFO.get().get("userId") : null;
    }

    public static String getUserRole() {
        return USER_INFO.get() != null ? USER_INFO.get().get("role") : null;
    }

    public static void remove() {
        USER_INFO.remove();
    }
}