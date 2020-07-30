package com.firstdecision.tracker.screens.tracker_settings;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TrackerViewModel extends ViewModel {
    private MutableLiveData<String> serialNumber;

    public MutableLiveData<String> getSerialNumber() {
        if (serialNumber == null)
            serialNumber = new MutableLiveData<String>();
        return serialNumber;
    }
}
