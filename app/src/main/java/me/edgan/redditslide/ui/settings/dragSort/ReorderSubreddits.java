/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.edgan.redditslide.ui.settings.dragSort;

import static me.edgan.redditslide.UserSubscriptions.setPinned;

import android.app.Dialog;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;
import com.nambimobile.widgets.efab.FabOption;

import me.edgan.redditslide.Activities.BaseActivityAnim;
import me.edgan.redditslide.Authentication;
import me.edgan.redditslide.CaseInsensitiveArrayList;
import me.edgan.redditslide.R;
import me.edgan.redditslide.SettingValues;
import me.edgan.redditslide.UserSubscriptions;
import me.edgan.redditslide.Visuals.ColorPreferences;
import me.edgan.redditslide.Visuals.Palette;
import me.edgan.redditslide.ui.settings.SettingsThemeFragment;
import me.edgan.redditslide.util.BlendModeUtil;
import me.edgan.redditslide.util.DisplayUtil;

import net.dean.jraw.models.MultiReddit;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.SubredditSearchPaginator;
import net.dean.jraw.paginators.UserSubredditsPaginator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ReorderSubreddits extends BaseActivityAnim {

    private CaseInsensitiveArrayList subs;
    private CustomAdapter adapter;
    private RecyclerView recyclerView;
    private String input;
    public static final String MULTI_REDDIT = "/m/";
    MenuItem subscribe;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reorder_subs, menu);

        subscribe = menu.findItem(R.id.alphabetize_subscribe);
        subscribe.setChecked(SettingValues.alphabetizeOnSubscribe);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.refresh:
                done = 0;
                final Dialog d =
                        new MaterialDialog.Builder(ReorderSubreddits.this)
                                .title(R.string.general_sub_sync)
                                .content(R.string.misc_please_wait)
                                .progress(true, 100)
                                .cancelable(false)
                                .show();
                new AsyncTask<Void, Void, ArrayList<String>>() {
                    @Override
                    protected ArrayList<String> doInBackground(Void... params) {
                        ArrayList<String> newSubs =
                                new ArrayList<>(
                                        UserSubscriptions.syncSubreddits(ReorderSubreddits.this));
                        UserSubscriptions.syncMultiReddits(ReorderSubreddits.this);
                        return newSubs;
                    }

                    @Override
                    protected void onPostExecute(ArrayList<String> newSubs) {
                        d.dismiss();
                        // Determine if we should insert subreddits at the end of the list or sorted
                        boolean sorted = (subs.equals(UserSubscriptions.sortNoExtras(subs)));
                        Resources res = getResources();

                        for (String s : newSubs) {
                            if (!subs.contains(s)) {
                                done++;
                                subs.add(s);
                            }
                        }
                        if (sorted && done > 0) {
                            subs = UserSubscriptions.sortNoExtras(subs);
                            adapter = new CustomAdapter(subs);
                            recyclerView.setAdapter(adapter);
                        } else if (done > 0) {
                            adapter.notifyDataSetChanged();
                            recyclerView.smoothScrollToPosition(subs.size());
                        }
                        new AlertDialog.Builder(ReorderSubreddits.this)
                                .setTitle(R.string.reorder_sync_complete)
                                .setMessage(
                                        res.getQuantityString(
                                                R.plurals.reorder_subs_added, done, done))
                                .setPositiveButton(R.string.btn_ok, null)
                                .show();
                    }
                }.execute();
                return true;
            case R.id.alphabetize:
                subs = UserSubscriptions.sortNoExtras(subs);
                adapter = new CustomAdapter(subs);
                //  adapter.setHasStableIds(true);
                recyclerView.setAdapter(adapter);
                return true;
            case R.id.alphabetize_subscribe:
                SettingValues.prefs
                        .edit()
                        .putBoolean(
                                SettingValues.PREF_ALPHABETIZE_SUBSCRIBE,
                                !SettingValues.alphabetizeOnSubscribe)
                        .apply();
                SettingValues.alphabetizeOnSubscribe = !SettingValues.alphabetizeOnSubscribe;
                if (subscribe != null) subscribe.setChecked(SettingValues.alphabetizeOnSubscribe);
                return true;
            case R.id.info:
                new AlertDialog.Builder(ReorderSubreddits.this)
                        .setTitle(R.string.reorder_subs_FAQ)
                        .setMessage(R.string.sorting_faq)
                        .show();
                return true;
        }
        return false;
    }

    @Override
    public void onPause() {
        try {
            UserSubscriptions.setSubscriptions(new CaseInsensitiveArrayList(subs));
            SettingsThemeFragment.changed = true;
        } catch (Exception e) {

        }
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (isMultiple) {
            chosen = new ArrayList<>();
            doOldToolbar();
            adapter.notifyDataSetChanged();
            isMultiple = false;
        } else {
            super.onBackPressed();
        }
    }

    private ArrayList<String> chosen = new ArrayList<>();
    HashMap<String, Boolean> isSubscribed;
    private boolean isMultiple;
    private int done = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        disableSwipeBackLayout();
        super.onCreate(savedInstanceState);
        applyColorTheme();
        setContentView(R.layout.activity_sort);
        setupAppBar(R.id.toolbar, R.string.settings_manage_subscriptions, false, true);
        mToolbar.setPopupTheme(new ColorPreferences(this).getFontStyle().getBaseId());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        isSubscribed = new HashMap<>();
        if (Authentication.isLoggedIn) {
            new AsyncTask<Void, Void, Void>() {
                boolean success = true;

                @Override
                protected Void doInBackground(Void... params) {
                    ArrayList<Subreddit> subs = new ArrayList<>();
                    UserSubredditsPaginator p =
                            new UserSubredditsPaginator(Authentication.reddit, "subscriber");
                    try {
                        while (p.hasNext()) {
                            subs.addAll(p.next());
                        }
                    } catch (Exception e) {
                        success = false;
                        return null;
                    }

                    for (Subreddit s : subs) {
                        isSubscribed.put(s.getDisplayName().toLowerCase(Locale.ENGLISH), true);
                    }

                    if (UserSubscriptions.multireddits == null) {
                        UserSubscriptions.loadMultireddits();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    if (success) {
                        d.dismiss();
                        doShowSubs();
                    } else {
                        new AlertDialog.Builder(ReorderSubreddits.this)
                                .setTitle(R.string.err_title)
                                .setMessage(R.string.misc_please_try_again_soon)
                                .setCancelable(false)
                                .setPositiveButton(R.string.btn_ok, (dialog, which) -> finish())
                                .show();
                    }
                }

                Dialog d;

                @Override
                protected void onPreExecute() {
                    d =
                            new MaterialDialog.Builder(ReorderSubreddits.this)
                                    .progress(true, 100)
                                    .content(R.string.misc_please_wait)
                                    .title(R.string.reorder_loading_title)
                                    .cancelable(false)
                                    .show();
                }
            }.execute();
        } else {
            doShowSubs();
        }
    }

    public void doShowSubs() {
        subs = new CaseInsensitiveArrayList(UserSubscriptions.getSubscriptions(this));
        recyclerView = (RecyclerView) findViewById(R.id.subslist);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(null);

        DragSortRecycler dragSortRecycler = new DragSortRecycler();
        dragSortRecycler.setViewHandleId();
        dragSortRecycler.setFloatingAlpha();
        dragSortRecycler.setAutoScrollSpeed();
        dragSortRecycler.setAutoScrollWindow();

        dragSortRecycler.setOnItemMovedListener(
                new DragSortRecycler.OnItemMovedListener() {
                    @Override
                    public void onItemMoved(int from, int to) {
                        if (to == subs.size()) {
                            to -= 1;
                        }
                        String item = subs.remove(from);
                        subs.add(to, item);
                        adapter.notifyDataSetChanged();

                        CaseInsensitiveArrayList pinned = UserSubscriptions.getPinned();
                        if (pinned.contains(item) && pinned.size() != 1) {
                            pinned.remove(item);
                            if (to > pinned.size()) {
                                to = pinned.size();
                            }
                            pinned.add(to, item);
                            setPinned(pinned);
                        }
                    }
                });

        dragSortRecycler.setOnDragStateChangedListener(
                new DragSortRecycler.OnDragStateChangedListener() {
                    @Override
                    public void onDragStart() {}

                    @Override
                    public void onDragStop() {}
                });

        {
            final FabOption collectionFab =
                    (FabOption) findViewById(R.id.sort_fabOption_collection);
            collectionFab.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (UserSubscriptions.multireddits != null
                                    && !UserSubscriptions.multireddits.isEmpty()) {
                                new AlertDialog.Builder(ReorderSubreddits.this)
                                        .setTitle(R.string.create_or_import_multi)
                                        .setPositiveButton(
                                                R.string.btn_new, (dialog, which) -> doCollection())
                                        .setNegativeButton(
                                                R.string.btn_import_multi,
                                                (dialog, which) -> {
                                                    final String[] multis =
                                                            new String
                                                                    [UserSubscriptions.multireddits
                                                                            .size()];
                                                    int i = 0;
                                                    for (MultiReddit m :
                                                            UserSubscriptions.multireddits) {
                                                        multis[i] = m.getDisplayName();
                                                        i++;
                                                    }

                                                    new MaterialDialog.Builder(
                                                                    ReorderSubreddits.this)
                                                            .title(
                                                                    R.string
                                                                            .reorder_subreddits_title)
                                                            .items(multis)
                                                            .itemsCallbackSingleChoice(
                                                                    -1,
                                                                    new MaterialDialog
                                                                            .ListCallbackSingleChoice() {
                                                                        @Override
                                                                        public boolean onSelection(
                                                                                MaterialDialog
                                                                                        dialog,
                                                                                View itemView,
                                                                                int which,
                                                                                CharSequence text) {

                                                                            String name =
                                                                                    multis[which];
                                                                            MultiReddit r =
                                                                                    UserSubscriptions
                                                                                            .getMultiredditByDisplayName(
                                                                                                    name);

                                                                            // Construct the new URL
                                                                            // format for
                                                                            // multireddits
                                                                            String username =
                                                                                    Authentication
                                                                                            .name;
                                                                            String multiName =
                                                                                    r
                                                                                            .getDisplayName();
                                                                            String sortMode =
                                                                                    "hot"; // default sort mode

                                                                            // Create the new URL
                                                                            // format
                                                                            String url =
                                                                                    String.format(
                                                                                            "api/user/%s/m/%s",
                                                                                            username,
                                                                                            multiName);

                                                                            int pos =
                                                                                    addSubAlphabetically(
                                                                                            MULTI_REDDIT
                                                                                                    + r
                                                                                                            .getDisplayName());
                                                                            UserSubscriptions
                                                                                    .setSubNameToProperties(
                                                                                            MULTI_REDDIT
                                                                                                    + r
                                                                                                            .getDisplayName(),
                                                                                            url);
                                                                            adapter
                                                                                    .notifyDataSetChanged();
                                                                            recyclerView
                                                                                    .smoothScrollToPosition(
                                                                                            pos);
                                                                            return false;
                                                                        }
                                                                    })
                                                            .show();
                                                })
                                        .show();
                            } else {
                                doCollection();
                            }
                        }
                    });
        }
        {
            final FabOption subFab = (FabOption) findViewById(R.id.sort_fabOption_sub);
            subFab.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MaterialDialog.Builder b =
                                    new MaterialDialog.Builder(ReorderSubreddits.this)
                                            .title(R.string.reorder_add_or_search_subreddit)
                                            .alwaysCallInputCallback()
                                            .input(
                                                    getString(R.string.reorder_subreddit_name),
                                                    null,
                                                    false,
                                                    new MaterialDialog.InputCallback() {
                                                        @Override
                                                        public void onInput(
                                                                MaterialDialog dialog,
                                                                CharSequence raw) {
                                                            input = raw.toString();
                                                        }
                                                    })
                                            .positiveText(R.string.btn_add)
                                            .onPositive(
                                                    new MaterialDialog.SingleButtonCallback() {
                                                        @Override
                                                        public void onClick(
                                                                MaterialDialog dialog,
                                                                DialogAction which) {
                                                            new AsyncGetSubreddit().execute(input);
                                                        }
                                                    })
                                            .negativeText(R.string.btn_cancel);
                            b.show();
                        }
                    });
        }
        {
            final FabOption domainFab = (FabOption) findViewById(R.id.sort_fabOption_domain);
            domainFab.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new MaterialDialog.Builder(ReorderSubreddits.this)
                                    .title(R.string.reorder_add_domain)
                                    .alwaysCallInputCallback()
                                    .input(
                                            "example.com"
                                                    + getString(
                                                            R.string.reorder_domain_placeholder),
                                            null,
                                            false,
                                            new MaterialDialog.InputCallback() {
                                                @Override
                                                public void onInput(
                                                        MaterialDialog dialog, CharSequence raw) {
                                                    input =
                                                            raw.toString()
                                                                    .replaceAll(
                                                                            "\\s", ""); // remove
                                                    // whitespace from
                                                    // input
                                                    dialog.getActionButton(DialogAction.POSITIVE)
                                                            .setEnabled(input.contains("."));
                                                }
                                            })
                                    .positiveText(R.string.btn_add)
                                    .inputRange(1, 35)
                                    .onPositive(
                                            new MaterialDialog.SingleButtonCallback() {
                                                @Override
                                                public void onClick(
                                                        MaterialDialog dialog, DialogAction which) {
                                                    try {
                                                        String url = (input);

                                                        List<String> sortedSubs =
                                                                UserSubscriptions.sortNoExtras(
                                                                        subs);

                                                        if (sortedSubs.equals(subs)) {
                                                            subs.add(url);
                                                            subs =
                                                                    UserSubscriptions.sortNoExtras(
                                                                            subs);
                                                            adapter = new CustomAdapter(subs);
                                                            recyclerView.setAdapter(adapter);
                                                        } else {
                                                            int pos = addSubAlphabetically(url);
                                                            adapter.notifyDataSetChanged();
                                                            recyclerView.smoothScrollToPosition(
                                                                    pos);
                                                        }
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                        // todo make this better
                                                        new AlertDialog.Builder(
                                                                        ReorderSubreddits.this)
                                                                .setTitle(R.string.reorder_url_err)
                                                                .setMessage(
                                                                        R.string
                                                                                .misc_please_try_again)
                                                                .show();
                                                    }
                                                }
                                            })
                                    .negativeText(R.string.btn_cancel)
                                    .show();
                        }
                    });
        }
        recyclerView.addItemDecoration(dragSortRecycler);
        recyclerView.addOnItemTouchListener(dragSortRecycler);
        recyclerView.addOnScrollListener(dragSortRecycler.getScrollListener());
        dragSortRecycler.setViewHandleId();

        if (subs != null && !subs.isEmpty()) {
            adapter = new CustomAdapter(subs);
            //  adapter.setHasStableIds(true);
            recyclerView.setAdapter(adapter);
        } else {
            subs = new CaseInsensitiveArrayList();
        }

        recyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING) {
                            diff += dy;
                        } else {
                            diff = 0;
                        }
                    }
                });
    }

    public int diff;

    public void doCollection() {
        if (UserSubscriptions.multireddits != null && !UserSubscriptions.multireddits.isEmpty()) {
            final String[] multis = new String[UserSubscriptions.multireddits.size()];
            int i = 0;
            for (MultiReddit m : UserSubscriptions.multireddits) {
                multis[i] = m.getDisplayName();
                i++;
            }

            new MaterialDialog.Builder(ReorderSubreddits.this)
                    .title(R.string.reorder_subreddits_title)
                    .items(multis)
                    .itemsCallbackSingleChoice(
                            -1,
                            new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(
                                        MaterialDialog dialog,
                                        View itemView,
                                        int which,
                                        CharSequence text) {
                                    String name = multis[which];
                                    MultiReddit r =
                                            UserSubscriptions.getMultiredditByDisplayName(name);

                                    // Construct the new URL format for multireddits
                                    String username = Authentication.name;
                                    String multiName = r.getDisplayName();
                                    String sortMode =
                                            "hot"; // default sort mode, can be parameterized if
                                    // needed

                                    String url =
                                            String.format(
                                                    "api/user/%s/m/%s/%s?limit=25",
                                                    username, multiName, sortMode);

                                    int pos =
                                            addSubAlphabetically(MULTI_REDDIT + r.getDisplayName());
                                    UserSubscriptions.setSubNameToProperties(
                                            MULTI_REDDIT + r.getDisplayName(), url);
                                    adapter.notifyDataSetChanged();
                                    recyclerView.smoothScrollToPosition(pos);
                                    return false;
                                }
                            })
                    .show();
        }
    }

    public void doAddSub(String subreddit) {
        subreddit = subreddit.toLowerCase(Locale.ENGLISH);
        List<String> sortedSubs = UserSubscriptions.sortNoExtras(subs);

        if (sortedSubs.equals(subs)) {
            subs.add(subreddit);
            subs = UserSubscriptions.sortNoExtras(subs);
            adapter = new CustomAdapter(subs);
            recyclerView.setAdapter(adapter);
        } else {
            int pos = addSubAlphabetically(subreddit);
            adapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(pos);
        }
    }

    private int addSubAlphabetically(String finalS) {
        int i = subs.size() - 1;
        while (i >= 0 && finalS.compareTo(subs.get(i)) < 0) {
            i--;
        }
        i += 1;
        subs.add(i, finalS);
        return i;
    }

    private class AsyncGetSubreddit extends AsyncTask<String, Void, Subreddit> {
        @Override
        public void onPostExecute(Subreddit subreddit) {
            if (subreddit != null) {
                doAddSub(subreddit.getDisplayName());
            } else if (isSpecial(sub)) {
                doAddSub(sub);
            }
        }

        ArrayList<Subreddit> otherSubs;
        String sub;

        @Override
        protected Subreddit doInBackground(final String... params) {
            sub = params[0];
            if (isSpecial(sub)) return null;
            try {
                return (subs.contains(params[0])
                        ? null
                        : Authentication.reddit.getSubreddit(params[0]));
            } catch (Exception e) {
                otherSubs = new ArrayList<>();
                SubredditSearchPaginator p =
                        new SubredditSearchPaginator(Authentication.reddit, sub);
                while (p.hasNext()) {
                    otherSubs.addAll((p.next()));
                }
                if (otherSubs.isEmpty()) {
                    runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        new AlertDialog.Builder(ReorderSubreddits.this)
                                                .setTitle(R.string.subreddit_err)
                                                .setMessage(
                                                        getString(
                                                                R.string.subreddit_err_msg,
                                                                params[0]))
                                                .setPositiveButton(
                                                        R.string.btn_ok,
                                                        (dialog, which) -> dialog.dismiss())
                                                .setOnDismissListener(null)
                                                .show();
                                    } catch (Exception ignored) {
                                    }
                                }
                            });
                } else {
                    runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        final ArrayList<String> subs = new ArrayList<>();
                                        for (Subreddit s : otherSubs) {
                                            subs.add(s.getDisplayName());
                                        }
                                        new AlertDialog.Builder(ReorderSubreddits.this)
                                                .setTitle(R.string.reorder_not_found_err)
                                                .setItems(
                                                        subs.toArray(new String[0]),
                                                        (dialog, which) ->
                                                                doAddSub(subs.get(which)))
                                                .setPositiveButton(R.string.btn_cancel, null)
                                                .show();
                                    } catch (Exception ignored) {
                                    }
                                }
                            });
                }
            }
            return null;
        }
    }

    public void doOldToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setVisibility(View.VISIBLE);
    }

    public class CustomAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final ArrayList<String> items;

        public CustomAdapter(ArrayList<String> items) {
            this.items = items;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == 2) {
                View v =
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.spacer, parent, false);
                return new SpacerViewHolder(v);
            }
            View v =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.subforsublistdrag, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == items.size()) {
                return 2;
            }
            return 1;
        }

        public void doNewToolbar() {
            mToolbar.setVisibility(View.GONE);
            mToolbar = (Toolbar) findViewById(R.id.toolbar2);
            mToolbar.setTitle(
                    getResources()
                            .getQuantityString(
                                    R.plurals.reorder_selected, chosen.size(), chosen.size()));
            mToolbar.findViewById(R.id.delete)
                    .setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    final AlertDialog.Builder b =
                                            new AlertDialog.Builder(ReorderSubreddits.this)
                                                    .setTitle(R.string.reorder_remove_title)
                                                    .setPositiveButton(
                                                            R.string.btn_remove,
                                                            (dialog, which) -> {
                                                                for (String s : chosen) {
                                                                    int index = subs.indexOf(s);
                                                                    subs.remove(index);
                                                                    adapter.notifyItemRemoved(
                                                                            index);
                                                                }
                                                                isMultiple = false;
                                                                chosen = new ArrayList<>();
                                                                doOldToolbar();
                                                            })
                                                    .setNegativeButton(R.string.btn_cancel, null);
                                    if (Authentication.isLoggedIn
                                            && Authentication.didOnline
                                            && isSingle(chosen)) {
                                        b.setNeutralButton(
                                                R.string.reorder_remove_unsubscribe,
                                                (dialog, which) -> {
                                                    for (String s : chosen) {
                                                        int index = subs.indexOf(s);
                                                        subs.remove(index);
                                                        adapter.notifyItemRemoved(index);
                                                    }
                                                    new UserSubscriptions.UnsubscribeTask()
                                                            .execute(chosen.toArray(new String[0]));
                                                    for (String s : chosen) {
                                                        isSubscribed.put(
                                                                s.toLowerCase(Locale.ENGLISH),
                                                                false);
                                                    }
                                                    isMultiple = false;
                                                    chosen = new ArrayList<>();
                                                    doOldToolbar();
                                                });
                                    }
                                    b.show();
                                }
                            });
            mToolbar.findViewById(R.id.top)
                    .setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    for (String s : chosen) {
                                        subs.remove(s);
                                        subs.add(0, s);
                                    }
                                    isMultiple = false;
                                    doOldToolbar();
                                    chosen = new ArrayList<>();
                                    notifyDataSetChanged();
                                    recyclerView.smoothScrollToPosition(0);
                                }
                            });
            mToolbar.findViewById(R.id.pin)
                    .setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    List<String> pinned = UserSubscriptions.getPinned();
                                    boolean contained = pinned.containsAll(chosen);
                                    for (String s : chosen) {
                                        if (contained) {
                                            UserSubscriptions.removePinned(
                                                    s, ReorderSubreddits.this);
                                        } else {
                                            UserSubscriptions.addPinned(s, ReorderSubreddits.this);
                                            subs.remove(s);
                                            subs.add(0, s);
                                        }
                                    }
                                    isMultiple = false;
                                    doOldToolbar();
                                    chosen = new ArrayList<>();
                                    notifyDataSetChanged();
                                    recyclerView.smoothScrollToPosition(0);
                                }
                            });
        }

        int[] textColorAttr = new int[] {R.attr.fontColor};
        TypedArray ta = obtainStyledAttributes(textColorAttr);
        int textColor = ta.getColor(0, Color.BLACK);

        public void updateToolbar() {
            mToolbar.setTitle(
                    getResources()
                            .getQuantityString(
                                    R.plurals.reorder_selected, chosen.size(), chosen.size()));
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holderB, final int position) {
            if (holderB instanceof ViewHolder) {
                final ViewHolder holder = (ViewHolder) holderB;
                final String origPos = items.get(position);
                holder.text.setText(origPos);

                if (chosen.contains(origPos)) {
                    holder.itemView.setBackgroundColor(
                            Palette.getDarkerColor(holder.text.getCurrentTextColor()));
                    holder.text.setTextColor(Color.WHITE);
                } else {
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                    holder.text.setTextColor(textColor);
                }
                if (!isSingle(origPos) || !Authentication.isLoggedIn) {
                    holder.check.setVisibility(View.GONE);
                } else {
                    holder.check.setVisibility(View.VISIBLE);
                }
                holder.check.setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(
                                    CompoundButton buttonView, boolean isChecked) {
                                // do nothing
                            }
                        });
                holder.check.setChecked(
                        isSubscribed.containsKey(origPos.toLowerCase(Locale.ENGLISH))
                                && isSubscribed.get(origPos.toLowerCase(Locale.ENGLISH)));
                holder.check.setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(
                                    CompoundButton buttonView, boolean isChecked) {
                                if (!isChecked) {
                                    new UserSubscriptions.UnsubscribeTask().execute(origPos);
                                    Snackbar.make(
                                                    mToolbar,
                                                    getString(
                                                            R.string.reorder_unsubscribed_toast,
                                                            origPos),
                                                    Snackbar.LENGTH_SHORT)
                                            .show();
                                } else {
                                    new UserSubscriptions.SubscribeTask(ReorderSubreddits.this)
                                            .execute(origPos);
                                    Snackbar.make(
                                                    mToolbar,
                                                    getString(
                                                            R.string.reorder_subscribed_toast,
                                                            origPos),
                                                    Snackbar.LENGTH_SHORT)
                                            .show();
                                }
                                isSubscribed.put(origPos.toLowerCase(Locale.ENGLISH), isChecked);
                            }
                        });
                final View colorView = holder.itemView.findViewById(R.id.color);
                colorView.setBackgroundResource(R.drawable.circle);
                BlendModeUtil.tintDrawableAsModulate(
                        colorView.getBackground(), Palette.getColor(origPos));
                if (UserSubscriptions.getPinned().contains(origPos)) {
                    holder.itemView.findViewById(R.id.pinned).setVisibility(View.VISIBLE);
                } else {
                    holder.itemView.findViewById(R.id.pinned).setVisibility(View.GONE);
                }
                holder.itemView.setOnLongClickListener(
                        new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                if (!isMultiple) {
                                    isMultiple = true;
                                    chosen = new ArrayList<>();
                                    chosen.add(origPos);

                                    doNewToolbar();
                                    holder.itemView.setBackgroundColor(
                                            Palette.getDarkerColor(Palette.getDefaultAccent()));
                                    holder.text.setTextColor(Color.WHITE);
                                } else if (chosen.contains(origPos)) {
                                    holder.itemView.setBackgroundColor(Color.TRANSPARENT);

                                    // set the color of the text back to what it should be
                                    holder.text.setTextColor(textColor);

                                    chosen.remove(origPos);

                                    if (chosen.isEmpty()) {
                                        isMultiple = false;
                                        doOldToolbar();
                                    }
                                } else {
                                    chosen.add(origPos);
                                    holder.itemView.setBackgroundColor(
                                            Palette.getDarkerColor(Palette.getDefaultAccent()));
                                    holder.text.setTextColor(textColor);
                                    updateToolbar();
                                }
                                return true;
                            }
                        });

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isMultiple) {
                            // Get the current position safely before showing dialog
                            final int currentPos = holder.getBindingAdapterPosition();
                            if (currentPos == RecyclerView.NO_POSITION || currentPos >= items.size()) {
                                return;
                            }

                            new AlertDialog.Builder(ReorderSubreddits.this)
                                    .setItems(
                                            new CharSequence[] {
                                                getString(R.string.reorder_move),
                                                UserSubscriptions.getPinned().contains(origPos)
                                                        ? "Unpin"
                                                        : "Pin",
                                                getString(R.string.btn_delete)
                                            },
                                            (dialog, which) -> {
                                                if (which == 2) {
                                                    final AlertDialog.Builder b =
                                                            new AlertDialog.Builder(
                                                                            ReorderSubreddits
                                                                                    .this)
                                                                            .setTitle(
                                                                                    R.string
                                                                                            .reorder_remove_title)
                                                                            .setPositiveButton(
                                                                                    R.string
                                                                                            .btn_remove,
                                                                                    (dialog1,
                                                                                            which1) -> {
                                                                                        subs.remove(
                                                                                                items
                                                                                                        .get(
                                                                                                                currentPos));
                                                                                        adapter
                                                                                                .notifyItemRemoved(
                                                                                                        currentPos);
                                                                                    })
                                                                            .setNegativeButton(
                                                                                    R.string
                                                                                            .btn_cancel,
                                                                                    null);
                                                    if (Authentication.isLoggedIn
                                                            && Authentication.didOnline
                                                            && isSingle(origPos)) {
                                                        b.setNeutralButton(
                                                                R.string
                                                                        .reorder_remove_unsubscribe,
                                                                (dialog12, which12) -> {
                                                                    final String sub =
                                                                            items.get(
                                                                                    currentPos);
                                                                    subs.remove(sub);
                                                                    adapter
                                                                            .notifyItemRemoved(
                                                                                    currentPos);
                                                                    new UserSubscriptions
                                                                                    .UnsubscribeTask()
                                                                            .execute(sub);
                                                                    isSubscribed.put(
                                                                            sub.toLowerCase(
                                                                                    Locale
                                                                                            .ENGLISH),
                                                                            false);
                                                                });
                                                    }
                                                    b.show();
                                                } else if (which == 0) {
                                                    String s = items.get(currentPos);
                                                    subs.remove(s);
                                                    subs.add(0, s);
                                                    notifyItemMoved(currentPos, 0);
                                                    recyclerView.smoothScrollToPosition(0);
                                                } else if (which == 1) {
                                                    // Pin/unpin
                                                    String s = items.get(currentPos);
                                                    if (!UserSubscriptions.getPinned().contains(s)) {
                                                        UserSubscriptions.addPinned(s, ReorderSubreddits.this);
                                                        subs.remove(s);
                                                        subs.add(0, s);
                                                        notifyItemMoved(currentPos, 0);
                                                        recyclerView.smoothScrollToPosition(0);
                                                    } else {
                                                        UserSubscriptions.removePinned(s, ReorderSubreddits.this);
                                                        notifyItemChanged(currentPos);
                                                    }
                                                }
                                            })
                                    .show();
                        } else {
                            if (chosen.contains(origPos)) {
                                holder.itemView.setBackgroundColor(Color.TRANSPARENT);

                                // set the color of the text back to what it should be
                                int[] textColorAttr = new int[] {R.attr.fontColor};
                                TypedArray ta = obtainStyledAttributes(textColorAttr);
                                holder.text.setTextColor(ta.getColor(0, Color.BLACK));
                                ta.recycle();

                                chosen.remove(origPos);
                                updateToolbar();

                                if (chosen.isEmpty()) {
                                    isMultiple = false;
                                    doOldToolbar();
                                }
                            } else {
                                chosen.add(origPos);
                                holder.itemView.setBackgroundColor(
                                        Palette.getDarkerColor(Palette.getDefaultAccent()));
                                holder.text.setTextColor(Color.WHITE);
                                updateToolbar();
                            }
                        }
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size() + 1;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            final TextView text;
            final AppCompatCheckBox check;

            public ViewHolder(View itemView) {
                super(itemView);
                text = itemView.findViewById(R.id.name);
                check = itemView.findViewById(R.id.isSubscribed);
            }
        }

        public class SpacerViewHolder extends RecyclerView.ViewHolder {
            public SpacerViewHolder(View itemView) {
                super(itemView);
                itemView.findViewById(R.id.height)
                        .setLayoutParams(
                                new LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        DisplayUtil.dpToPxVertical(88)));
            }
        }
    }

    /**
     * Check if all of the subreddits are single
     *
     * @param subreddits list of subreddits to check
     * @return if all of the subreddits are single
     * @see #isSingle(java.lang.String)
     */
    private boolean isSingle(List<String> subreddits) {
        for (String subreddit : subreddits) {
            if (!isSingle(subreddit)) return false;
        }
        return true;
    }

    /**
     * If the subreddit isn't special, combined, or a multireddit - can attempt to be subscribed to
     *
     * @param subreddit name of a subreddit
     * @return if the subreddit is single
     */
    private boolean isSingle(String subreddit) {
        return !(isSpecial(subreddit)
                || subreddit.contains("+")
                || subreddit.contains(".")
                || subreddit.contains(MULTI_REDDIT));
    }

    /**
     * Subreddits with important behaviour - frontpage, all, random, etc.
     *
     * @param subreddit name of a subreddit
     * @return if the subreddit is special
     */
    private boolean isSpecial(String subreddit) {
        for (String specialSubreddit : UserSubscriptions.specialSubreddits) {
            if (subreddit.equalsIgnoreCase(specialSubreddit)) return true;
        }
        return false;
    }
}
