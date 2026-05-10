package com.alex.geiger;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ScreenSlidePagerAdapter extends FragmentStateAdapter {

    private final GeigerFragment geigerFragment;
    private final MapFragment mapFragment;

    public ScreenSlidePagerAdapter(@NonNull FragmentActivity fa,
                                   GeigerFragment geigerFragment,
                                   MapFragment mapFragment) {
        super(fa);
        this.geigerFragment = geigerFragment;
        this.mapFragment = mapFragment;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return geigerFragment;
        return mapFragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}