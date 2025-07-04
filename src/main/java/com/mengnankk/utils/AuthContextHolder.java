package com.mengnankk.utils;

import java.util.List;

public class AuthContextHolder {
    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> phoneNumberHolder = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> rolesHolder = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> permissionsHolder = new ThreadLocal<>();

    public static void setUserId(Long userId) { userIdHolder.set(userId); }
    public static Long getUserId() { return userIdHolder.get(); }

    public static void setUsername(String username) { usernameHolder.set(username); }
    public static String getUsername() { return usernameHolder.get(); }

    public static void setPhoneNumber(String phoneNumber) { phoneNumberHolder.set(phoneNumber); }
    public static String getPhoneNumber() { return phoneNumberHolder.get(); }

    public static void setRoles(List<String> roles) { rolesHolder.set(roles); }
    public static List<String> getRoles() { return rolesHolder.get(); }

    public static void setPermissions(List<String> permissions) { permissionsHolder.set(permissions); }
    public static List<String> getPermissions() { return permissionsHolder.get(); }

    public static void clear() {
        userIdHolder.remove();
        usernameHolder.remove();
        phoneNumberHolder.remove();
        rolesHolder.remove();
        permissionsHolder.remove();
    }
}

