package com.firstdecision.tracker.screens.tracker_settings;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.firstdecision.tracker.R;
import com.firstdecision.tracker.databinding.FragmentTrackerBinding;
import com.firstdecision.tracker.utils.BackgroundService;
import com.firstdecision.tracker.utils.Common;
import com.firstdecision.tracker.utils.Device;
import com.firstdecision.tracker.utils.SendLocationToActivity;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.List;

public class TrackerFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    public TrackerFragment() {
    }

    private TrackerViewModel viewModel;
    private FragmentTrackerBinding binding;
    private BackgroundService mService = null;
    private boolean mBound = false;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BackgroundService.LocalBinder binder = (BackgroundService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        /* Configuração da ViewModel e fazendo o binding com o Layout */
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tracker, container, false);
        binding.setTrackerViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        viewModel = ViewModelProviders.of(this).get(TrackerViewModel.class);

        binding.tkEtSerial.setText(Device.getSerialNumber(this.getActivity().getApplicationContext()));

        Dexter.withActivity(getActivity())
                .withPermissions(Arrays.asList(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ))
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        binding.tkSwitchGps.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (binding.tkSwitchGps.isEnabled())
                                    mService.requestLocationUpdates();
                                else
                                    mService.removeLocationUpdates();
                            }
                        });
                        setButtonState(Common.requestingLocationUpdates(getActivity().getApplicationContext()));
                        getActivity().bindService(new Intent(getActivity(), BackgroundService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {}
                }).check();

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        PreferenceManager
                .getDefaultSharedPreferences(getActivity().getApplicationContext());
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        if (mBound) {
            getActivity().unbindService(mServiceConnection);
            mBound = false;
        }

        PreferenceManager
                .getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);

        EventBus.getDefault().unregister(this);


        super.onStop();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.tkBtnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavDirections action = TrackerFragmentDirections.actionTrackerFragmentToAboutFragment();
                NavHostFragment.findNavController(TrackerFragment.this).navigate(action);
            }
        });


        binding.tkEtSerial.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.getSerialNumber().setValue(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Common.KEY_REQUESTING_LOCATION_UPDATES)) {
            setButtonState(sharedPreferences.getBoolean(Common.KEY_REQUESTING_LOCATION_UPDATES, false));
        }
    }

    private void setButtonState(boolean value) {
        if (value) {
            binding.tkSwitchGps.setEnabled(false);
        } else {
            binding.tkSwitchGps.setEnabled(true);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onListenLocation(SendLocationToActivity event) {
        if (event != null) {
            String data = new StringBuilder()
                    .append(event.getLocation().getLatitude())
                    .append("/")
                    .append(event.getLocation().getLatitude())
                    .toString();

            Toast.makeText(mService, data, Toast.LENGTH_LONG).show();
        }
    }
}
