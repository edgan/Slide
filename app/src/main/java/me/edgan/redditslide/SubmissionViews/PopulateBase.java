package me.edgan.redditslide.SubmissionViews;

import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import me.edgan.redditslide.Activities.MediaView;
import me.edgan.redditslide.Authentication;
import me.edgan.redditslide.Fragments.SubmissionsView;
import me.edgan.redditslide.R;
import me.edgan.redditslide.util.LayoutUtils;

import net.dean.jraw.ApiException;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Submission;

/** Created by TacoTheDank on 04/04/2021. */
public class PopulateBase {
    public static void addAdaptorPosition(
            Intent myIntent, Submission submission, int adapterPosition) {
        if (submission.getComments() == null && adapterPosition != -1) {
            myIntent.putExtra(MediaView.ADAPTER_POSITION, adapterPosition);
            myIntent.putExtra(MediaView.SUBMISSION_URL, submission.getPermalink());
        }
        SubmissionsView.currentPosition(adapterPosition);
        SubmissionsView.currentSubmission(submission);
    }

    public static class AsyncReportTask extends AsyncTask<String, Void, Void> {
        private final Submission submission;
        private final View contextView;

        public AsyncReportTask(final Submission submission, final View contextView) {
            this.submission = submission;
            this.contextView = contextView;
        }

        @Override
        protected Void doInBackground(String... reason) {
            try {
                new AccountManager(Authentication.reddit).report(submission, reason[0]);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Snackbar s =
                    Snackbar.make(contextView, R.string.msg_report_sent, Snackbar.LENGTH_SHORT);
            LayoutUtils.showSnackbar(s);
        }
    }
}
