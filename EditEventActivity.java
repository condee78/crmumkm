package com.suupahiro.crmumkm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.suupahiro.crmumkm.providers.woocommerce.model.CredentialStorage;
import com.suupahiro.crmumkm.providers.wordpress.PostItem;
import com.suupahiro.crmumkm.providers.wordpress.api.WordpressGetTaskInfo;
import com.suupahiro.crmumkm.util.Log;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class EditEventActivity extends AppCompatActivity {

    private TextView mTitle;
    private TextView mEventContent;
    private View mProgressView;
    private View mEditFormView;
    //Extra's
    public static final String EXTRA_POSTITEM = "postitem";

    private PostItem post;
    private String jsonUpdate;
    private String urlEditPost = "http://crmumkm.com/wp-json/wp/v2/posts/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Extras
        Bundle bundle = this.getIntent().getExtras();
        post = (PostItem) getIntent().getSerializableExtra(EXTRA_POSTITEM);

        //Views
        mTitle = findViewById(R.id.title);
        mEventContent = findViewById(R.id.eventText);
        mProgressView = findViewById(R.id.loading_progress);
        mEditFormView = findViewById(R.id.edit_form);

        mTitle.setText(post.getTitle());
        mEventContent.setText(post.getContent());

        urlEditPost += post.getId();

        Button btnUpdate = findViewById(R.id.btnUpdate);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UpdateEvent();
            }
        });
    }

    private void UpdateEvent()
    {
        // Reset errors.
        mTitle.setError(null);
        mEventContent.setError(null);

        // Store values at the time of the login attempt.
        String title = mTitle.getText().toString();
        String content = mEventContent.getText().toString();

        // Hide Virtual Keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mTitle.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        showProgress(true);

        jsonUpdate = "{\n" +
                "    \"title\": \"" + title + "\",\n" +
                "    \"content\": \"" + content + "\"\n" +
                "}";

         new EditEventAPI("POST", urlEditPost, jsonUpdate).execute();
    }

    public class EditEventAPI extends AsyncTask<String, String, JSONArray> {

        //Jetpack
        private final String JETPACK_BASE = "https://public-api.wordpress.com/rest/v1.1/sites/";
        private final String JETPACK_FIELDS = "&fields=ID,author,title,URL,content,discussion,featured_image,post_thumbnail,tags,discussion,date,attachments";
        private final SimpleDateFormat JETPACK_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());

        private final String URL_REST_API = "http://crmumkm.com/wp-json/wp/v2/posts";

        private WordpressGetTaskInfo info;
        private String url;
        private String method;
        private String dataJson;

        // [F] add this for BEARER AUTHENTICATION
        // TOKEN WAS TAKEN AT 03.08 2018 03.08 AM
        private static final String BEARER_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOlwvXC9jcm11bWttLmNvbSIsImlhdCI6MTUzMzI0MDUxMiwibmJmIjoxNTMzMjQwNTEyLCJleHAiOjE3NTM5OTI1MTIsImRhdGEiOnsidXNlciI6eyJpZCI6IjEifX19.ORypK328U0aPBeDBO9nTmHOPyxeLoAz9wKopKU3fvv4";
        private final Boolean IS_USE_BEARER = true;

        public EditEventAPI(String methodType, String url, String dataJson) {
            this.method = methodType;
            this.url = url;
            //this.info = info;
            this.dataJson = dataJson;
        }

        @Override
        protected JSONArray doInBackground(String... param) {
            return postJSONItem(url, dataJson);
        }

        @Override
        protected void onPreExecute() {
        }

        protected void onPostExecute(JSONArray result) {

            Log.i("INFO postExecute", result.toString());
            if (result.length() > 0) {
                postingEventCompleted(true, result);
            } else {
                postingEventCompleted(false, result);
            }
            super.onPostExecute(result);
        }

        private JSONArray postJSONItem(String url, String jsonItem) {
            // Making HTTP request
            Log.v("INFO", "Requesting: " + url);

            String result = "";
            StringBuffer chaine = new StringBuffer("");
            try {
                URL urlCon = new URL(url);

                //Open a connection
                HttpURLConnection connection = (HttpURLConnection) urlCon
                        .openConnection();
                connection.setRequestProperty("User-Agent", "CRMUMKM (Android)");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                // [F} add this
                if (this.IS_USE_BEARER) {
                    connection.setRequestProperty("authorization", "Bearer " + this.BEARER_TOKEN);
                }
                connection.connect();

                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                wr.write(jsonItem);
                wr.flush();


                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    //info.pages = connection.getHeaderFieldInt("X-WP-TotalPages", 1);
                }

                result = connection.getResponseCode() + " " + connection.getResponseMessage();
                Log.i("INFO RESPONSE", result);

                //Get the stream from the connection and read it
                InputStream inputStream = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(
                        inputStream));
                String line = "";
                //if (rd != null){
                chaine.append("[");
                //}

                while ((line = rd.readLine()) != null) {
                    chaine.append(line);
                }

                //if (rd.readLine() != null){
                chaine.append("]");
                //}

            } catch (IOException e) {
                // writing exception to log
                Log.e("postJSONItem", "Error posting item");
                Log.printStackTrace(e);
            }

            String response = chaine.toString();
            try {
                return new JSONArray(response);
            } catch (Exception e) {
                Log.e("postJSONItem", "Error parsing JSON. Printing stacktrace now");
                Log.printStackTrace(e);
                return null;
            }
            //return  result;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mEditFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mEditFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mEditFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });

        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mEditFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public void postingEventCompleted(final Boolean success, final JSONArray result) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                showProgress(false);

                if (success) {
                    //saveUserCredential(result);
                    finish();
                    //startActivity(new Intent(getApplicationContext(), RegistrationSuccessActivity.class));
                } else {
                    mTitle.setError(getString(R.string.error_field_general));
                    mEventContent.setError(getString(R.string.error_field_general));
                    mEventContent.requestFocus();
                }
            }
        });
    }
}
