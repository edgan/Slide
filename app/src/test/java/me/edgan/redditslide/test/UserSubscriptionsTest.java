package me.edgan.redditslide.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import me.edgan.redditslide.CaseInsensitiveArrayList;
import me.edgan.redditslide.UserSubscriptions;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

/** Created by Alex Macleod on 28/03/2016. */
public class UserSubscriptionsTest {
    private final CaseInsensitiveArrayList subreddits =
            new CaseInsensitiveArrayList(
                    Arrays.asList(
                            "xyy",
                            "xyz",
                            "frontpage",
                            "mod",
                            "friends",
                            "random",
                            "aaa",
                            "pinned",
                            "pinned2"));

    @BeforeClass
    public static void setUp() {
        UserSubscriptions.pinned = new TestUtils.MockPreferences("pinned,pinned2");
    }

    @Test
    public void sortsSubreddits() {
        assertThat(
                UserSubscriptions.sort(subreddits),
                is(
                        new ArrayList<>(
                                Arrays.asList(
                                        "pinned",
                                        "pinned2",
                                        "frontpage",
                                        "all",
                                        "random",
                                        "friends",
                                        "mod",
                                        "aaa",
                                        "xyy",
                                        "xyz"))));
    }

    @Test
    public void sortsSubredditsNoExtras() {
        assertThat(
                UserSubscriptions.sortNoExtras(subreddits),
                is(
                        new ArrayList<>(
                                Arrays.asList(
                                        "pinned",
                                        "pinned2",
                                        "frontpage",
                                        "random",
                                        "friends",
                                        "mod",
                                        "aaa",
                                        "xyy",
                                        "xyz"))));
    }
}
