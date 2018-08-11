package com.yeolabgt.mahmoodms.emgdronedemo;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * This {@link Fragment} is shown while the presenter is connecting to the server
 * and exchanging initial information.
 */
public class Connecting extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_connecting, container, false);
    }
}
