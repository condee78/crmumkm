package com.suupahiro.crmumkm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.suupahiro.crmumkm.attachmentviewer.model.MediaAttachment;
import com.suupahiro.crmumkm.model.Event;
import com.suupahiro.crmumkm.providers.woocommerce.model.CredentialStorage;
import com.suupahiro.crmumkm.providers.woocommerce.model.users.User;
import com.suupahiro.crmumkm.providers.wordpress.PostItem;
import com.suupahiro.crmumkm.providers.wordpress.api.WordpressGetTaskInfo;
import com.suupahiro.crmumkm.providers.wordpress.api.WordpressPostsTask;
import com.suupahiro.crmumkm.util.Helper;
import com.suupahiro.crmumkm.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private EditText mUserNameView;
    private EditText mMobilePhoneView;
    private View mProgressView;
    private View mLoginFormView;

    private String urlRegister = "http://crmumkm.com/wp-json/wp/v2/users";
    private String jsonRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Set up the login form.
        mUserNameView = findViewById(R.id.user_name);
        mEmailView = findViewById(R.id.user);
        mMobilePhoneView = findViewById(R.id.mobile_phone);
        mPasswordView = findViewById(R.id.password);

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        TextView mRegisterButton = findViewById(R.id.user_register_button);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
                //startActivity(new Intent(getApplicationContext(), RegistrationSuccessActivity.class));
                //saveUserCredential();
            }
        });
    }

    private void attemptLogin()
    {
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUserNameView.getText().toString();
        String email = mEmailView.getText().toString();
        String mobilePhone = mMobilePhoneView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Hide Virtual Keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mEmailView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            jsonRegister = "{\n" +
                    "    \"username\": \""+ username +"\",\n" +
                    "    \"email\": \""+ email +"\",\n" +
                    "    \"password\": \""+ password +"\"\n" +
                    "}";


            new registerAPI("POST",urlRegister, jsonRegister ).execute();

        }

    }

    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 3;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public void loginAttemptCompleted(final Boolean success, final JSONArray result) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                showProgress(false);

                if (success) {
                    saveUserCredential(result);
                    finish();
                    startActivity(new Intent(getApplicationContext(), RegistrationSuccessActivity.class));
                } else {
                    mEmailView.setError(getString(R.string.error_incorrect_credentials));
                    mPasswordView.setError(getString(R.string.error_incorrect_credentials));
                    mPasswordView.requestFocus();
                }
            }
        });
    }

    public void saveUserCredential(JSONArray dataJson) {
        int userID =-1;
        String userName = "";
        JSONArray jUserRole = null;
        String userRole = "";
        /* just for tested
        JSONArray dataJson = null;
        try {
            dataJson = new JSONArray("[{\"id\":19,\"username\":\"android5\",\"name\":\"android5\",\"first_name\":\"\",\"last_name\":\"\",\"email\":\"android5@yah.com\",\"url\":\"\",\"description\":\"\",\"link\":\"http:\\/\\/crmumkm.com\\/author\\/android5\\/\",\"locale\":\"en_US\",\"nickname\":\"android5\",\"slug\":\"android5\",\"roles\":[\"seller\"],\"registered_date\":\"2018-08-09T20:19:46+00:00\",\"capabilities\":{\"read\":true,\"publish_posts\":true,\"edit_posts\":true,\"delete_published_posts\":true,\"edit_published_posts\":true,\"delete_posts\":true,\"manage_categories\":true,\"moderate_comments\":true,\"unfiltered_html\":true,\"upload_files\":true,\"edit_shop_orders\":true,\"edit_product\":true,\"read_product\":true,\"delete_product\":true,\"edit_products\":true,\"publish_products\":true,\"read_private_products\":true,\"delete_products\":true,\"delete_private_products\":true,\"delete_published_products\":true,\"edit_private_products\":true,\"edit_published_products\":true,\"manage_product_terms\":true,\"delete_product_terms\":true,\"assign_product_terms\":true,\"dokandar\":true,\"dokan_view_sales_overview\":true,\"dokan_view_sales_report_chart\":true,\"dokan_view_announcement\":true,\"dokan_view_order_report\":true,\"dokan_view_review_reports\":true,\"dokan_view_product_status_report\":true,\"dokan_view_overview_report\":true,\"dokan_view_daily_sale_report\":true,\"dokan_view_top_selling_report\":true,\"dokan_view_top_earning_report\":true,\"dokan_view_statement_report\":true,\"dokan_view_order\":true,\"dokan_manage_order\":true,\"dokan_manage_order_note\":true,\"dokan_manage_refund\":true,\"dokan_add_coupon\":true,\"dokan_edit_coupon\":true,\"dokan_delete_coupon\":true,\"dokan_view_reviews\":true,\"dokan_manage_reviews\":true,\"dokan_manage_withdraw\":true,\"dokan_add_product\":true,\"dokan_edit_product\":true,\"dokan_delete_product\":true,\"dokan_view_product\":true,\"dokan_duplicate_product\":true,\"dokan_import_product\":true,\"dokan_export_product\":true,\"dokan_view_overview_menu\":true,\"dokan_view_product_menu\":true,\"dokan_view_order_menu\":true,\"dokan_view_coupon_menu\":true,\"dokan_view_report_menu\":true,\"dokan_view_review_menu\":true,\"dokan_view_withdraw_menu\":true,\"dokan_view_store_settings_menu\":true,\"dokan_view_store_payment_menu\":true,\"dokan_view_store_shipping_menu\":true,\"dokan_view_store_social_menu\":true,\"dokan_view_store_seo_menu\":true,\"seller\":true},\"extra_capabilities\":{\"seller\":true},\"avatar_urls\":{\"24\":\"http:\\/\\/0.gravatar.com\\/avatar\\/c3a43bbc94bb6766be084dc5151a03e5?s=24&d=mm&r=g\",\"48\":\"http:\\/\\/0.gravatar.com\\/avatar\\/c3a43bbc94bb6766be084dc5151a03e5?s=48&d=mm&r=g\",\"96\":\"http:\\/\\/0.gravatar.com\\/avatar\\/c3a43bbc94bb6766be084dc5151a03e5?s=96&d=mm&r=g\"},\"meta\":[],\"_links\":{\"self\":[{\"href\":\"http:\\/\\/crmumkm.com\\/wp-json\\/wp\\/v2\\/users\\/19\"}],\"collection\":[{\"href\":\"http:\\/\\/crmumkm.com\\/wp-json\\/wp\\/v2\\/users\"}]}}]");
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

        try {
            userID = dataJson.getJSONObject(0).getInt("id");
            userName = dataJson.getJSONObject(0).getString("username");
            jUserRole = dataJson.getJSONObject(0).getJSONArray("roles");

            userRole = jUserRole.getString(0);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        CredentialStorage.saveCredentials(RegisterActivity.this,
                mEmailView.getText().toString(),
                mPasswordView.getText().toString(),
                userID,
                userName,
                userRole
                );

    }

    public class registerAPI extends AsyncTask<String, String, JSONArray> {

        //Jetpack
        private final String JETPACK_BASE = "https://public-api.wordpress.com/rest/v1.1/sites/";
        private final String JETPACK_FIELDS = "&fields=ID,author,title,URL,content,discussion,featured_image,post_thumbnail,tags,discussion,date,attachments";
        private final SimpleDateFormat JETPACK_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());

        private final String URL_REST_API = "http://crmumkm.com/wp-json/wp/v2/users";

        private WordpressGetTaskInfo info;
        private String url;
        private String method;
        private String dataJson;

        // [F] add this for BEARER AUTHENTICATION
        // TOKEN WAS TAKEN AT 03.08 2018 03.08 AM
        private static final String BEARER_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOlwvXC9jcm11bWttLmNvbSIsImlhdCI6MTUzMzI0MDUxMiwibmJmIjoxNTMzMjQwNTEyLCJleHAiOjE3NTM5OTI1MTIsImRhdGEiOnsidXNlciI6eyJpZCI6IjEifX19.ORypK328U0aPBeDBO9nTmHOPyxeLoAz9wKopKU3fvv4";
        private final Boolean IS_USE_BEARER = true;

        public registerAPI(String methodType, String url, String dataJson ){
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
            if (result.length() > 0)
            {
                loginAttemptCompleted(true, result);
            }
            else
            {
                loginAttemptCompleted(false, result);
            }
            super.onPostExecute(result);
        }

        private JSONArray postJSONItem(String url, String jsonItem){
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

        public ArrayList<PostItem> parsePostsFromUrl(WordpressGetTaskInfo info, String url) {
            //Get JSON
            JSONObject json = Helper.getJSONObjectFromUrl(url);
            if (json == null) return null;

            ArrayList<PostItem> result = null;
            try {

                info.pages = json.getInt("found") / WordpressPostsTask.PER_PAGE + (json.getInt("found") % WordpressPostsTask.PER_PAGE == 0 ? 0 : 1);

                // parsing json object
                if (json.has("posts")) {
                    JSONArray posts = json.getJSONArray("posts");

                    result = new ArrayList<PostItem>();

                    for (int i = 0; i < posts.length(); i++) {
                        try {
                            JSONObject post = posts.getJSONObject(i);
                            PostItem item = itemFromJsonObject(post);

                            if (!item.getId().equals(info.ignoreId)) {
                                result.add(item);
                            }
                        } catch (Exception e) {
                            Log.v("INFO", "Item " + i + " of " + posts.length()
                                    + " has been skipped due to exception!");
                            Log.printStackTrace(e);
                        }
                    }
                }
            } catch (Exception e) {
                Log.printStackTrace(e);
            }

            return result;
        }

        public PostItem itemFromJsonObject(JSONObject post) throws JSONException {
            PostItem item = new PostItem(PostItem.PostType.JETPACK);

            item.setId(post.getLong("ID"));
            item.setAuthor(post.getJSONObject("author").getString("name"));
            try {
                item.setDate(JETPACK_DATE_FORMAT.parse(post.getString("date")));
            } catch (ParseException e) {
                Log.printStackTrace(e);
            }
            item.setTitle(Html.fromHtml(post.getString("title"))
                    .toString());
            item.setUrl(post.getString("URL"));
            item.setContent(post.getString("content"));
            item.setCommentCount(post.getJSONObject("discussion").getLong("comment_count"));
            item.setFeaturedImageUrl(post.getString("featured_image"));

            //Set the thumbnail and establish the ID of the post thumbnail
            long thumbId = -1;
            if (!post.isNull("post_thumbnail")) {
                thumbId = post.getJSONObject("post_thumbnail").getLong("ID");
                item.setThumbnailUrl(post.getJSONObject("post_thumbnail").getString("URL"));
            }

            if (post.has("attachments") && post.getJSONObject("attachments").names() != null) {
                JSONObject attachments = post.getJSONObject("attachments");
                for (int i = 0; i < attachments.names().length(); i++) {
                    JSONObject attachment = attachments.getJSONObject(attachments.names().getString(i));
                    String thumbnail = (attachment.has("thumbnails") &&
                            attachment.getJSONObject("thumbnails").has("thumbnail")) ?
                            attachment.getJSONObject("thumbnails").getString("thumbnail") : null;

                    String title = attachment.has("title") ? attachment.getString("title") : null;
                    MediaAttachment mediaAttachment = new MediaAttachment(attachment.getString("URL"), attachment.getString("mime_type"), thumbnail, title);
                    item.addAttachment(mediaAttachment);

                    //We obtained a thumbnail ID earlier. And set a thumbnail image earlier
                    //If a smaller thumbnail is available (thumbnail of thumbnail) we'll use it.
                    if (attachment.getLong("ID") == thumbId &&
                            attachment.has("thumbnails") &&
                            attachment.getJSONObject("thumbnails").has("thumbnail")) {
                        item.setThumbnailUrl(attachment.getJSONObject("thumbnails").getString("thumbnail"));
                    }
                }

            }

            //If there are tags, save the first one
            JSONObject tags = post.getJSONObject("tags");
            if (tags != null && tags.names() != null && tags.names().length() > 0)
                item.setTag(tags.getJSONObject(tags.names().getString(0)).getString("slug"));

            item.setPostCompleted();

            return item;
        }
    }
}