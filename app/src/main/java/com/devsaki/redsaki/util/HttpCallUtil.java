package com.devsaki.redsaki.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import com.devsaki.redsaki.R;
import com.devsaki.redsaki.dto.MeDTO;
import com.devsaki.redsaki.dto.PostDTO;
import com.devsaki.redsaki.dto.SubredditDTO;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpCallUtil {

    private static final String API_REST_URL = "api.reddit.com";
    private static final String OAUTH_API_REST_URL = "oauth.reddit.com";

    public static List<PostDTO> list(Context context, String subreddit, String sort) throws IOException, JSONException {
        if (subreddit == null)
            return callList(context, new String[]{sort}, null);
        else
            return callList(context, new String[]{"r", subreddit, sort}, null);
    }

    public static List<PostDTO> listUserCategory(Context context, String user, String category) throws IOException, JSONException {
        return callList(context, new String[]{"user", user, category}, null);
    }

    public static List<PostDTO> searchPosts(Context context, String query, String sort, String filter) throws IOException, JSONException {
        Map<String, String> queryParam = new HashMap<>();
        queryParam.put("q", query);
        queryParam.put("sort", sort);
        queryParam.put("t", filter);
        return callList(context, new String[]{"search"}, queryParam);
    }

    @NonNull
    private static List<PostDTO> callList(Context context, String[] paths, Map<String, String> queryParams) throws IOException, JSONException {
        List<PostDTO> result = new ArrayList<>(30);
        StringBuilder sb = new StringBuilder();

        String code = retrieveToken(context);

        String rest_url;
        if (code == null) {
            rest_url = API_REST_URL;
        } else {
            rest_url = OAUTH_API_REST_URL;
        }
        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority(rest_url);

        if (paths != null) {
            for (String path : paths) {
                if (path != null && !path.isEmpty())
                    builder.appendPath(path);
            }
        }

        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (entry.getValue() != null) {
                    builder.appendQueryParameter(entry.getKey(), entry.getValue());
                }
            }
        }

        URL url = new URL(builder.build().toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (code != null)
            conn.setRequestProperty("Authorization", "bearer " + code);
        conn.setRequestMethod("GET");

        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();

        JSONObject root = new JSONObject(sb.toString());

        JSONArray children = root.getJSONObject("data").getJSONArray("children");
        for (int i = 0; i < children.length(); i++) {
            JSONObject item = children.getJSONObject(i).getJSONObject("data");
            PostDTO post = new PostDTO();
            post.setId(item.getString("name"));
            post.setTitle(item.getString("title"));
            post.setAuthor(item.getString("author"));
            post.setDomain(item.getString("domain"));
//            post.setQtyUp(item.getInt("ups"));
//            post.setQtyDown(item.getInt("downs"));
            post.setCreated(item.getInt("created_utc"));
//            post.setQtyComments(item.getInt("num_comments"));
            post.setThumbnail(item.getString("thumbnail"));
            post.setSubreddit(item.getString("subreddit"));
            post.setPermanlink(item.getString("permalink"));
            post.setUrl(StringEscapeUtils.unescapeHtml4((item.getString("url"))));
            post.setSaved(item.getBoolean("saved"));
            post.setVisited(item.getBoolean("visited"));
            post.setUp(item.optBoolean("likes"));
            post.setDown(!item.get("likes").toString().equals("null") && !item.optBoolean("likes"));

            result.add(post);
        }
        return result;
    }

    private static String retrieveToken(Context context) throws IOException {
        SharedPreferences sharedPref = context.getSharedPreferences("private", Context.MODE_PRIVATE);
        String code = sharedPref.getString("access_code", null);
        if (code == null)
            return null;


        try {
            long tokenDuration = sharedPref.getLong("token_duration", -1);
            if (tokenDuration != -1) {
                if (tokenDuration > System.currentTimeMillis())
                    return sharedPref.getString("last_token", null);
            }

            String refreshToken = sharedPref.getString("refresh_token", null);

            Uri.Builder builder = new Uri.Builder()
                    .scheme("https")
                    .authority("www.reddit.com")
                    .appendPath("api")
                    .appendPath("v1")
                    .appendPath("access_token");

            if (refreshToken == null) {
                builder.appendQueryParameter("grant_type", "authorization_code")
                        .appendQueryParameter("redirect_uri", "redsaki://oauth")
                        .appendQueryParameter("code", code);
            } else {
                builder.appendQueryParameter("grant_type", "refresh_token")
                        .appendQueryParameter("refresh_token", refreshToken);
            }

            URL url = new URL(builder.build().toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");

            String authorization = "basic " + Base64.encodeToString((context.getString(R.string.client_id) + ":").getBytes(), Base64.NO_WRAP);
            conn.setRequestProperty("Authorization", authorization);

            StringBuilder sb = new StringBuilder();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();


            JSONObject root = new JSONObject(sb.toString());
            String accessToken = root.getString("access_token");

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong("token_duration", System.currentTimeMillis() + 3600 * 1000);
            editor.putString("last_token", accessToken);
            if (refreshToken == null) {
                editor.putString("refresh_token", root.getString("refresh_token"));
            }
            editor.commit();

            return accessToken;
        } catch (Exception ex) {
            Log.e("HttpCallUtil", "retrieveToken", ex);
        }


        return null;
    }

    public static List<SubredditDTO> listSubreddits(Context context) throws IOException, JSONException {
        List<SubredditDTO> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        String code = retrieveToken(context);

        String rest_url;
        if (code == null) {
            rest_url = API_REST_URL;
        } else {
            rest_url = OAUTH_API_REST_URL;
        }

        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority(rest_url)
                .appendPath("subreddits");

        if (code == null) {
            builder.appendPath("default");
        } else {
            builder.appendPath("mine");
        }

        String after = null;

        String baseUrl = builder.build().toString();
        do {
            URL url = new URL(baseUrl + "?after=" + after);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (code != null)
                conn.setRequestProperty("Authorization", "bearer " + code);
            conn.setRequestMethod("GET");

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();

            JSONObject root = new JSONObject(sb.toString());

            JSONArray children = root.getJSONObject("data").getJSONArray("children");
            for (int i = 0; i < children.length(); i++) {
                JSONObject item = children.getJSONObject(i).getJSONObject("data");
                SubredditDTO dto = new SubredditDTO();
                dto.setDisplayName(item.getString("display_name"));
                dto.setUrl(item.getString("url"));
                dto.setSuscriber(item.optBoolean("user_is_subscriber"));
                dto.setPublicDescription(item.getString("public_description"));
                dto.setId(item.getString("name"));
                result.add(dto);
            }
        } while (after != null);
        return result;
    }

    public static List<SubredditDTO> listSearchSubreddits(Context context, String query) throws IOException, JSONException {
        List<SubredditDTO> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        String code = retrieveToken(context);

        String rest_url;
        if (code == null) {
            rest_url = API_REST_URL;
        } else {
            rest_url = OAUTH_API_REST_URL;
        }

        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority(rest_url)
                .appendPath("subreddits").appendPath("search");

        builder.appendQueryParameter("q", query);
        String baseUrl = builder.build().toString();
        URL url = new URL(baseUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (code != null)
            conn.setRequestProperty("Authorization", "bearer " + code);
        conn.setRequestMethod("GET");

        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();

        JSONObject root = new JSONObject(sb.toString());

        JSONArray children = root.getJSONObject("data").getJSONArray("children");
        for (int i = 0; i < children.length(); i++) {
            JSONObject item = children.getJSONObject(i).getJSONObject("data");
            SubredditDTO dto = new SubredditDTO();
            dto.setDisplayName(item.getString("display_name"));
            dto.setUrl(item.getString("url"));
            dto.setSuscriber(item.optBoolean("user_is_subscriber"));
            dto.setPublicDescription(item.getString("public_description"));
            dto.setId(item.getString("name"));
            result.add(dto);
        }
        return result;
    }

    public static void suscribe(Context context, String id, boolean suscribed) throws IOException, JSONException {
        StringBuilder sb = new StringBuilder();

        String code = retrieveToken(context);

        String rest_url;
        if (code == null) {
            rest_url = API_REST_URL;
        } else {
            rest_url = OAUTH_API_REST_URL;
        }
        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority(rest_url).appendPath("api").appendPath("subscribe");

        if (!suscribed) {
            builder.appendQueryParameter("action", "sub");
        } else {
            builder.appendQueryParameter("action", "unsub");
        }

        builder.appendQueryParameter("sr", id);

        URL url = new URL(builder.build().toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (code != null)
            conn.setRequestProperty("Authorization", "bearer " + code);
        conn.setRequestMethod("POST");

        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
    }

    public static void savedPost(Context context, String id, boolean save) throws IOException, JSONException {
        StringBuilder sb = new StringBuilder();

        String code = retrieveToken(context);

        String rest_url;
        if (code == null) {
            rest_url = API_REST_URL;
        } else {
            rest_url = OAUTH_API_REST_URL;
        }
        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority(rest_url).appendPath("api");

        if (save) {
            builder.appendPath("save");
        } else {
            builder.appendPath("unsave");
        }

        builder.appendQueryParameter("id", id);

        URL url = new URL(builder.build().toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (code != null)
            conn.setRequestProperty("Authorization", "bearer " + code);
        conn.setRequestMethod("POST");

        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
    }

    public static void votePost(Context context, String id, int vote) throws IOException, JSONException {
        StringBuilder sb = new StringBuilder();

        String code = retrieveToken(context);

        String rest_url;
        if (code == null) {
            rest_url = API_REST_URL;
        } else {
            rest_url = OAUTH_API_REST_URL;
        }
        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority(rest_url)
                .appendPath("api").appendPath("vote")
                .appendQueryParameter("id", id)
                .appendQueryParameter("dir", vote + "");

        URL url = new URL(builder.build().toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (code != null)
            conn.setRequestProperty("Authorization", "bearer " + code);
        conn.setRequestMethod("POST");

        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
    }

    public static MeDTO extractMe(Context context) throws IOException, JSONException {
        StringBuilder sb = new StringBuilder();

        String code = retrieveToken(context);

        String rest_url;
        if (code == null) {
            rest_url = API_REST_URL;
        } else {
            rest_url = OAUTH_API_REST_URL;
        }
        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority(rest_url)
                .appendPath("api").appendPath("v1").appendPath("me");

        URL url = new URL(builder.build().toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (code != null)
            conn.setRequestProperty("Authorization", "bearer " + code);
        conn.setRequestMethod("GET");

        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        JSONObject root = new JSONObject(sb.toString());

        MeDTO meDTO = new MeDTO();
        meDTO.setName(root.getString("name"));
        return meDTO;
    }
}
