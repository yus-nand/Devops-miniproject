package com.grievanceportal.filter;

import com.grievanceportal.model.Role;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Which roles may use which URLs. Paths that are not listed are open to every logged-in user. */
public final class AccessRules {

    private static final Map<String, Set<Role>> RULES = new HashMap<>();

    static {
        Set<Role> policyholder = EnumSet.of(Role.POLICYHOLDER);
        Set<Role> staff = EnumSet.of(Role.OFFICER, Role.ADMIN);

        add("GET", "/grievances/new", policyholder);
        add("POST", "/grievances/new", policyholder);
        add("GET", "/grievances/submitted", policyholder);
        add("GET", "/grievances/mine", policyholder);
        add("GET", "/grievances/edit", policyholder);
        add("POST", "/grievances/edit", policyholder);
        add("GET", "/grievances", staff);
        add("POST", "/grievances/pickup", EnumSet.of(Role.OFFICER));
        add("POST", "/grievances/status", staff);
        add("GET", "/dashboard", EnumSet.of(Role.ADMIN));
    }

    private AccessRules() {
    }

    private static void add(String method, String path, Set<Role> roles) {
        RULES.put(method + " " + path, roles);
    }

    public static boolean isAllowed(String method, String path, Role role) {
        Set<Role> allowed = RULES.get(method.toUpperCase(Locale.ROOT) + " " + path);
        return allowed == null || allowed.contains(role);
    }
}
