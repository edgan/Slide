package me.edgan.redditslide.Adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import me.edgan.redditslide.Activities.OpenContent;
import me.edgan.redditslide.Activities.SetupWidget;
import me.edgan.redditslide.Activities.Shortcut;
import me.edgan.redditslide.R;
import me.edgan.redditslide.Visuals.Palette;
import me.edgan.redditslide.Widget.SubredditWidgetProvider;
import me.edgan.redditslide.util.BlendModeUtil;
import me.edgan.redditslide.util.DrawableUtil;
import me.edgan.redditslide.util.ImageUtil;
import me.edgan.redditslide.util.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Created by ccrama on 8/17/2015. */
public class SubChooseAdapter extends ArrayAdapter<String> {
    private final List<String> objects;
    private Filter filter;
    public ArrayList<String> baseItems;
    public ArrayList<String> fitems;
    public boolean openInSubView = true;

    public SubChooseAdapter(
            Context context, ArrayList<String> objects, ArrayList<String> allSubreddits) {
        super(context, 0, objects);
        this.objects = new ArrayList<>(allSubreddits);
        filter = new SubFilter();
        fitems = new ArrayList<>(objects);
        baseItems = new ArrayList<>(objects);
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public Filter getFilter() {

        if (filter == null) {
            filter = new SubFilter();
        }
        return filter;
    }

    private static class ViewHolderItem {
        private TextView t;

        ViewHolderItem(TextView t) {
            this.t = t;
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolderItem viewHolderItem;
        if (convertView == null) {
            convertView =
                    LayoutInflater.from(getContext())
                            .inflate(R.layout.subforsublist, parent, false);
            viewHolderItem = new ViewHolderItem(convertView.findViewById(R.id.name));
            convertView.setTag(viewHolderItem);
        } else {
            viewHolderItem = (ViewHolderItem) convertView.getTag();
        }
        final TextView t = viewHolderItem.t;
        t.setText(fitems.get(position));

        final String subreddit = fitems.get(position);

        final View colorView = convertView.findViewById(R.id.color);
        colorView.setBackgroundResource(R.drawable.circle);
        BlendModeUtil.tintDrawableAsModulate(
                colorView.getBackground(), Palette.getColor(subreddit));

        if (getContext() instanceof SetupWidget) {
            convertView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((SetupWidget) getContext()).name = subreddit;
                            SubredditWidgetProvider.lastDone = subreddit;
                            ((SetupWidget) getContext()).startWidget();
                        }
                    });

        } else if (getContext() instanceof Shortcut) {
            convertView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final Bitmap src;
                            final Bitmap bm2;
                            Intent shortcutIntent = new Intent(getContext(), OpenContent.class);
                            if (subreddit.toLowerCase(Locale.ENGLISH).equals("androidcirclejerk")) {
                                bm2 =
                                        DrawableUtil.drawableToBitmapShortcut(
                                                ContextCompat.getDrawable(
                                                        getContext(), R.drawable.matiasduarte));
                                Log.v(LogUtil.getTag(), "NULL IS " + (bm2 == null));
                            } else {
                                src =
                                        DrawableUtil.drawableToBitmapShortcut(
                                                ContextCompat.getDrawable(
                                                        getContext(), R.drawable.blackandwhite));
                                final int overlayColor = Palette.getColor(subreddit);
                                final Paint paint = new Paint();
                                final Bitmap bm1 =
                                        Bitmap.createBitmap(
                                                src.getWidth(),
                                                src.getHeight(),
                                                Bitmap.Config.ARGB_8888);
                                Canvas c = new Canvas(bm1);
                                BlendModeUtil.tintPaintAsOverlay(paint, overlayColor);
                                c.drawBitmap(src, 0, 0, paint);

                                bm2 =
                                        Bitmap.createBitmap(
                                                src.getWidth(),
                                                src.getHeight(),
                                                Bitmap.Config.ARGB_8888);
                                c = new Canvas(bm2);
                                BlendModeUtil.tintPaintAsSrcAtop(paint, overlayColor);
                                c.drawBitmap(src, 0, 0, paint);

                                ImageUtil.drawWithTargetColor(bm2, bm1, overlayColor, 0);
                            }

                            final float scale =
                                    getContext().getResources().getDisplayMetrics().density;
                            int p = (int) (50 * scale + 0.5f);
                            shortcutIntent.putExtra(
                                    OpenContent.EXTRA_URL, "reddit.com/r/" + subreddit);
                            Intent intent = new Intent();
                            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "/r/" + subreddit);
                            intent.putExtra(
                                    Intent.EXTRA_SHORTCUT_ICON,
                                    Bitmap.createScaledBitmap(bm2, p, p, false));
                            ((Shortcut) getContext()).setResult(Activity.RESULT_OK, intent);

                            ((Shortcut) getContext()).finish();
                        }
                    });
        }
        return convertView;
    }

    @Override
    public int getCount() {
        return fitems.size();
    }

    private class SubFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            String prefix = constraint.toString().toLowerCase(Locale.ENGLISH);

            if (prefix == null || prefix.isEmpty()) {
                ArrayList<String> list = new ArrayList<>(baseItems);
                results.values = list;
                results.count = list.size();
            } else {
                openInSubView = true;
                final ArrayList<String> list = new ArrayList<>(objects);
                final ArrayList<String> nlist = new ArrayList<>();

                for (String sub : list) {
                    if (sub.contains(prefix)) nlist.add(sub);
                    if (sub.equals(prefix)) openInSubView = false;
                }
                if (openInSubView) {
                    nlist.add(prefix);
                }

                results.values = nlist;
                results.count = nlist.size();
            }
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            fitems = (ArrayList<String>) results.values;
            clear();
            if (fitems != null) {
                addAll(fitems);
                notifyDataSetChanged();
            }
        }
    }
}
