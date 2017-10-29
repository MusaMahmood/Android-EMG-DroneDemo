package com.yeolabgt.mahmoodms.emgdronedemo;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;

/**
 * Created by Musa Mahmood on 5/15/2017.
 * This file controls the AndroidPlot graph.
 */

class GraphAdapter {
    // Variables
    SimpleXYSeries series;
    LineAndPointFormatter lineAndPointFormatter;
    private int seriesHistoryDataPoints;
//    private int bufferSize;
//    double[] classificationBuffer;
    int sampleRate;
    private double xAxisIncrement;
    boolean plotData;

    // Set/Get Methods (Don't need yet)
    void setPlotData(boolean plotData) {
        this.plotData = plotData;
        if (!plotData) {
            clearPlot();
        }
    }

    // Constructor
    GraphAdapter(int seriesHistoryDataPoints, String XYSeriesTitle, boolean useImplicitXVals, int lineAndPointFormatterColor) {
        //default values
        this.seriesHistoryDataPoints = seriesHistoryDataPoints;
        this.lineAndPointFormatter = new LineAndPointFormatter(lineAndPointFormatterColor, null, null, null);
        setPointWidth(5); //Def value:
//        this.bufferSize = classificationBufferSize;
//        this.classificationBuffer = new double[classificationBufferSize];
        this.series = new SimpleXYSeries(XYSeriesTitle);
        if(useImplicitXVals) this.series.useImplicitXVals();
        this.plotData = false; //Don't plot data until explicitly told to do so:
    }

    void setPointWidth(float width) {
        this.lineAndPointFormatter.getLinePaint().setStrokeWidth(width);
    }

    void setSeriesHistoryDataPoints(int seriesHistoryDataPoints) {
        this.seriesHistoryDataPoints = seriesHistoryDataPoints;
    }

    void addDataPointsGeneric(double[] xdata, double[] ydata, int istart, int iend) {
        if(this.plotData) {
            for (int i = istart; i < iend; i++) {
                plot(xdata[i],ydata[i]);
            }
        }
    }

    void addDataPoint(double data, int index) {
        if(this.plotData) plot((double)index*xAxisIncrement,data);
    }

    void setxAxisIncrementFromSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        setxAxisIncrement((double)1/(double)sampleRate);
    }

    private void setxAxisIncrement(double xAxisIncrement) {
        this.xAxisIncrement = xAxisIncrement;
    }

    //Graph Stuff:
    private void clearPlot() {
        if(this.series!=null) {
            DeviceControlActivity.redrawer.pause();
            while(this.series.size()>0) {
                this.series.removeFirst();
            }
            DeviceControlActivity.redrawer.start();
        }
    }

    private void plot(double x, double y) {
        if(series.size()>seriesHistoryDataPoints-1) {
            series.removeFirst();
        }
        series.addLast(x,y);
    }
}
