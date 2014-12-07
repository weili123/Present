package com.group15.toq_o.present;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
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
        Toast sync = Toast.makeText(getBaseContext(), "Sync in progress", Toast.LENGTH_LONG);
        sync.show();
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
                /*case REQUEST_GOOGLE_PLAY_SERVICES:
                    if (resultCode == Activity.RESULT_OK) {
                        haveGooglePlayServices();
                    } else {
                        checkGooglePlayServicesAvailable();
                    }
                    break;*/
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
        @Override
        protected Void doInBackground(Void... params) {
            if(getAllPresentationFiles("root") != false) {
                //jump to new activity after all files have been obtained
                Intent intent = new Intent(GoogleDriveActivity.this, ViewFilesActivity.class);
                startActivity(intent);
            }
            return null;
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
                                }
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
}
