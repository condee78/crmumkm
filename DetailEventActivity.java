package com.suupahiro.crmumkm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.suupahiro.crmumkm.attachmentviewer.model.MediaAttachment;
import com.suupahiro.crmumkm.attachmentviewer.ui.AttachmentActivity;
import com.suupahiro.crmumkm.comments.CommentsActivity;
import com.suupahiro.crmumkm.model.Event;
import com.suupahiro.crmumkm.providers.fav.FavDbAdapter;
import com.suupahiro.crmumkm.providers.wordpress.PostItem;
import com.suupahiro.crmumkm.providers.wordpress.api.JsonApiPostLoader;
import com.suupahiro.crmumkm.providers.wordpress.api.RestApiPostLoader;
import com.suupahiro.crmumkm.providers.wordpress.api.WordpressGetTaskInfo;
import com.suupahiro.crmumkm.providers.wordpress.api.providers.JetPackProvider;
import com.suupahiro.crmumkm.providers.wordpress.api.providers.RestApiProvider;
import com.suupahiro.crmumkm.providers.wordpress.ui.WordpressDetailActivity;
import com.suupahiro.crmumkm.util.DetailActivity;
import com.suupahiro.crmumkm.util.Helper;
import com.suupahiro.crmumkm.util.Log;
import com.suupahiro.crmumkm.util.WebHelper;

import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetailEventActivity extends DetailActivity implements JsonApiPostLoader.BackgroundPostCompleterListener {

    //By default, we remove the first image, however, you can disable this
    private static final boolean REMOVE_FIRST_IMG = true;
    //Preload all posts for faster loading, increases API requests
    public static final boolean PRELOAD_POSTS = true;

    //Utilties
    private FavDbAdapter mDbHelper;
    private WebView htmlTextView;
    private TextView mTitle;
    private View mProgressView;
    //Extra's
    public static final String EXTRA_POSTITEM = "postitem";

    private View mDetailEventView;
    public static final String EXTRA_API_BASE = "apiurl";
    public static final String EXTRA_DISQUS = "disqus";

    //Post information
    private PostItem post;
    private String disqusParseable;
    private String apiBase;

    private String URL_POST = "http://crmumkm.com/wp-json/wp/v2/posts";
    private String question_delete = "Hapus acara ";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Use the general detaillayout and set the viewstub for wordpress
        setContentView(R.layout.activity_details);
        ViewStub stub = findViewById(R.id.layout_stub);
        stub.setLayoutResource(R.layout.activity_detail_event);
        View inflated = stub.inflate();

        mToolbar = findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Views
        thumb = findViewById(R.id.image);
        coolblue = findViewById(R.id.coolblue);
        mTitle = findViewById(R.id.title);
        TextView mDateAuthorView = findViewById(R.id.dateauthorview);

        mDetailEventView = findViewById(R.id.scroller);
        //mProgressView = findViewById(R.id.progressBar);
        mProgressView = findViewById(R.id.loading_progress);

        //Extras
        Bundle bundle = this.getIntent().getExtras();
        post = (PostItem) getIntent().getSerializableExtra(EXTRA_POSTITEM);
        disqusParseable = getIntent().getStringExtra(EXTRA_DISQUS);
        apiBase = getIntent().getStringExtra(EXTRA_API_BASE);

        Button editButton = findViewById(R.id.edit);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), EditEventActivity.class);
                intent.putExtra(DetailEventActivity.EXTRA_POSTITEM, post);
                startActivity(intent);
                //startActivity(new Intent(getApplicationContext(), EditEventActivity.class));
            }
        });

        Button deleteButton = findViewById(R.id.delete);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteProcess(post.getId(), post.getTitle());
            }
        });

        //If we have a post and a bundle
        if (null != post && null != bundle) {

            String dateAuthorString;
            if (post.getDate() != null)
                dateAuthorString = getResources().getString(R.string.wordpress_subtitle_start) +
                        DateUtils.getRelativeDateTimeString(this, post.getDate().getTime(), DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL)
                        + getResources().getString(R.string.wordpress_subtitle_end)
                        + post.getAuthor();
            else
                dateAuthorString = post.getAuthor();

            mTitle.setText(post.getTitle());
            mDateAuthorView.setText(dateAuthorString);

            loadHeaderImage();
            configureFAB();
            setUpHeader(post.getImageCandidate());

            Helper.admobLoader(this, findViewById(R.id.adView));

            configureContentWebView();

            //If the post is completed, load the body. Else, retrieve the full body first
            if (post.getPostType() == PostItem.PostType.JSON && !post.isCompleted()) {
                new JsonApiPostLoader(post, getIntent().getStringExtra(EXTRA_API_BASE), this).start();
            } else if (post.getPostType() == PostItem.PostType.REST && !post.isCompleted()) {
                new RestApiPostLoader(post, getIntent().getStringExtra(EXTRA_API_BASE), this).start();
                loadPostBody(post);
            } else {
                loadPostBody(post);
            }
        }
    }

    public void deleteProcess(final long postID, String title){

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        //String reconfirm = R.string.delete_user_confirmation + " *" + username;
        String reconfirm = question_delete + title + " ?";
        alertDialogBuilder.setTitle(reconfirm);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                showProgress(true);
                                // TODO DELETE user --> delete or change roles?????
                                String url = URL_POST + "/" + postID + "?force=true";
                                new DeleteEventAPI("DELETE", url, null).execute();
                                //notifyDataSetChanged();
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }


    private void configureContentWebView(){
        htmlTextView = findViewById(R.id.htmlTextView);
        htmlTextView.getSettings().setJavaScriptEnabled(true);
        htmlTextView.setBackgroundColor(Color.TRANSPARENT);
        htmlTextView.getSettings().setDefaultFontSize(
                WebHelper.getWebViewFontSize(this));
        htmlTextView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        htmlTextView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null
                        && (url.endsWith(".png") || url
                        .endsWith(".jpg") || url
                        .endsWith(".jpeg"))) {

                    AttachmentActivity.startActivity(DetailEventActivity.this, MediaAttachment.withImage(
                            url
                    ));

                    return true;
                } else if (url != null
                        && (url.startsWith("http://") || url
                        .startsWith("https://"))) {
                    HolderActivity.startWebViewActivity(DetailEventActivity.this, url, Config.OPEN_INLINE_EXTERNAL, false, null);
                    return true;
                } else {
                    Uri uri = Uri.parse(url);
                    Intent ViewIntent = new Intent(Intent.ACTION_VIEW, uri);

                    // Verify it resolves
                    PackageManager packageManager = getPackageManager();
                    List<ResolveInfo> activities = packageManager
                            .queryIntentActivities(ViewIntent, 0);
                    boolean isIntentSafe = activities.size() > 0;

                    // Start an activity if it's safe
                    if (isIntentSafe) {
                        startActivity(ViewIntent);
                    }
                    return true;
                }
            }
        });
    }

    private void configureFAB(){
        if (post.getAttachments() != null && post.getAttachments().size() > 1 && Config.WP_ATTACHMENTS_BUTTON){
            FloatingActionButton fb = findViewById(R.id.attachments_button);
            fb.setVisibility(View.VISIBLE);
            fb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AttachmentActivity.startActivity(DetailEventActivity.this, post.getAttachments());
                }
            });

            for (MediaAttachment att : post.getAttachments()){
                Log.v("INFO", att.toString());
            }
        }
    }

    private void loadHeaderImage(){
        String imageurl = post.getImageCandidate();
        if ((null != imageurl && !imageurl.equals("") && !imageurl.equals("null"))) {
            Picasso.with(this).load(imageurl).fit().centerCrop().into(thumb);
            thumb.setOnClickListener(new View.OnClickListener() {
                public void onClick(View arg0) {

                    if (post.getAttachments() != null) {

                        //Make sure that the featured attachment is (the first) in the list
                        String imageUrl = post.getImageCandidate();
                        ArrayList<MediaAttachment> attachmentList = new ArrayList<MediaAttachment>();
                        boolean inAttachments = false;
                        for (MediaAttachment attachment : post.getAttachments()){
                            if (imageUrl.equals(attachment.getUrl()) || imageUrl.equals(attachment.getThumbnailUrl())){
                                attachmentList.add(0, attachment);
                                inAttachments = true;
                            } else {
                                attachmentList.add(attachment);
                            }
                        }
                        if (!inAttachments){
                            attachmentList.add(0, MediaAttachment.withImage(imageUrl));
                        }

                        //Show attachments
                        AttachmentActivity.startActivity(DetailEventActivity.this, attachmentList);
                    }

                }
            });

            findViewById(R.id.scroller).setOnTouchListener(new View.OnTouchListener() {

                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return (findViewById(R.id.progressBar).getVisibility() == View.VISIBLE) && android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN;
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        htmlTextView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (htmlTextView != null)
            htmlTextView.onResume();
    }


    private void loadPostBody(final PostItem result) {
        if (null != result) {
            setHTML(result.getContent());

            //If we have a commentsArray or a disqus url, enable comments
            if ((result.getCommentCount() != null &&
                    result.getCommentCount() != 0 &&
                    result.getCommentsArray() != null) ||
                    disqusParseable != null ||
                    ((post.getPostType() == PostItem.PostType.JETPACK || post.getPostType() == PostItem.PostType.REST) &&
                            result.getCommentCount() != 0)) {

                Button btnComment = findViewById(R.id.comments);

                //Set the comments count if we have it available
                if (result.getCommentCount() == 0 || (result.getCommentCount() == 10 && post.getPostType() == PostItem.PostType.REST))
                    btnComment.setText(getResources().getString(R.string.comments));
                else
                    btnComment.setText(Helper.formatValue(result.getCommentCount()) + " " + getResources().getString(R.string.comments));

                btnComment.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View arg0) {

                        Intent commentIntent = new Intent(DetailEventActivity.this, CommentsActivity.class);

                        if (disqusParseable != null) {
                            commentIntent.putExtra(CommentsActivity.DATA_PARSEABLE, disqusParseable);
                            commentIntent.putExtra(CommentsActivity.DATA_TYPE, CommentsActivity.DISQUS);
                            commentIntent.putExtra(CommentsActivity.DATA_ID, post.getId().toString());
                        } else {
                            if (post.getPostType() == PostItem.PostType.JETPACK) {
                                commentIntent.putExtra(CommentsActivity.DATA_PARSEABLE, JetPackProvider.getPostCommentsUrl(apiBase, post.getId().toString()));
                                commentIntent.putExtra(CommentsActivity.DATA_TYPE, CommentsActivity.WORDPRESS_JETPACK);
                            } else if (post.getPostType() == PostItem.PostType.REST){
                                commentIntent.putExtra(CommentsActivity.DATA_PARSEABLE, RestApiProvider.getPostCommentsUrl(apiBase, post.getId().toString()));
                                commentIntent.putExtra(CommentsActivity.DATA_TYPE, CommentsActivity.WORDPRESS_REST);
                            } else if (post.getPostType() == PostItem.PostType.JSON) {
                                commentIntent.putExtra(CommentsActivity.DATA_PARSEABLE, result.getCommentsArray().toString());
                                commentIntent.putExtra(CommentsActivity.DATA_TYPE, CommentsActivity.WORDPRESS_JSON);
                            }
                        }

                        startActivity(commentIntent);
                    }
                });
            }
        } else {
            findViewById(R.id.progressBar).setVisibility(View.GONE);

            Helper.noConnection(DetailEventActivity.this);
        }
    }

    public void setHTML(String source) {
        Document doc = Jsoup.parse(source);

        //Remove the first image to prevent a repetition of the header image (if enabled and present)
        if (REMOVE_FIRST_IMG) {
            if (doc.select("img") != null && doc.select("img").first() != null)
                doc.select("img").first().remove();
        }

        String html = WebHelper.docToBetterHTML(doc, this);

        htmlTextView.loadDataWithBaseURL(post.getUrl(), html, "text/html", "UTF-8", "");
        htmlTextView.setVisibility(View.VISIBLE);
        findViewById(R.id.progressBar).setVisibility(View.GONE);
    }

    @Override
    public void completed(final PostItem item) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (item.getPostType() == PostItem.PostType.JSON)
                        loadPostBody(item);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    public class DeleteEventAPI extends AsyncTask<String, String, JSONArray> {

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

        public DeleteEventAPI(String methodType, String url, String dataJson) {
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
                connection.setRequestMethod("DELETE");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                // [F} add this
                if (this.IS_USE_BEARER) {
                    connection.setRequestProperty("authorization", "Bearer " + this.BEARER_TOKEN);
                }
                connection.connect();

                //OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                //wr.write(jsonItem);
                //wr.flush();


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

            mDetailEventView.setVisibility(show ? View.GONE : View.VISIBLE);
            mDetailEventView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mDetailEventView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mDetailEventView.setVisibility(show ? View.GONE : View.VISIBLE);


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
                    startActivity(new Intent(getApplicationContext(), ListEventActivity.class));
                } else {
                    //mTitle.setError(getString(R.string.error_field_general));
                    /*mEventContent.setError(getString(R.string.error_field_general));
                    mEventContent.requestFocus();*/
                }
            }
        });
    }

}
