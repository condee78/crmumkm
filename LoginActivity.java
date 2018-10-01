package com.suupahiro.crmumkm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
// [F] check user credential
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.suupahiro.crmumkm.Config;
import com.suupahiro.crmumkm.HolderActivity;
import com.suupahiro.crmumkm.R;
import com.suupahiro.crmumkm.providers.woocommerce.WooCommerceTask;
import com.suupahiro.crmumkm.providers.woocommerce.model.CredentialStorage;
import com.suupahiro.crmumkm.providers.woocommerce.model.RestAPI;
import com.suupahiro.crmumkm.providers.woocommerce.model.users.User;
import com.suupahiro.crmumkm.util.Helper;
import com.suupahiro.crmumkm.util.Log;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity{

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    private OkHttpClient client;

    // [F] check credential user
    private Activity mAct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //mAct = getActivity();

        // [F] check user credentdial. if already sign-in
        if (CredentialStorage.credentialsAvailable(this)) {
            //refreshItems();
            //loadHeader();
            finish();
            startActivity(new Intent(this, MainActivity.class));
        } else {

        }
        // Set up the login form.
        mEmailView = findViewById(R.id.user);

        mPasswordView = findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = findViewById(R.id.user_sign_in_button);
        TextView mRegisterButton = findViewById(R.id.user_register_button);
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
            }
        });

        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        // temp debug
        //email = "arichan@yahoo.com";
        //password = "arichan2018";

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
            /*[F] change login method
            attemptLogin(email, password);
            */
            authenticateLogin(email, password);
            //startActivity(new Intent(this, MainActivity.class));
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

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    void authenticateLogin(final String mEmail, final String mPassword) {
        WooCommerceTask.WooCommerceBuilder postProcess = new WooCommerceTask.WooCommerceBuilder(this);
        //builder.getUsers(new WooCommerceTask.Callback<User>() {
        postProcess.postUsersAuth(new WooCommerceTask.Callback<User>() {
            @Override
            public void success(ArrayList<User> users) {
                if (users.size() == 1){
                    User user = users.get(0);
                    if(user.getId() != null) {
                        CredentialStorage.saveCredentials(LoginActivity.this,
                                mEmail,
                                mPassword,
                                user.getId(),
                                user.getFirstName(),
                                user.getRole());
                        loginAttemptCompleted(true);
                    }
                    else{
                        loginAttemptCompleted(false);
                    }
                } else {
                    if (users.size() == 0){
                        Log.e("INFO", "No Customers found with this email. Perhaps this person is a user, but not a customer");
                    } else {
                        Log.e("INFO", "More than 1 Customer found with this email");
                    }
                    loginAttemptCompleted(false);
                }
            }

            @Override
            public void failed() {
                loginAttemptCompleted(false);
            }
        }, mEmail, mPassword).execute();

    }
    void attemptLogin(final String mEmail, final String mPassword) {
        RequestBody requestBody = new FormBody.Builder()
                .add("log", mEmail)
                .add("pwd", mPassword)
                .build();

        final RestAPI api = new RestAPI(getApplication());

        Request request = new Request.Builder()
                .url(api.getHost() + api.getLogin())
                .post(requestBody)
                .build();

        Log.i("INFO", "Requesting: " + request.url());

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Helper.isOnlineShowDialog(LoginActivity.this);
                    }
                });
                loginAttemptCompleted(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();

                List<String> cookies = response.headers().values("Set-Cookie");
                for (String cookie : cookies){
                    Log.i("Cookie", cookie);
                    if (cookie.startsWith(api.getLoginCookie())) {
                        retrieveUserData(mEmail, mPassword);
                        return;
                    }
                }

                //No login cookie was found
                loginAttemptCompleted(false);
            }
        });
    }

    public void retrieveUserData(final String mEmail, final String mPassword){
        WooCommerceTask.WooCommerceBuilder builder = new WooCommerceTask.WooCommerceBuilder(this);
        builder.getUsers(new WooCommerceTask.Callback<User>() {
            @Override
            public void success(ArrayList<User> users) {
                if (users.size() == 1){
                    User user = users.get(0);
                    CredentialStorage.saveCredentials(LoginActivity.this,
                            mEmail,
                            mPassword,
                            user.getId(),
                            user.getFirstName(),
                            user.getRole());
                    loginAttemptCompleted(true);
                } else {
                    if (users.size() == 0){
                        Log.e("INFO", "No Customers found with this email. Perhaps this person is a user, but not a customer");
                    } else {
                        Log.e("INFO", "More than 1 Customer found with this email");
                    }
                    loginAttemptCompleted(false);
                }
            }

            @Override
            public void failed() {
                loginAttemptCompleted(false);
            }
        }, mEmail).execute();
    }


    public void loginAttemptCompleted(final Boolean success) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                showProgress(false);

                if (success) {
                    finish();
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                } else {
                    mEmailView.setError(getString(R.string.error_incorrect_credentials));
                    mPasswordView.setError(getString(R.string.error_incorrect_credentials));
                    mPasswordView.requestFocus();
                }
            }
        });
    }
}

