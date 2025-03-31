package me.edgan.redditslide.Activities;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;



import me.edgan.redditslide.Adapters.SubredditPosts;
import me.edgan.redditslide.Constants;
import me.edgan.redditslide.Fragments.SubmissionsView;
import me.edgan.redditslide.R;
import me.edgan.redditslide.Reddit;
import me.edgan.redditslide.SettingValues;
import me.edgan.redditslide.Visuals.ColorPreferences;
import me.edgan.redditslide.Visuals.Palette;
import me.edgan.redditslide.util.LogUtil;
import me.edgan.redditslide.util.StringUtil;

public class MainPagerAdapter extends FragmentStatePagerAdapter {
    protected SubmissionsView mCurrentFragment;
    private MainActivity mainActivity;

    // Modified constructor to accept MainActivity
    public MainPagerAdapter(MainActivity mainActivity, FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.mainActivity = mainActivity;

        mainActivity.pager.clearOnPageChangeListeners();
        mainActivity.pager.addOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageScrolled(
                            int position, float positionOffset, int positionOffsetPixels) {
                        if (positionOffset == 0) {
                            mainActivity.header.animate()
                                    .translationY(0)
                                    .setInterpolator(new LinearInterpolator())
                                    .setDuration(180);
                            if (position < mainActivity.usedArray.size()) {
                                mainActivity.sidebarController.doSubSidebarNoLoad(mainActivity.usedArray.get(position));
                            }
                        }
                    }

                    @Override
                    public void onPageSelected(final int position) {
                        if (position >= mainActivity.usedArray.size()) return;

                        Reddit.currentPosition = position;
                        mainActivity.selectedSub = mainActivity.usedArray.get(position);
                        SubmissionsView page = (SubmissionsView) getCurrentFragment();

                        if (mainActivity.hea != null) {
                            mainActivity.hea.setBackgroundColor(Palette.getColor(mainActivity.selectedSub));
                            if (mainActivity.accountsArea != null) {
                                mainActivity.accountsArea.setBackgroundColor(
                                        Palette.getDarkerColor(mainActivity.selectedSub));
                            }
                        }

                        int colorFrom = ((ColorDrawable) mainActivity.header.getBackground()).getColor();
                        int colorTo = Palette.getColor(mainActivity.selectedSub);

                        ValueAnimator colorAnimation =
                                ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);

                        colorAnimation.addUpdateListener(
                                new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animator) {
                                        int color = (int) animator.getAnimatedValue();

                                        mainActivity.header.setBackgroundColor(color);

                                        if (Build.VERSION.SDK_INT
                                                >= Build.VERSION_CODES.LOLLIPOP) {
                                            int finalColor = Palette.getDarkerColor(color);

                                            if (SettingValues.alwaysBlackStatusbar) {
                                                finalColor = android.graphics.Color.BLACK;
                                            }

                                            mainActivity.getWindow().setStatusBarColor(finalColor);

                                            if (SettingValues.colorNavBar) {
                                                mainActivity.getWindow()
                                                        .setNavigationBarColor(
                                                                Palette.getDarkerColor(color));
                                            }
                                        }
                                    }
                                });
                        colorAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
                        colorAnimation.setDuration(200);
                        colorAnimation.start();

                        mainActivity.setRecentBar(mainActivity.selectedSub);

                        if (SettingValues.single || mainActivity.mTabLayout == null) {
                            // Smooth out the fading animation for the toolbar subreddit search UI
                            if ((SettingValues.subredditSearchMethod
                                                    == Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR
                                            || SettingValues.subredditSearchMethod
                                                    == Constants.SUBREDDIT_SEARCH_METHOD_BOTH)
                                    && mainActivity.findViewById(R.id.toolbar_search).getVisibility()
                                            == View.VISIBLE) {
                                new Handler()
                                        .postDelayed(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mainActivity.getSupportActionBar()
                                                                .setTitle(mainActivity.selectedSub);
                                                    }
                                                },
                                                mainActivity.ANIMATE_DURATION + mainActivity.ANIMATE_DURATION_OFFSET);
                            } else {
                                mainActivity.getSupportActionBar().setTitle(mainActivity.selectedSub);
                            }
                        } else {
                            mainActivity.mTabLayout.setSelectedTabIndicatorColor(
                                    new ColorPreferences(mainActivity)
                                            .getColor(mainActivity.selectedSub));
                        }
                        if (page != null && page.adapter != null) {
                            SubredditPosts p = page.adapter.dataSet;
                            if (p.offline && !mainActivity.isRestart) {
                                p.doMainActivityOffline(mainActivity, p.displayer);
                            }
                        }
                    }
                });

        if (mainActivity.pager.getAdapter() != null) {
            mainActivity.pager.getAdapter().notifyDataSetChanged();
            mainActivity.pager.setCurrentItem(1);
            mainActivity.pager.setCurrentItem(0);
        }
    }

    @Override
    public int getCount() {
        if (mainActivity.usedArray == null) {
            return 1;
        } else {
            return mainActivity.usedArray.size();
        }
    }

    @NonNull
    @Override
    public Fragment getItem(int i) {
        SubmissionsView f = new SubmissionsView();
        Bundle args = new Bundle();
        String name;
        if (i < mainActivity.usedArray.size()) {
            if (mainActivity.multiNameToSubsMap.containsKey(mainActivity.usedArray.get(i))) {
                name = mainActivity.multiNameToSubsMap.get(mainActivity.usedArray.get(i));
            } else {
                name = mainActivity.usedArray.get(i);
            }
            args.putString("id", name);
        } else {
            args.putString("id", "frontpage");
        }
        f.setArguments(args);

        return f;
    }

    @Override
    public void setPrimaryItem(
            @NonNull ViewGroup container, int position, @NonNull Object object) {
        // Ensure position is valid before accessing usedArray
        if (position >= 0 && position < mainActivity.usedArray.size()) {
            if (mainActivity.reloadItemNumber == position || mainActivity.reloadItemNumber < 0) {
                super.setPrimaryItem(container, position, object);
                // Check size again before calling doSetPrimary
                if (position < mainActivity.usedArray.size()) {
                    doSetPrimary(object, position);
                }
            } else {
                // Ensure reloadItemNumber is valid
                if (mainActivity.reloadItemNumber >= 0 && mainActivity.reloadItemNumber < mainActivity.usedArray.size()) {
                    if (mainActivity.multiNameToSubsMap.containsKey(mainActivity.usedArray.get(mainActivity.reloadItemNumber))) {
                        mainActivity.shouldLoad = mainActivity.multiNameToSubsMap.get(mainActivity.usedArray.get(mainActivity.reloadItemNumber));
                    } else {
                        mainActivity.shouldLoad = mainActivity.usedArray.get(mainActivity.reloadItemNumber);
                    }
                } else {
                    mainActivity.shouldLoad = "frontpage";
                }
            }
        } else {
             // Handle invalid position, maybe log an error or do nothing
            Log.e(LogUtil.getTag(), "Invalid position in setPrimaryItem: " + position);
        }
    }


    @Override
    public Parcelable saveState() {
        return null;
    }

    public void doSetPrimary(Object object, int position) {
         // Add null check for usedArray and bounds check for position
        if (mainActivity.usedArray == null || position < 0 || position >= mainActivity.usedArray.size()) {
            Log.e(LogUtil.getTag(), "Invalid state in doSetPrimary: usedArray=" + mainActivity.usedArray + ", position=" + position);
            return;
        }

        if (object != null
                && getCurrentFragment() != object
                && position != mainActivity.toOpenComments
                && object instanceof SubmissionsView) {
            mainActivity.shouldLoad = mainActivity.usedArray.get(position);
            if (mainActivity.multiNameToSubsMap.containsKey(mainActivity.usedArray.get(position))) {
                mainActivity.shouldLoad = mainActivity.multiNameToSubsMap.get(mainActivity.usedArray.get(position));
            } else {
                mainActivity.shouldLoad = mainActivity.usedArray.get(position);
            }

            mCurrentFragment = ((SubmissionsView) object);
            if (mCurrentFragment.posts == null && mCurrentFragment.isAdded()) {
                mCurrentFragment.doAdapter();
            }
        }
    }

    // Public getter for mCurrentFragment
    public Fragment getCurrentFragment() {
        return mCurrentFragment;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (mainActivity.usedArray != null && position < mainActivity.usedArray.size()) {
            return StringUtil.abbreviate(mainActivity.usedArray.get(position), 25);
        } else {
            return "";
        }
    }
}