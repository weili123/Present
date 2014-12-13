package com.group15.toq_o.present;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.group15.toq_o.present.Presentation.Presentation;

import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class GoogleDriveActivity extends Activity{

    GoogleAccountCredential credential;
    static final int REQUEST_ACCOUNT_PICKER = 2;
    static final int REQUEST_AUTHORIZATION = 1;
    final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
    private Drive service;
    ArrayList<String> files;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        files = new ArrayList<String>();
        credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton(DriveScopes.DRIVE));
        service = new Drive.Builder(httpTransport, new GsonFactory(), credential)
                .setApplicationName("Present/1.0").build();
    }

    public void syncDrive(View view) {
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_AUTHORIZATION:
                    if (resultCode == Activity.RESULT_OK) {
                        //do nothing
                        System.out.println("unable to authorize");
                    } else {
                        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
                    }
                    break;
                case REQUEST_ACCOUNT_PICKER:
                    if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                        String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                        if (accountName != null) {
                            credential.setSelectedAccountName(accountName);
                        }
                    }
                    break;
            }
            QueryFilesAsync queryTask = new QueryFilesAsync();
            queryTask.execute();
        }
    }

    //queries all files from root
    private void queryFiles() throws IOException {

    }

    class QueryFilesAsync extends AsyncTask<Void, Void, Void> {

        int progress;

        ArrayList<FileDescriptor> downloadURLs = new ArrayList<FileDescriptor> ();
        ProgressDialog progressDialog;

        private class FileDescriptor {
            String downloadUrl;
            File file;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(GoogleDriveActivity.this);
            //Set the progress dialog to display a horizontal progress bar
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            //Set the dialog title to 'Loading...'
            progressDialog.setTitle("Loading...");
            //Set the dialog message to 'Loading application View, please wait...'
            progressDialog.setMessage("Downloading Files, please wait...");
            //This dialog can't be canceled by pressing the back key
            progressDialog.setCancelable(false);
            //This dialog isn't indeterminate
            progressDialog.setIndeterminate(false);
            //The maximum number of items is 100
            progressDialog.setMax(100);
            //Set the current progress to zero
            progressDialog.setProgress(0);
            //Display the progress dialog
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (!checkAuthorization("root")) {
                return null;
            }
            if(getAllPresentationFiles("root") != false) {
                progressDialog.setProgress(20);
                progress = 20;
                //download files
                download();
                //jump to new activity after all files have been obtained
                progressDialog.dismiss();
                Presentation ppt = createPresentation("Human Models", true);
                PresentApplication app = (PresentApplication) getApplication();
                app.addToPresentationHashMap("Human Models", ppt);
                Intent intent = new Intent(GoogleDriveActivity.this, ViewFilesActivity.class);
                startActivity(intent);

            }
            return null;
        }

        private boolean checkAuthorization(String path) {
            try {
                Drive.Children.List request = service.children().list(path);
                try {
                    request.execute();
                } catch (UserRecoverableAuthIOException e) {
                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                    return false;
                }
            } catch (Exception e) {
                System.out.println("should not fail");
                e.printStackTrace();
            }
            return true;
        }

        private boolean getAllPresentationFiles(String path) {
            try {
                Drive.Children.List request = service.children().list(path);
                do {
                    try {
                        ChildList children = null;
                        try {
                            children = request.execute();
                        } catch (UserRecoverableAuthIOException e) {
                            startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                            return false;
                        }

                        for (ChildReference child : children.getItems()) {
                            String fileId = child.getId();
                            File file = service.files().get(fileId).execute();
                            String mimeType = file.getMimeType();
                            //check to see if it is a presentation
                            if (mimeType.equals("application/vnd.google-apps.presentation")) {
                                //get url of file
                                String downloadUrl = file.getExportLinks().get("application/pdf");
                                if (downloadUrl != null) {
                                    FileDescriptor desc = new FileDescriptor();
                                    desc.downloadUrl = downloadUrl;
                                    desc.file = file;
                                    downloadURLs.add(desc);
                                } else {
                                    System.out.println(fileId);
                                    System.out.println("not stored " + file.getTitle());
                                }
                                /*if (downloadUrl != null) {
                                    //download + store file as pdf
                                    InputStream pdfStream = downloadFile(downloadUrl);
                                    String filename = turnInputStreamIntoFile(pdfStream);
                                    Presentation slides = new Presentation(file.getTitle(), filename, file.getModifiedDate(), fileId);
                                    //store Presentation object into application
                                    PresentApplication app = (PresentApplication) getApplication();
                                    app.addToPresentationHashMap(fileId, slides);
                                } else {
                                    System.out.println(fileId);
                                    System.out.println("not stored " + file.getTitle());
                                }*/
                            }
                            if (mimeType.equals("application/vnd.google-apps.folder")) {
                                getAllPresentationFiles(fileId);
                            }
                        }
                        request.setPageToken(children.getNextPageToken());
                    } catch (IOException e) {
                        System.out.println("An error occurred: " + e);
                        request.setPageToken(null);
                    }
                } while (request.getPageToken() != null && request.getPageToken().length() > 0);
                return true;
            } catch(IOException e) {
                System.out.println("failed");
                return false;
            }
        }

        private void download() {
            int size = downloadURLs.size();
            int progressCounter = 80/size;
            for(int i=0;i<size;i++) {
                try {
                    FileDescriptor desc = downloadURLs.get(i);
                    String downloadUrl = desc.downloadUrl;
                    File file = desc.file;
                    String fileId = file.getId();
                    InputStream pdfStream = downloadFile(downloadUrl);
                    String filename = turnInputStreamIntoFile(pdfStream);
                    Presentation slides = new Presentation(file.getTitle(), filename, file.getModifiedDate(), fileId);
                    //store Presentation object into application
                    PresentApplication app = (PresentApplication) getApplication();
                    app.addToPresentationHashMap(fileId, slides);
                } catch (Exception e) {
                    System.out.println("failed");
                    e.printStackTrace();
                }
                progress += progressCounter;
                progressDialog.setProgress(progress);
            }
        }
    }

    private InputStream downloadFile(String url) {
        try {
            HttpResponse resp = service.getRequestFactory().buildGetRequest(new GenericUrl(url)).execute();
            return resp.getContent();
        } catch (IOException e) {
            // An error occurred.
            e.printStackTrace();
            return null;
        }
    }

    private String turnInputStreamIntoFile(InputStream stream) throws IOException {
        String root = Environment.getExternalStorageDirectory().toString();
        java.io.File myDir = new java.io.File(root + "/pdf");
        myDir.mkdirs();
        java.io.File file;
        String random;
        do {
            random = new BigInteger(130, new SecureRandom()).toString(32);
            random = random + ".pdf";
            file = new java.io.File(myDir, random);
        } while(file.exists());

        try {
            FileOutputStream out = new FileOutputStream(file);
            IOUtils.copy(stream, out);
            out.flush();
            out.close();
        } catch(Exception e) {
            System.out.println("should not error");
            e.printStackTrace();
            return null;
        }
        return random;
    }

    private Presentation createPresentation(String name, boolean canSync) {
        Presentation ppt = new Presentation(name, "sample", new DateTime(new Date()), name, 3);
        ppt.setCanSync(canSync);
        return ppt;
    }
}
