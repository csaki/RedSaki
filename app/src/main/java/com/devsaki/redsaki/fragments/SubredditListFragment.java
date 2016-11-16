package com.devsaki.redsaki.fragments;

import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.devsaki.redsaki.R;
import com.devsaki.redsaki.adapter.SubredditAdapter;
import com.devsaki.redsaki.dto.SubredditDTO;
import com.devsaki.redsaki.util.RefresherActivity;

import java.util.ArrayList;
import java.util.List;

public class SubredditListFragment extends Fragment {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private View mRootView;
    private RecyclerView recyclerView;
    private SubredditAdapter subredditAdapter;
    private ArrayList<SubredditDTO> list;
    RefresherActivity refresherActivity;

    public static SubredditListFragment newInstance(RefresherActivity refresherActivity) {
        SubredditListFragment fragment = new SubredditListFragment();
        fragment.setRefresherActivity(refresherActivity);
        return fragment;
    }

    public void setRefresherActivity(RefresherActivity refresherActivity) {
        this.refresherActivity = refresherActivity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_list, container, false);
        recyclerView = (RecyclerView) mRootView.findViewById(R.id.rv_list_posts);

        list = new ArrayList<>();
        subredditAdapter = new SubredditAdapter(list, getActivity());
        recyclerView.setAdapter(subredditAdapter);
        if(getResources().getBoolean(R.bool.isTablet)) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
            }else{
                recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
            }
        }else{
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }

        recyclerView.setItemAnimator(new DefaultItemAnimator());

        mSwipeRefreshLayout = (SwipeRefreshLayout) mRootView.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresherActivity.refreshList();
            }
        });

        return mRootView;
    }

    public void refresh(List<SubredditDTO> subredditDTOs, boolean append) {
        if (!append) {
            list.clear();
        }
        list.addAll(subredditDTOs);
        subredditAdapter.notifyDataSetChanged();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    public void startLoad() {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(true);
            list.clear();
            subredditAdapter.notifyDataSetChanged();
        }
    }

}
