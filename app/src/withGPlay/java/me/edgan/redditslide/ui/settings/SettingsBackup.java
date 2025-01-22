package me.edgan.redditslide.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.jakewharton.processphoenix.ProcessPhoenix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import me.edgan.redditslide.Activities.BaseActivityAnim;
import me.edgan.redditslide.R;
import me.edgan.redditslide.util.FileUtil;
import me.edgan.redditslide.util.LayoutUtils;
import me.edgan.redditslide.util.LogUtil;

/**
 * Handles backing up and restoring app settings both locally and to Google Drive.
 */
public class SettingsBackup extends BaseActivityAnim {

    private static final String TAG = "SettingsBackup";

    // Request codes
    private static final int RC_SIGN_IN = 100;
    private static final int RC_AUTHORIZATION = 101;
    private static final int RC_OPEN_DOCUMENT = 102;

    // Google sign-in client
    private GoogleSignInClient mGoogleSignInClient;

    // Google Drive service
    private Drive mDriveService;

    // Progress dialog
    private MaterialDialog progress;

    // File created during "backup to directory"
    private File backedup;

    // For counting errors during tasks
    private int errors = 0;

    // Common single file name on Google Drive
    private static final String DRIVE_BACKUP_FILENAME = "shared_prefs_backup.txt";

    private HttpTransport HTTP_TRANSPORT;

    // Flags to track whether user wanted to do certain actions after sign-in
    private boolean backupRequestedAfterSignIn = false;
    private boolean restoreRequestedAfterSignIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HTTP_TRANSPORT = new NetHttpTransport();

        applyColorTheme();
        setContentView(R.layout.activity_settings_sync);
        setupAppBar(R.id.toolbar, R.string.settings_title_backup, true, true);

        // Initialize Google sign-in
        initializeGoogleSignIn();

        // Setup UI elements and listeners
        setupUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if already signed in
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && mDriveService == null) {
            Log.d(TAG, "Already signed in, initializing Drive service.");
            initializeDriveService(account);
        } else {
            Log.d(TAG, "Not signed in or service is already initialized.");
        }
    }

    /**
     * Initialize Google sign-in options and client.
     */
    private void initializeGoogleSignIn() {
        Log.d(TAG, "Initializing Google sign-in options");
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(
                        new Scope(DriveScopes.DRIVE_FILE),
                        new Scope(DriveScopes.DRIVE_APPDATA))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    /**
     * Initialize the Google Drive service with the signed-in account.
     *
     * @param account The GoogleSignInAccount obtained after sign-in.
     */
    private void initializeDriveService(GoogleSignInAccount account) {
        Log.d(TAG, "Initializing Drive service for account: " + account.getEmail());
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                this, Collections.singleton(DriveScopes.DRIVE_APPDATA));
        credential.setSelectedAccount(account.getAccount());

        try {
            mDriveService = new Drive.Builder(
                    HTTP_TRANSPORT,
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName(getString(R.string.app_name))
                    .build();
            Log.d(TAG, "Drive service initialized successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Drive service", e);
            showErrorDialog(R.string.err_general, R.string.err_general);
        }
    }

    /**
     * Setup UI elements and their click listeners.
     */
    private void setupUI() {
        findViewById(R.id.back).setOnClickListener(v -> handleBackup());
        findViewById(R.id.restore).setOnClickListener(v -> handleRestore());
        findViewById(R.id.backfile).setOnClickListener(v -> showBackupToDirDialog());
        findViewById(R.id.restorefile).setOnClickListener(v -> openRestoreFile());
    }

    /**
     * Handle the Backup-to-Google-Drive button click.
     */
    private void handleBackup() {
        Log.d(TAG, "handleBackup() called.");
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            Log.d(TAG, "User not signed in, requesting sign in before backup.");
            backupRequestedAfterSignIn = true;
            requestSignIn();
            return;
        }

        if (mDriveService == null) {
            initializeDriveService(account);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.general_confirm)
                .setMessage(R.string.backup_confirm)
                .setPositiveButton(R.string.btn_ok, (dialog, whichButton) -> new BackupToDriveAsyncTask().execute())
                .setNegativeButton(R.string.btn_no, null)
                .setCancelable(false)
                .show();
    }

    /**
     * Handle the Restore-from-Google-Drive button click.
     */
    private void handleRestore() {
        Log.d(TAG, "handleRestore() called.");
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            Log.d(TAG, "User not signed in, requesting sign in before restore.");
            restoreRequestedAfterSignIn = true;
            requestSignIn();
            return;
        }

        if (mDriveService == null) {
            initializeDriveService(account);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.general_confirm)
                .setMessage(R.string.backup_restore_confirm)
                .setPositiveButton(R.string.btn_ok, (dialog, whichButton) -> new RestoreFromDriveAsyncTask().execute())
                .setNegativeButton(R.string.btn_no, null)
                .setCancelable(false)
                .show();
    }

    /**
     * Show dialog to choose backup to directory options (local .txt).
     */
    private void showBackupToDirDialog() {
        Log.d(TAG, "showBackupToDirDialog() called.");
        new AlertDialog.Builder(this)
                .setTitle(R.string.include_personal_info)
                .setMessage(R.string.include_personal_info_msg)
                .setPositiveButton(R.string.btn_yes, (dialog, which) -> backupToDir(false))
                .setNegativeButton(R.string.btn_no, (dialog, which) -> backupToDir(true))
                .setNeutralButton(R.string.btn_cancel, null)
                .setCancelable(false)
                .show();
    }

    /**
     * Open a file picker to select a backup file for local restoration.
     */
    private void openRestoreFile() {
        Log.d(TAG, "openRestoreFile() called.");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Adjust type as needed
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_backup_file)), RC_OPEN_DOCUMENT);
    }

    /**
     * Request Google sign-in.
     */
    private void requestSignIn() {
        Log.d(TAG, "requestSignIn() - launching Google sign-in intent.");
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    /**
     * Handle the result from activities like sign-in and file picker.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        switch (requestCode) {
            case RC_SIGN_IN:
                handleSignInResult(data);
                break;
            case RC_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "RC_AUTHORIZATION: user granted authorization. Retrying if needed.");
                    // You can retry the last operation if needed
                } else {
                    Log.w(TAG, "RC_AUTHORIZATION: user denied or canceled authorization.");
                }
                break;
            case RC_OPEN_DOCUMENT:
                handleFilePickerResult(resultCode, data);
                break;
            default:
                break;
        }
    }

    /**
     * Handle the result of the Google sign-in intent.
     */
    private void handleSignInResult(Intent data) {
        Log.d(TAG, "handleSignInResult() called.");
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        task.addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
            @Override
            public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                try {
                    GoogleSignInAccount account = task.getResult(Exception.class);
                    if (account != null) {
                        Log.d(TAG, "Sign-in successful, email: " + account.getEmail());
                        initializeDriveService(account);

                        // If we had requested a backup or restore after sign-in, proceed now
                        if (backupRequestedAfterSignIn) {
                            backupRequestedAfterSignIn = false;
                            new BackupToDriveAsyncTask().execute();
                        } else if (restoreRequestedAfterSignIn) {
                            restoreRequestedAfterSignIn = false;
                            new RestoreFromDriveAsyncTask().execute();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Sign-in failed", e);
                    showErrorDialog(R.string.sign_in_failed, R.string.sign_in_failed_msg);
                }
            }
        });
    }

    /**
     * Handle the result from the local file picker intent.
     */
    private void handleFilePickerResult(int resultCode, Intent data) {
        Log.d(TAG, "handleFilePickerResult: resultCode=" + resultCode + ", data=" + data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            Log.d(TAG, "Selected file URI: " + fileUri);

            progress = new MaterialDialog.Builder(this)
                    .title(R.string.backup_restoring)
                    .content(R.string.misc_please_wait)
                    .cancelable(false)
                    .progress(true, 1)
                    .build();
            progress.show();

            // Handle file restoration asynchronously
            new RestoreFromFileAsyncTask(fileUri).execute();
        } else {
            if (progress != null) {
                progress.dismiss();
            }
            showErrorDialog(R.string.err_file_not_found, R.string.err_file_not_found_msg);
        }
    }

    /**
     * Show an error dialog with the specified title and message.
     */
    private void showErrorDialog(int titleResId, int messageResId) {
        Log.d(TAG, "showErrorDialog: title=" + titleResId + ", message=" + messageResId);
        new AlertDialog.Builder(this)
                .setTitle(titleResId)
                .setMessage(messageResId)
                .setPositiveButton(R.string.btn_ok, null)
                .setCancelable(false)
                .show();
    }

    /**
     * Asynchronous task to bundle all shared prefs into a single string
     * and upload/overwrite "shared_prefs_backup.txt" in Google Drive appDataFolder.
     */
    private class BackupToDriveAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "BackupToDriveAsyncTask: started");
            progress = new MaterialDialog.Builder(SettingsBackup.this)
                    .title(R.string.backup_backing_up)
                    .content(R.string.misc_please_wait)
                    .cancelable(false)
                    .progress(true, 0)
                    .build();
            progress.show();
            errors = 0;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            // Build the entire backup as a single string in memory
            File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
            if (!prefsDir.exists() || !prefsDir.isDirectory()) {
                Log.w(TAG, "No shared_prefs directory found for Drive backup.");
                return false;
            }

            String[] list = prefsDir.list();
            if (list == null) {
                Log.w(TAG, "No preference files found to backup for Drive.");
                return false;
            }

            StringBuilder backupBuilder = new StringBuilder();
            backupBuilder.append("Slide_backupEND>");  // starting marker

            for (String s : list) {
                if (!s.contains("cache") && !s.contains("ion-cookies") && !s.contains("albums")
                        && !s.contains("STACKTRACE") && !s.contains("com.google")) {

                    File fileToBackup = new File(prefsDir, s);
                    String content = readFileFully(fileToBackup);
                    if (content != null) {
                        backupBuilder.append("<START").append(s).append(">");
                        backupBuilder.append(content);
                        backupBuilder.append("END>");
                        Log.d(TAG, "Adding file to single backup: " + s);
                    } else {
                        Log.w(TAG, "Content was null for local file: " + s);
                    }
                } else {
                    Log.d(TAG, "Skipping local file: " + s);
                }
            }

            // Convert entire backup string to bytes for upload
            byte[] backupData = backupBuilder.toString().getBytes();

            // Check if there's an existing backup file named shared_prefs_backup.txt, if so we update; otherwise create
            try {
                String fileId = findDriveBackupFileId();
                if (fileId == null) {
                    // Create new file
                    com.google.api.services.drive.model.File fileMetadata =
                            new com.google.api.services.drive.model.File();
                    fileMetadata.setName(DRIVE_BACKUP_FILENAME);
                    fileMetadata.setParents(Collections.singletonList("appDataFolder"));

                    ByteArrayContent contentStream = new ByteArrayContent("text/plain", backupData);
                    mDriveService.files().create(fileMetadata, contentStream)
                            .setFields("id")
                            .execute();
                    Log.d(TAG, "Created new backup file on Drive: " + DRIVE_BACKUP_FILENAME);
                } else {
                    // Update existing file
                    ByteArrayContent contentStream = new ByteArrayContent("text/plain", backupData);
                    mDriveService.files().update(fileId, null, contentStream)
                            .execute();
                    Log.d(TAG, "Updated existing backup file on Drive: " + DRIVE_BACKUP_FILENAME);
                }
            } catch (UserRecoverableAuthIOException urae) {
                Log.w(TAG, "UserRecoverableAuthIOException while uploading single backup. Requesting auth again.");
                startActivityForResult(urae.getIntent(), RC_AUTHORIZATION);
                return false;
            } catch (IOException e) {
                Log.e(TAG, "Error uploading single-file backup to Drive", e);
                errors++;
                return false;
            }
            return (errors == 0);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (progress != null) {
                progress.dismiss();
            }
            if (success) {
                new AlertDialog.Builder(SettingsBackup.this)
                        .setTitle(R.string.backup_success)
                        .setPositiveButton(R.string.btn_close, (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            } else {
                showErrorDialog(R.string.err_general, R.string.backup_failed_msg);
            }
        }

        /**
         * Search for an existing "shared_prefs_backup.txt" in the appDataFolder. Return its fileId or null if not found.
         */
        private String findDriveBackupFileId() throws IOException {
            FileList result = mDriveService.files().list()
                    .setSpaces("appDataFolder")
                    .setFields("files(id, name)")
                    .execute();
            if (result.getFiles() != null && !result.getFiles().isEmpty()) {
                for (com.google.api.services.drive.model.File file : result.getFiles()) {
                    if (DRIVE_BACKUP_FILENAME.equals(file.getName())) {
                        return file.getId();
                    }
                }
            }
            return null;
        }
    }

    /**
     * Asynchronous task to download and restore the single "shared_prefs_backup.txt" from Drive.
     */
    private class RestoreFromDriveAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "RestoreFromDriveAsyncTask: started");
            progress = new MaterialDialog.Builder(SettingsBackup.this)
                    .title(R.string.backup_restoring)
                    .content(R.string.misc_please_wait)
                    .cancelable(false)
                    .progress(true, 0)
                    .build();
            progress.show();
            errors = 0;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // Search for our single backup file in Drive
                String fileId = null;
                FileList result = mDriveService.files().list()
                        .setSpaces("appDataFolder")
                        .setFields("files(id, name)")
                        .execute();
                if (result.getFiles() != null && !result.getFiles().isEmpty()) {
                    for (com.google.api.services.drive.model.File file : result.getFiles()) {
                        if (DRIVE_BACKUP_FILENAME.equals(file.getName())) {
                            fileId = file.getId();
                            break;
                        }
                    }
                }

                if (fileId == null) {
                    Log.w(TAG, "No single backup file named " + DRIVE_BACKUP_FILENAME + " found in Drive.");
                    return false;
                }

                // Download the file content
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                mDriveService.files().get(fileId)
                        .executeMediaAndDownloadTo(outputStream);
                String data = outputStream.toString();
                Log.d(TAG, "Downloaded " + data.length() + " bytes from: " + DRIVE_BACKUP_FILENAME);

                // Parse the single-file backup just like local restore
                return restoreSharedPrefsFromString(data);

            } catch (UserRecoverableAuthIOException urae) {
                Log.w(TAG, "UserRecoverableAuthIOException while downloading single backup. Requesting auth again.");
                startActivityForResult(urae.getIntent(), RC_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(TAG, "Error downloading single-file backup from Drive", e);
                errors++;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (progress != null) {
                progress.dismiss();
            }

            if (!success) {
                showErrorDialog(R.string.err_general, R.string.backup_restore_failed_msg);
                return;
            }

            // List the shared_prefs directory to confirm the new files are there
            listSharedPrefsDirectory("After Drive Restore");

            new AlertDialog.Builder(SettingsBackup.this)
                    .setTitle(R.string.backup_restore_settings)
                    .setMessage(R.string.backup_restarting)
                    .setOnDismissListener(dialog -> {
                        Log.d(TAG, "ProcessPhoenix.triggerRebirth() called from onDismiss (Drive restore).");
                        ProcessPhoenix.triggerRebirth(SettingsBackup.this);
                    })
                    .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                        Log.d(TAG, "ProcessPhoenix.triggerRebirth() called from OK button (Drive restore).");
                        ProcessPhoenix.triggerRebirth(SettingsBackup.this);
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    /**
     * Restore the shared_prefs contents from a single large string that uses the local-file markers.
     * @param data The entire backup string containing multiple files.
     * @return true if success, false otherwise.
     */
    private boolean restoreSharedPrefsFromString(String data) {
        try {
            if (!data.contains("Slide_backupEND>")) {
                Log.w(TAG, "Backup file did not contain 'Slide_backupEND>' marker, likely invalid.");
                return false;
            }
            // Example data is:
            // Slide_backupEND><STARTsomefile.xml>filecontentEND><STARTotherfile.xml>filecontentEND> ...
            String[] files = data.split("END>");
            // files[0] will contain "Slide_backupEND>", skip it
            for (int i = 1; i < files.length; i++) {
                String innerFile = files[i];
                // Example: <STARTfilename.xml>content
                int startIndex = innerFile.indexOf("<START");
                if (startIndex == -1) {
                    Log.w(TAG, "Skipping malformed file block: " + innerFile);
                    continue;
                }
                // substring from 6 chars after <START to the next '>'
                String name = innerFile.substring(startIndex + 6, innerFile.indexOf(">", startIndex));
                String fileContent = innerFile.substring(innerFile.indexOf(">", startIndex) + 1);

                File newF = new File(getApplicationInfo().dataDir + "/shared_prefs/" + name);
                Log.d(TAG, "Restoring local file: " + name + " (size=" + fileContent.length() + ")");
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(newF))) {
                    bw.write(fileContent);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while parsing single-file backup data", e);
            return false;
        }
        return true;
    }

    /**
     * Asynchronous task to perform backup operations to a local directory (e.g. /Downloads).
     * @param personal Whether to exclude certain personal data.
     */
    private void backupToDir(final boolean personal) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                Log.d(TAG, "backupToDir() started, personal=" + personal);
                progress = new MaterialDialog.Builder(SettingsBackup.this)
                        .cancelable(false)
                        .title(R.string.backup_backing_up)
                        .progress(false, 40)
                        .build();
                progress.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");

                if (prefsDir.exists() && prefsDir.isDirectory()) {
                    String[] list = prefsDir.list();
                    if (list == null) {
                        Log.w(TAG, "No preference files found to backup locally.");
                        return null;
                    }

                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    downloadsDir.mkdirs();

                    String timeStamp = new SimpleDateFormat("-yyyy-MM-dd-HH-mm-ss").format(new Date());
                    String personalSuffix = !personal ? "-personal" : "";
                    backedup = new File(downloadsDir, "Slide" + timeStamp + personalSuffix + ".txt");

                    try {
                        backedup.createNewFile();
                        try (BufferedWriter bw = new BufferedWriter(new FileWriter(backedup))) {
                            bw.write("Slide_backupEND>");
                            for (String s : list) {
                                // Skip some known irrelevant/personal files
                                if (!s.contains("cache") && !s.contains("ion-cookies") && !s.contains("albums")
                                        && !s.contains("STACKTRACE") && !s.contains("com.google")
                                        && (!personal || (!s.contains("SUBSNEW") && !s.contains("appRestart")
                                                && !s.contains("AUTH") && !s.contains("TAGS")
                                                && !s.contains("SEEN") && !s.contains("HIDDEN")
                                                && !s.contains("HIDDEN_POSTS")))) {

                                    File fileToBackup = new File(prefsDir, s);
                                    String content = readFileFully(fileToBackup);
                                    if (content != null) {
                                        Log.d(TAG, "Backing up local file: " + s + " (size=" + content.length() + ")");
                                        bw.write("<START" + fileToBackup.getName() + ">");
                                        bw.write(content);
                                        bw.write("END>");
                                    } else {
                                        Log.w(TAG, "Content was null for local file: " + s);
                                    }
                                } else {
                                    Log.d(TAG, "Skipping local file: " + s);
                                }
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error creating or writing backup file", e);
                    }
                } else {
                    Log.w(TAG, "No shared_prefs directory found for local backup.");
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (progress != null) {
                    progress.dismiss();
                }
                new AlertDialog.Builder(SettingsBackup.this)
                        .setTitle(R.string.backup_complete)
                        .setMessage(R.string.backup_saved_downloads)
                        .setPositiveButton(R.string.btn_view, (dialog, which) -> {
                            if (backedup != null && backedup.exists()) {
                                Intent intent = FileUtil.getFileIntent(backedup,
                                        new Intent(Intent.ACTION_VIEW),
                                        SettingsBackup.this);
                                if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
                                    startActivity(Intent.createChooser(intent, getString(R.string.settings_backup_view)));
                                } else {
                                    Snackbar s = Snackbar.make(findViewById(R.id.restorefile),
                                            getString(R.string.settings_backup_err_no_explorer) + backedup.getAbsolutePath(),
                                            Snackbar.LENGTH_INDEFINITE);
                                    LayoutUtils.showSnackbar(s);
                                }
                            } else {
                                showErrorDialog(R.string.err_file_not_found, R.string.err_file_not_found_msg);
                            }
                        })
                        .setNegativeButton(R.string.btn_close, null)
                        .setCancelable(false)
                        .show();
            }
        }.execute();
    }

    /**
     * Asynchronous task to restore settings from a selected local file (via file picker).
     */
    private class RestoreFromFileAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private final Uri fileUri;

        RestoreFromFileAsyncTask(Uri fileUri) {
            this.fileUri = fileUri;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Log.d(TAG, "RestoreFromFileAsyncTask started for URI: " + fileUri);
            StringBuilder fw = new StringBuilder();
            try (InputStream is = getContentResolver().openInputStream(fileUri);
                 BufferedReader reader = (is == null) ? null : new BufferedReader(new InputStreamReader(is))) {

                if (reader == null) {
                    Log.e(TAG, "Could not open InputStream for fileUri: " + fileUri);
                    return false;
                }

                int c;
                while ((c = reader.read()) != -1) {
                    fw.append((char) c);
                }
                String read = fw.toString();
                Log.d(TAG, "Read " + read.length() + " characters from backup file.");

                // Reuse same parse logic as in restoreSharedPrefsFromString
                return restoreSharedPrefsFromString(read);

            } catch (Exception e) {
                Log.e(TAG, "Exception while restoring from fileUri: " + fileUri, e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (progress != null) {
                progress.dismiss();
            }
            if (success) {
                // List the shared_prefs directory to confirm the new files are there
                listSharedPrefsDirectory("After Local File Restore");

                new AlertDialog.Builder(SettingsBackup.this)
                        .setTitle(R.string.backup_restore_settings)
                        .setMessage(R.string.backup_restarting)
                        .setOnDismissListener(dialog -> {
                            Log.d(TAG, "ProcessPhoenix.triggerRebirth() from onDismiss for local file restore.");
                            ProcessPhoenix.triggerRebirth(SettingsBackup.this);
                        })
                        .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                            Log.d(TAG, "ProcessPhoenix.triggerRebirth() from OK button for local file restore.");
                            ProcessPhoenix.triggerRebirth(SettingsBackup.this);
                        })
                        .setCancelable(false)
                        .show();
            } else {
                Log.w(TAG, "Restore from local file failed or invalid file.");
                showErrorDialog(R.string.err_not_valid_backup, R.string.err_not_valid_backup_msg);
            }
        }
    }

    /**
     * Read the entire content of a file into a String.
     * @param file The file to read.
     * @return The file content as a String, or null if an error occurs.
     */
    private String readFileFully(File file) {
        if (!file.exists()) {
            Log.w(TAG, "readFileFully: file does not exist - " + file.getAbsolutePath());
            return null;
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            Log.d(TAG, "readFileFully: " + file.getName() + " (size=" + builder.length() + ")");
            return builder.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + file.getName(), e);
            return null;
        }
    }

    /**
     * Write a String content to a file.
     * @param file    The file to write to.
     * @param content The content to write.
     */
    private void writeStringToFile(File file, String content) {
        Log.d(TAG, "writeStringToFile: " + file.getName() + " (size=" + content.length() + ")");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(content);
        } catch (IOException e) {
            Log.e(TAG, "Error writing to file: " + file.getName(), e);
            errors++;
        }
    }

    /**
     * Helper method to list the shared_prefs directory contents for debugging.
     * @param contextMessage Extra message to clarify when/why we're listing.
     */
    private void listSharedPrefsDirectory(String contextMessage) {
        File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
        if (!prefsDir.exists() || !prefsDir.isDirectory()) {
            Log.w(TAG, contextMessage + ": shared_prefs directory not found.");
            return;
        }
        String[] localPrefs = prefsDir.list();
        if (localPrefs == null || localPrefs.length == 0) {
            Log.w(TAG, contextMessage + ": No files in shared_prefs directory.");
            return;
        }
        Log.d(TAG, contextMessage + ": listing shared_prefs files...");
        for (String file : localPrefs) {
            File f = new File(prefsDir, file);
            Log.d(TAG, " - " + file + " (size=" + f.length() + ")");
        }
    }
}
