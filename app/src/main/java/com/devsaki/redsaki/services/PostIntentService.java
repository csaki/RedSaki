package com.devsaki.redsaki.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.TaskParams;

public class PostIntentService extends IntentService {

  public PostIntentService(){
    super(PostIntentService.class.getName());
  }

  public PostIntentService(String name) {
    super(name);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.d(PostIntentService.class.getSimpleName(), "PostIntentService");
    PostTaskService taskService = new PostTaskService(this);
    Bundle args = new Bundle();

    args.putString("sort", intent.getStringExtra("sort"));
    args.putString("subreddit", intent.getStringExtra("subreddit"));
    args.putString("query", intent.getStringExtra("query"));
    args.putString("filter", intent.getStringExtra("filter"));
    args.putString("category", intent.getStringExtra("category"));
    args.putString("user", intent.getStringExtra("user"));
    taskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args));
  }
}
