package com.yeolabgt.mahmoodms.ssvepinterfacetf

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceManager

/**
 * Created by mmahmood31 on 11/2/2017.
 *
 */

class PreferencesFragment : PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.prefs)
    }

    companion object {
        //Key Association:
        private var SETTINGS_SAVE_TIMESTAMPS = "timestamps"
        private var SETTINGS_SAVE_CLASS = "save_class"
        private var SETTINGS_BIT_PRECISION = "bit_precision"
        private var SETTINGS_FILTER_DATA = "filterData"

        fun saveTimestamps(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SETTINGS_SAVE_TIMESTAMPS, false)
        }

        fun saveClass(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SETTINGS_SAVE_CLASS, true)
        }

        fun setFilterData(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SETTINGS_FILTER_DATA, false)
        }

        fun setBitPrecision(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SETTINGS_BIT_PRECISION, true)
        }

        fun setNumberChannelsEnabled(context: Context): Int {
            return PreferenceManager.getDefaultSharedPreferences(context).getString("channel_list_preference", "4").toInt()
        }

        fun setSampleRate(context: Context): Int {
            return PreferenceManager.getDefaultSharedPreferences(context).getString("sample_rate_list_preference", "1").toInt()
        }

        fun setGainCh12(context: Context) : Int {
            return PreferenceManager.getDefaultSharedPreferences(context).getString("gain_list_preference", "1").toInt()
        }

        fun setSRB1(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("switch_srb1", false)
        }
    }
}
