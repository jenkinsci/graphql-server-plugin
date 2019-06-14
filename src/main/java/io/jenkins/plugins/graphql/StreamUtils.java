package io.jenkins.plugins.graphql;

import hudson.model.Item;
import hudson.model.View;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import jenkins.model.Jenkins;

public class StreamUtils {
    private StreamUtils() {}

    public static boolean isAllowed(Object item) {
        if (item instanceof Item) {
            return ((Item) item).hasPermission(Item.READ);
        }
        if (item instanceof View) {
            return ((View) item).hasPermission(View.READ);
        }
        if (item instanceof AccessControlled) {
            return ((AccessControlled) item).hasPermission(Permission.READ) || ((AccessControlled) item).hasPermission(Jenkins.READ);
        }
        // not something that has access control rules, so by default just allow it
        // Most notiable, Project and Run Actions
        return true;
    }
}
