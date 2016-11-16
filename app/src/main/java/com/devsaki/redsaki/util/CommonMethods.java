package com.devsaki.redsaki.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.devsaki.redsaki.MeActivity;
import com.devsaki.redsaki.R;
import com.devsaki.redsaki.SearchActivity;
import com.devsaki.redsaki.SearchSubredditActivity;
import com.devsaki.redsaki.dto.MeDTO;

import org.json.JSONException;

/**
 * Created by DevSaki on 13/11/2016.
 */

public class CommonMethods {

    public static boolean checkLogin(final Context context){
        SharedPreferences sharedPref = sharedPref = context.getSharedPreferences("private", Context.MODE_PRIVATE);
        if (sharedPref.getString("access_code", null) == null) {
            AlertDialog.Builder messageDialog = new AlertDialog.Builder(context);
            messageDialog.setTitle(R.string.login).setMessage(R.string.login_message);

            messageDialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                    CustomTabsIntent customTabsIntent = builder.build();
                    Uri.Builder uriBuilder = new Uri.Builder();
                    uriBuilder.scheme("https")
                            .authority("www.reddit.com")
                            .appendPath("api")
                            .appendPath("v1")
                            .appendPath("authorize")
                            .appendQueryParameter("client_id", context.getString(R.string.client_id))
                            .appendQueryParameter("response_type", "code")
                            .appendQueryParameter("state", "connect")
                            .appendQueryParameter("redirect_uri", "redsaki://oauth")
                            .appendQueryParameter("duration", "permanent")
                            .appendQueryParameter("scope", "identity,edit,flair,history,modconfig,modflair,modlog,modposts,modwiki,mysubreddits,privatemessages,read,report,save,submit,subscribe,vote,wikiedit,wikiread");
                    customTabsIntent.launchUrl((Activity) context, uriBuilder.build());
                }
            }).show();

            return false;
        }else {
            return true;
        }
    }

    public static void search(final Context context, final String subreddit){
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
        builderSingle.setTitle(R.string.search);

        CharSequence[] array = null;
        if (subreddit != null)
            array = new CharSequence[]{context.getString(R.string.search_in_this_subreddit), context.getString(R.string.search_all_subreddit), context.getString(R.string.search_all_posts)};
        else
            array = new CharSequence[]{context.getString(R.string.search_all_subreddit), context.getString(R.string.search_all_posts)};


        builderSingle.setItems(array, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int aux = 0;
                final int string;

                if (subreddit == null)
                    aux = 1;
                if (which + aux == 0) {
                    string = R.string.search_in_this_subreddit;
                } else if (which + aux == 1) {
                    string = R.string.search_all_subreddit;
                } else {
                    string = R.string.search_all_posts;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(string);

                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String query = input.getText().toString();

                        if(string==R.string.search_in_this_subreddit || string == R.string.search_all_posts){
                            if (string == R.string.search_in_this_subreddit){
                                query = query + " subreddit:" + subreddit;
                            }
                            Intent intent = new Intent(context, SearchActivity.class);
                            intent.putExtra(SearchActivity.ARG_QUERY, query);
                            context.startActivity(intent);
                        }else{
                            Intent intent = new Intent(context, SearchSubredditActivity.class);
                            intent.putExtra(SearchActivity.ARG_QUERY, query);
                            context.startActivity(intent);
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        builderSingle.show();
    }

    public static void commonMenu(Context context, String subreddit, int id){
        SharedPreferences sharedPref = sharedPref = context.getSharedPreferences("private", Context.MODE_PRIVATE);
        if (id == R.id.action_login) {
            if (!CommonMethods.checkLogin(context)) {

            } else if (sharedPref.getString("username", null) == null) {
                new LoadMeAsyncTask(context).execute();
            } else {
                Intent intent = new Intent(context, MeActivity.class);
                context.startActivity(intent);
            }
        } else if (id == R.id.action_search) {
            CommonMethods.search(context, subreddit);
        }
    }

    static class LoadMeAsyncTask extends AsyncTask<Void, Void, MeDTO> {

        Context mContext;

        LoadMeAsyncTask(Context context){
            this.mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(mContext, R.string.loading_user_data, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected MeDTO doInBackground(Void... params) {
            Log.i("LoadListAsyncTask", "doInBackground");
            MeDTO result = null;
            try {
                result = HttpCallUtil.extractMe((Activity) mContext);
            } catch (java.io.IOException e) {
                Log.e("LoadMeAsyncTask", "doInBackground", e);
            } catch (JSONException e) {
                Log.e("LoadMeAsyncTask", "doInBackground", e);
            }

            return result;
        }

        @Override
        protected void onPostExecute(MeDTO meDTO) {
            SharedPreferences sharedPref = sharedPref = mContext.getSharedPreferences("private", Context.MODE_PRIVATE);
            if (meDTO == null) {
                sharedPref.edit().putString("code", null).commit();
            } else {
                sharedPref.edit().putString("username", meDTO.getName()).commit();
                Intent intent = new Intent(mContext, MeActivity.class);
                mContext.startActivity(intent);
            }
        }
    }
}
