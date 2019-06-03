package io.jenkins.plugins.io.jenkins.plugins.graphql;

import hudson.model.Action;
import hudson.model.CauseAction;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ClassUtilsTest {
    @Test
    public void getParentInterfaces() {
        assertEquals(
            Arrays.asList(
                Action.class
            ),
            ClassUtils.getAllInterfaces(CauseAction.class)
        );
    }

}
