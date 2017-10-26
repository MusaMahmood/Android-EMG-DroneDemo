package com.yeolabgt.mahmoodms.emgdronedemo;

import android.util.Log;

import com.google.common.primitives.Doubles;

import java.util.List;

/**
 * Created by mmahmood31 on 9/12/2017.
 * This class exists to process the read strings from a file in order to handle and produce the
 * featureset that will be used with the classifier.
 */

class ClassDataAnalysis {
    private static final String TAG = ClassDataAnalysis.class.getSimpleName();
    private static double rawData[][];
    private static final int COLUMNS = 2;
    boolean ERROR = false;

    ClassDataAnalysis(List<String[]> strings, int length) {
        int stringSize = strings.size();
        rawData = new double[COLUMNS][length];
        //Check rawData integrity first:
        if(strings.size() >= length) {
            for (int i = 0; i < length; i++) {
                if(strings.get(i).length!=COLUMNS) {
                    Log.e(TAG, "ERROR! - INCORRECT LENGTH("+ String.valueOf(strings.get(i).length)+")!; BREAK @ [ " + String.valueOf(i) + "/" + String.valueOf(stringSize) + "]");
                    return;
                }
            }
            for (int i = 0; i < length; i++) {
                for (int j = 0; j < COLUMNS; j++) { //add to array
                    if (strings.get(i).length==COLUMNS)
                        rawData[j][i] = Double.parseDouble(strings.get(i)[j]);
                }
            }
            Log.e(TAG,"Final Len: ["+ String.valueOf(rawData[0].length)+"]");
            this.ERROR = false;
        } else {
            Log.e(TAG, "ERROR! - Not enough rawData. ");
            this.ERROR = true;
        }
    }

    static double[] concatAll() {
        if(rawData[0]!=null) return Doubles.concat(rawData[0], rawData[1]);
        else return null;
    }

}
