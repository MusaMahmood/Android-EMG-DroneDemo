/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *  Presenter. Android Client to remote control a presentation.          *
 *  Copyright (C) 2017 Felix Wohlfrom                                    *
 *                                                                       *
 *  This program is free software: you can redistribute it and/or modify *
 *  it under the terms of the GNU General Public License as published by *
 *  the Free Software Foundation, either version 3 of the License, or    *
 *  (at your option) any later version.                                  *
 *                                                                       *
 *  This program is distributed in the hope that it will be useful,      *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of       *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        *
 *  GNU General Public License for more details.                         *
 *                                                                       *
 *  You should have received a copy of the GNU General Public License    *
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.*
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package com.yeolabgt.mahmoodms.emgdronedemo;

import android.app.Fragment;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import static android.content.Context.AUDIO_SERVICE;

/**
 * This fragment displays the main presenter. It has two buttons, one that shows the next slide
 * and one that shows the previous slides.
 */
public class Presenter extends Fragment {
    /**
     * Audio management
     */
    private Settings mSettings;
    private AudioManager mAudioManager;
    private int mPreviousAudioRingerMode;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHideSystemUi = new Runnable() {
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    /**
     * Listener interface for presenter events.
     */
    public interface PresenterListener {
        /**
         * Called if the server should switch to the previous slide.
         */
        void onPrevSlide();

        /**
         * Called if the server should switch to the next slide.
         */
        void onNextSlide();

        void endPresentation();

        void beginPresentation();

    }

    /**
     * The listener for presenter events.
     */
    private PresenterListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_presenter, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (PresenterListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement PresenterListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        Button nextSlideButton = (Button) getActivity().findViewById(R.id.next_slide);
        nextSlideButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mListener.onNextSlide();
            }
        });

        Button prevSlideButton = (Button) getActivity().findViewById(R.id.prev_slide);
        prevSlideButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mListener.onPrevSlide();
            }
        });

        Button endPresentationButton = (Button) getActivity().findViewById(R.id.end_pres_button);
        endPresentationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.endPresentation();
            }
        });

        Button startPresentationButton = (Button) getActivity().findViewById(R.id.start_pres_button);
        startPresentationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.beginPresentation();
            }
        });

        // Hide the system ui with a short delay
        mContentView = getActivity().findViewById(R.id.presenter);
        mHideHandler.postDelayed(mHideSystemUi, UI_ANIMATION_DELAY);

        mSettings = new Settings(getActivity());

        // Silence audio during presentation. Store old audio state to reset it afterwards.
        if (mSettings.silenceDuringPresentation()) {
            mAudioManager = (AudioManager)getActivity().getSystemService(AUDIO_SERVICE);
            if (mAudioManager != null) {
                mPreviousAudioRingerMode = mAudioManager.getRingerMode();

                mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mSettings.silenceDuringPresentation()) {
            mAudioManager.setRingerMode(mPreviousAudioRingerMode);
        }
    }
}
