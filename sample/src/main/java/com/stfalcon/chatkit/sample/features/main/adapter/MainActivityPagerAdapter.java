package com.stfalcon.chatkit.sample.features.main.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.stfalcon.chatkit.sample.R;

/*
 * Created by troy379 on 11.04.17.
 */
public class MainActivityPagerAdapter extends FragmentStatePagerAdapter {

    public static final int ID_DEFAULT = 0;
    public static final int ID_STYLED = 1;

    private Context context;

    public MainActivityPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        String title = null;
        String description = null;
        switch (position) {
            case ID_DEFAULT:
                title = context.getString(R.string.fragment_chat);
                description = context.getString(R.string.fragment_chat_discrip);
                break;
            case ID_STYLED:
                title = context.getString(R.string.fragment_chat_group);
                description = context.getString(R.string.fragment_chat_group_discrip);
                break;
        }
        return DemoCardFragment.newInstance(position, title, description);
    }

    @Override
    public int getCount() {
        return 2;
    }
}