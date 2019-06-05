package io.jenkins.plugins.graphql;

import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.ModelObject;
import hudson.model.queue.FoldableAction;
import hudson.plugins.git.GitTagAction;
import jenkins.model.RunAction2;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    @Test
    public void GitTagAction() {
        assertArrayEquals(
            new Set[]{
                new HashSet<Class>(
                    Arrays.asList(
                        hudson.model.Action.class,
                        hudson.model.BuildBadgeAction.class,
                        hudson.model.ModelObject.class,
                        hudson.search.SearchableModelObject.class,
                        hudson.model.Describable.class,
                        hudson.search.SearchItem.class,
                        jenkins.model.RunAction2.class

                    )
                )
            },
            new Set[]{ClassUtils.getAllInterfaces(GitTagAction.class)}
        );
    }

}
