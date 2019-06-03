package io.jenkins.plugins.io.jenkins.plugins.graphql;

import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.ModelObject;
import hudson.model.queue.FoldableAction;
import jenkins.model.RunAction2;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ClassUtilsTest {
    @Test
    public void getParentInterfaces() {
        assertThat(
            ClassUtils.getAllInterfaces(CauseAction.class),
            Matchers.containsInAnyOrder(
                ModelObject.class,
                RunAction2.class,
                Action.class,
                FoldableAction.class
            )
        );
    }

}
