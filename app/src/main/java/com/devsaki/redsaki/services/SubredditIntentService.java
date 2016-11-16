package com.devsaki.redsaki.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.TaskParams;

public class SubredditIntentService extends IntentService {

  public SubredditIntentService(){
    super(SubredditIntentService.class.getName());
  }

  public SubredditIntentService(String name) {
    super(name);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.d(SubredditIntentService.class.getSimpleName(), "PostIntentService");
    SubredditTaskService subredditTaskService = new SubredditTaskService(this);
    Bundle args = new Bundle();

    args.putString("query", intent.getStringExtra("query"));
    subredditTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args));
  }
}
