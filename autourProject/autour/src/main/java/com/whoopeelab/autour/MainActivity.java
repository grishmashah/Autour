package com.whoopeelab.autour;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.whoopeelab.autour.utils.Utilities;

public class MainActivity extends Activity {

    public static final int LOGIN = 0;
    private static final int MAIN = 2;
    private static final int LOADING = 1;
    private Fragment[] fragments = new Fragment[3];

    private UiLifecycleHelper uiHelper;
    private Session.StatusCallback sessionCallback = new Session.StatusCallback() {
        @Override
        public void call(final Session session, final SessionState state, final Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uiHelper = new UiLifecycleHelper(this, sessionCallback);
        uiHelper.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        FragmentManager fm = getFragmentManager();
        LoginFragment loginFragment = (LoginFragment) fm.findFragmentById(R.id.loginFragment);
        fragments[LOGIN] = loginFragment;
        fragments[MAIN] = fm.findFragmentById(R.id.mainFragment);
        fragments[LOADING] = fm.findFragmentById(R.id.loadingFragment);

        FragmentTransaction transaction = fm.beginTransaction();
        for(int i = 0; i < fragments.length; i++) {
            transaction.hide(fragments[i]);
        }
        transaction.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiHelper.onResume();

        Session session = Session.getActiveSession();
        if (session != null && session.isOpened()) {
            SharedPreferences sharedPref = Utilities.getAutourSharedPreference(this);
            if(sharedPref.getString("current_user_id", null) == null) {
                showFragment(LOADING, false);
            }
            else {
                MainFragment mf = (MainFragment) fragments[MAIN];
                mf.initializeFragment();
                showFragment(MAIN, false);
            }
        } else {
            showFragment(LOGIN, false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (session!=null && session.isOpened()) {
            makeMeRequest(session);
        }
    }

    // method to get information about logged in user from Facebook
    private void makeMeRequest(final Session session) {
        Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
            @Override
            public void onCompleted(GraphUser user, Response response) {
                if (session == Session.getActiveSession()) {
                    if (user != null) {
                        SharedPreferences sharedPref = Utilities.getAutourSharedPreference(getApplicationContext());
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("current_user_id", user.getId());
                        editor.putString("current_user_name", user.getName());
                        editor.commit();

                        MainFragment mf = (MainFragment) fragments[MAIN];
                        mf.initializeFragment();

                        showFragment(MAIN, false);
                    }
                }
                if (response.getError() != null) {
                }
            }
        });
        Bundle params = new Bundle();
        params.putString("fields", "id,name");
        request.setParameters(params);
        request.executeAsync();
    }

    public void showFragment(int fragmentIndex, boolean addToBackStack) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        for (int i = 0; i < fragments.length; i++) {
            if (i == fragmentIndex) {
                transaction.show(fragments[i]);
            } else {
                transaction.hide(fragments[i]);
            }
        }
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
}