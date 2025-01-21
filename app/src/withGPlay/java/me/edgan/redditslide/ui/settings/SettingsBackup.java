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
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import me.edgan.redditslide.Activities.BaseActivityAnim;
import me.edgan.redditslide.R;
import me.edgan.redditslide.util.FileUtil;
import me.edgan.redditslide.util.LayoutUtils;
import me.edgan.redditslide.util.LogUtil;

public class SettingsBackup extends BaseActivityAnim {

    // Request codes
    private static final int RC_SIGN_IN = 100;
    private static final int RC_AUTHORIZATION = 101;
    private static final int RC_OPEN_DOCUMENT = 102;

    // Google Sign-In client
    private GoogleSignInClient mGoogleSignInClient;

    // Google Drive service
    private Drive mDriveService;

    // Progress dialog
    private MaterialDialog progress;

    // Other fields
    private int errors;

    // Declare 'backedup' as a class member
    private File backedup;

    private HttpTransport HTTP_TRANSPORT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HTTP_TRANSPORT = new NetHttpTransport();
        applyColorTheme();
        setContentView(R.layout.activity_settings_sync);
        setupAppBar(R.id.toolbar, R.string.settings_title_backup, true, true);

        // Initialize Google Sign-In
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
            initializeDriveService(account);
        }
    }

    /**
     * Initialize Google Sign-In options and client.
     */
    private void initializeGoogleSignIn() {
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
        } catch (Exception e) {
            Log.e(LogUtil.getTag(), "Error initializing Drive service", e);
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
     * Handle the Backup button click.
     */
    private void handleBackup() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            requestSignIn();
            return;
        }

        if (mDriveService == null) {
            initializeDriveService(account);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.general_confirm)
                .setMessage(R.string.backup_confirm)
                .setPositiveButton(R.string.btn_ok, (dialog, whichButton) -> performBackup())
                .setNegativeButton(R.string.btn_no, null)
                .setCancelable(false)
                .show();
    }

    /**
     * Perform the backup operation by uploading shared preferences to Google Drive.
     */
    private void performBackup() {
        // Use java.io.File by specifying the full path
        File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");

        if (prefsDir.exists() && prefsDir.isDirectory()) {
            String[] list = prefsDir.list();
            if (list == null || list.length == 0) {
                showErrorDialog(R.string.backup_no_prefs_found, R.string.backup_no_prefs_found_msg);
                return;
            }

            // Show progress dialog
            progress = new MaterialDialog.Builder(this)
                    .title(R.string.backup_backing_up)
                    .progress(false, list.length)
                    .cancelable(false)
                    .build();
            progress.show();

            new BackupAsyncTask().execute(list);
        } else {
            showErrorDialog(R.string.backup_no_prefs_dir, R.string.backup_no_prefs_dir_msg);
        }
    }

    /**
     * Handle the Restore button click.
     */
    private void handleRestore() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            requestSignIn();
            return;
        }

        if (mDriveService == null) {
            initializeDriveService(account);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.general_confirm)
                .setMessage(R.string.backup_restore_confirm)
                .setPositiveButton(R.string.btn_ok, (dialog, whichButton) -> performRestore())
                .setNegativeButton(R.string.btn_no, null)
                .setCancelable(false)
                .show();
    }

    /**
     * Perform the restore operation by downloading shared preferences from Google Drive.
     */
    private void performRestore() {
        progress = new MaterialDialog.Builder(this)
                .title(R.string.backup_restoring)
                .content(R.string.misc_please_wait)
                .cancelable(false)
                .progress(true, 0)
                .build();
        progress.show();

        new RestoreAsyncTask().execute();
    }

    /**
     * Show dialog to choose backup to directory options.
     */
    private void showBackupToDirDialog() {
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
     * Open a file picker to select a backup file for restoration.
     */
    private void openRestoreFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Adjust type as needed
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_backup_file)), RC_OPEN_DOCUMENT);
    }

    /**
     * Request Google Sign-In.
     */
    private void requestSignIn() {
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    /**
     * Handle the result from activities like sign-in and file picker.
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult().
     * @param resultCode  The integer result code returned by the child activity.
     * @param data        An Intent, which can return result data to the caller.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RC_SIGN_IN:
                handleSignInResult(data);
                break;
            case RC_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    // Retry the failed operation if necessary
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
     * Handle the result of the Google Sign-In intent.
     *
     * @param data The returned Intent from sign-in.
     */
    private void handleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        task.addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
            @Override
            public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                try {
                    GoogleSignInAccount account = task.getResult(Exception.class);
                    if (account != null) {
                        initializeDriveService(account);
                        // You can perform further actions here if needed
                    }
                } catch (Exception e) {
                    Log.e(LogUtil.getTag(), "Sign-in failed", e);
                    showErrorDialog(R.string.sign_in_failed, R.string.sign_in_failed_msg);
                }
            }
        });
    }

    /**
     * Handle the result from the file picker intent.
     *
     * @param resultCode The integer result code returned by the child activity.
     * @param data       An Intent, which can return result data to the caller.
     */
    private void handleFilePickerResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            Log.v(LogUtil.getTag(), "Selected file: " + fileUri.toString());

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
            progress.dismiss();
            showErrorDialog(R.string.err_file_not_found, R.string.err_file_not_found_msg);
        }
    }

    /**
     * Show an error dialog with the specified title and message.
     *
     * @param titleResId   The resource ID of the title string.
     * @param messageResId The resource ID of the message string.
     */
    private void showErrorDialog(int titleResId, int messageResId) {
        new AlertDialog.Builder(this)
                .setTitle(titleResId)
                .setMessage(messageResId)
                .setPositiveButton(R.string.btn_ok, null)
                .setCancelable(false)
                .show();
    }

    /**
     * Asynchronous task to perform backup operations.
     */
    private class BackupAsyncTask extends AsyncTask<String[], Void, Void> {
        @Override
        protected Void doInBackground(String[]... params) {
            String[] prefsList = params[0];
            for (String prefFilename : prefsList) {
                if (!prefFilename.contains("com.google") && !prefFilename.contains("cache") && !prefFilename.contains("STACKTRACE")) {
                    String content = readFileFully(new File(getApplicationInfo().dataDir + "/shared_prefs/" + prefFilename));
                    if (content != null) {
                        uploadFile(prefFilename, content);
                    }
                }
                publishProgress(); // Increment progress
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            if (progress != null) {
                progress.incrementProgress(1);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (progress != null) {
                progress.dismiss();
            }
            new AlertDialog.Builder(SettingsBackup.this)
                    .setTitle(R.string.backup_success)
                    .setPositiveButton(R.string.btn_close, (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        }

        /**
         * Upload a single file to Google Drive App Folder.
         *
         * @param fileName    The name of the file to upload.
         * @param fileContent The content of the file.
         */
        private void uploadFile(String fileName, String fileContent) {
            try {
                // Use Drive API's File by fully qualifying its class name
                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                fileMetadata.setName(fileName);
                fileMetadata.setParents(Collections.singletonList("appDataFolder"));

                ByteArrayContent contentStream = new ByteArrayContent("text/xml", fileContent.getBytes());

                mDriveService.files().create(fileMetadata, contentStream)
                        .setFields("id")
                        .execute();
            } catch (com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), RC_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(LogUtil.getTag(), "Error uploading file: " + fileName, e);
                errors++;
            }
        }
    }

    /**
     * Asynchronous task to perform restore operations from Google Drive.
     */
    private class RestoreAsyncTask extends AsyncTask<Void, Integer, List<com.google.api.services.drive.model.File>> {
        @Override
        protected List<com.google.api.services.drive.model.File> doInBackground(Void... voids) {
            List<com.google.api.services.drive.model.File> filesToRestore = new ArrayList<>();
            try {
                FileList result = mDriveService.files().list()
                        .setSpaces("appDataFolder")
                        .setFields("files(id, name)")
                        .execute();
                if (result.getFiles() != null) {
                    filesToRestore.addAll(result.getFiles());
                }
            } catch (com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), RC_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(LogUtil.getTag(), "Error listing files", e);
                errors++;
            }
            return filesToRestore;
        }

        @Override
        protected void onPostExecute(List<com.google.api.services.drive.model.File> files) {
            if (progress != null) {
                progress.dismiss();
            }

            if (files.isEmpty()) {
                showErrorDialog(R.string.backup_no_files, R.string.backup_no_files_msg);
                return;
            }

            progress = new MaterialDialog.Builder(SettingsBackup.this)
                    .title(R.string.backup_restoring)
                    .progress(false, files.size())
                    .cancelable(false)
                    .build();
            progress.show();

            new RestoreFilesAsyncTask(files).execute();
        }
    }

    /**
     * Asynchronous task to download and restore each file from Google Drive.
     */
    private class RestoreFilesAsyncTask extends AsyncTask<Void, Integer, Void> {
        private List<com.google.api.services.drive.model.File> files;

        RestoreFilesAsyncTask(List<com.google.api.services.drive.model.File> files) {
            this.files = files;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (com.google.api.services.drive.model.File driveFile : files) {
                String content = downloadFileContent(driveFile.getId());
                if (content != null) {
                    writeStringToFile(new File(getApplicationInfo().dataDir + "/shared_prefs/" + driveFile.getName()), content);
                }
                publishProgress(1);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (progress != null) {
                progress.incrementProgress(1);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (progress != null) {
                progress.dismiss();
            }

            new AlertDialog.Builder(SettingsBackup.this)
                    .setTitle(R.string.backup_restore_settings)
                    .setMessage(R.string.backup_restarting)
                    .setOnDismissListener(dialog -> {
                        // Implement app restart logic here, e.g., using ProcessPhoenix or another method
                        // ProcessPhoenix.triggerRebirth(SettingsBackup.this);
                    })
                    .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                        // Implement app restart logic here
                        // ProcessPhoenix.triggerRebirth(SettingsBackup.this);
                    })
                    .setCancelable(false)
                    .show();
        }

        /**
         * Download the content of a file from Google Drive.
         *
         * @param fileId The ID of the file to download.
         * @return The content of the file as a String, or null if failed.
         */
        private String downloadFileContent(String fileId) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                mDriveService.files().get(fileId)
                        .executeMediaAndDownloadTo(outputStream);
                return outputStream.toString();
            } catch (com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), RC_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(LogUtil.getTag(), "Error downloading file: " + fileId, e);
                errors++;
            }
            return null;
        }
    }

    /**
     * Asynchronous task to perform backup operations to a directory.
     *
     * @param personal Whether to include personal data.
     */
    private void backupToDir(final boolean personal) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
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
                    if (list == null) return null;

                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    downloadsDir.mkdirs();
                    backedup = new File(downloadsDir,
                            "Slide" +
                                    new java.text.SimpleDateFormat("-yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().getTime()) +
                                    (!personal ? "-personal" : "") +
                                    ".txt");

                    try {
                        backedup.createNewFile();
                        BufferedWriter bw = new BufferedWriter(new FileWriter(backedup));
                        bw.write("Slide_backupEND>");
                        for (String s : list) {
                            if (!s.contains("cache") && !s.contains("ion-cookies") && !s.contains("albums")
                                    && !s.contains("STACKTRACE") && !s.contains("com.google") &&
                                    (!personal || (!s.contains("SUBSNEW") && !s.contains("appRestart")
                                            && !s.contains("AUTH") && !s.contains("TAGS")
                                            && !s.contains("SEEN") && !s.contains("HIDDEN")
                                            && !s.contains("HIDDEN_POSTS")))) {
                                File fileToBackup = new File(prefsDir, s);
                                String content = readFileFully(fileToBackup);
                                if (content != null) {
                                    bw.write("<START" + fileToBackup.getName() + ">");
                                    bw.write(content);
                                    bw.write("END>");
                                }
                                publishProgress();
                            }
                        }
                        bw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        // TODO: Handle error appropriately
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                progress.dismiss();
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
     * Asynchronous task to restore settings from a selected file.
     */
    private class RestoreFromFileAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private Uri fileUri;

        RestoreFromFileAsyncTask(Uri fileUri) {
            this.fileUri = fileUri;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            StringBuilder fw = new StringBuilder();
            try {
                InputStream is = getContentResolver().openInputStream(fileUri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                int c = reader.read();
                while (c != -1) {
                    fw.append((char) c);
                    c = reader.read();
                }
                reader.close();
                String read = fw.toString();
                if (read.contains("Slide_backupEND>")) {
                    String[] files = read.split("END>");
                    for (int i = 1; i < files.length; i++) {
                        String innerFile = files[i];
                        String t = innerFile.substring(6, innerFile.indexOf(">"));
                        innerFile = innerFile.substring(innerFile.indexOf(">") + 1);

                        File newF = new File(getApplicationInfo().dataDir + "/shared_prefs/" + t);
                        BufferedWriter bw = new BufferedWriter(new FileWriter(newF));
                        bw.write(innerFile);
                        bw.close();
                    }
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progress.dismiss();
            if (success) {
                new AlertDialog.Builder(SettingsBackup.this)
                        .setTitle(R.string.backup_restore_settings)
                        .setMessage(R.string.backup_restarting)
                        .setOnDismissListener(dialog -> {
                            // Implement app restart logic here, e.g., using ProcessPhoenix or another method
                            // ProcessPhoenix.triggerRebirth(SettingsBackup.this);
                        })
                        .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                            // Implement app restart logic here
                            // ProcessPhoenix.triggerRebirth(SettingsBackup.this);
                        })
                        .setCancelable(false)
                        .show();
            } else {
                showErrorDialog(R.string.err_not_valid_backup, R.string.err_not_valid_backup_msg);
            }
        }
    }

    /**
     * Read the entire content of a file into a String.
     *
     * @param file The file to read.
     * @return The file content as a String, or null if an error occurs.
     */
    private String readFileFully(File file) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (IOException e) {
            Log.e(LogUtil.getTag(), "Error reading file: " + file.getName(), e);
            return null;
        }
    }

    /**
     * Write a String content to a file.
     *
     * @param file    The file to write to.
     * @param content The content to write.
     */
    private void writeStringToFile(File file, String content) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(content);
        } catch (IOException e) {
            Log.e(LogUtil.getTag(), "Error writing to file: " + file.getName(), e);
            errors++;
        }
    }

    /**
     * Asynchronous task to perform backup operations to a directory.
     *
     * @param personal Whether to include personal data.
     */
    // Already defined above (backupToDir method)

    // Additional methods and inner classes as needed...

}
