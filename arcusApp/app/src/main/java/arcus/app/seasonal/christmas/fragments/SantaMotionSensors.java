/*
 *  Copyright 2019 Arcus Project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package arcus.app.seasonal.christmas.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import arcus.cornea.provider.DeviceModelProvider;
import arcus.cornea.utils.Listeners;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.DeviceModel;
import arcus.app.R;
import arcus.app.common.backstack.BackstackManager;
import arcus.app.common.view.Version1ButtonColor;
import arcus.app.seasonal.christmas.fragments.adapter.SantaListAdapter;
import arcus.app.seasonal.christmas.model.ChristmasModel;
import arcus.app.seasonal.christmas.model.SantaListItemModel;
import arcus.app.seasonal.christmas.model.SantaMotionSensor;
import arcus.app.common.view.Version1Button;
import arcus.app.common.view.Version1TextView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SantaMotionSensors extends BaseChristmasFragment {
    private ListenerRegistration devicesLoadedReg;
    private ListView santaListView;
    private final Listener<List<DeviceModel>> devicesLoadedListener =
          Listeners.runOnUiThread(new Listener<List<DeviceModel>>() {
              @Override
              public void onEvent(List<DeviceModel> deviceModels) {
                  devicesLoaded(deviceModels);
              }
          });

    public static SantaMotionSensors newInstance(ChristmasModel model) {
        SantaMotionSensors motionSensors = new SantaMotionSensors();

        Bundle bundle = new Bundle(1);
        bundle.putSerializable(MODEL, model);
        motionSensors.setArguments(bundle);

        return motionSensors;
    }

    public void onResume() {
        super.onResume();

        View rootView = getView();
        if (rootView == null) {
            return;
        }

        ImageView logo = (ImageView) rootView.findViewById(R.id.small_image);
        if (logo != null) {
            Picasso.with(getActivity()).load(R.drawable.icon_snowflake).into(logo);
        }

        Version1TextView logoText = (Version1TextView) rootView.findViewById(R.id.small_image_text);
        if (logoText != null) {
            logoText.setText(getString(R.string.santa_exit_enter_motion));
        }

        final ChristmasModel model = getDataModel();
        Version1Button nextButton = (Version1Button) rootView.findViewById(R.id.santa_next_button);
        if (nextButton != null) {
            if (model.isSetupComplete()) {
                nextButton.setVisibility(View.GONE);
            }
            else {
                nextButton.setColorScheme(Version1ButtonColor.WHITE);
                nextButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BackstackManager.getInstance()
                              .navigateToFragment(SantaPictureFragment.newInstance(model), true);
                    }
                });
            }
        }

        santaListView = (ListView) rootView.findViewById(R.id.santa_list_choice);
        devicesLoadedReg = DeviceModelProvider.instance().addStoreLoadListener(devicesLoadedListener);
        DeviceModelProvider.instance().load();
    }

    public void onPause() {
        super.onPause();
        Listeners.clear(devicesLoadedReg);
    }

    public void onDestroy() {
        super.onDestroy();
        Listeners.clear(devicesLoadedReg);
    }

    private void devicesLoaded(List<DeviceModel> deviceModels) {
        if (santaListView == null) {
            return;
        }

        santaListView.setDivider(null);
        String motionSensor = getString(R.string.santa_motion_sensor_default_description);
        List<SantaListItemModel> items = new ArrayList<>();

        final ChristmasModel christmasModel = getDataModel();
        final Set<String> selectedSensors = christmasModel.getMotionSensors();
        List<String> defaultSelectedSensors = new ArrayList<>();
        for (SantaMotionSensor sensor : SantaMotionSensor.values()) {
            String title = getString(sensor.getTitle());
            String description = getString(sensor.getDescription());
            items.add(new SantaListItemModel(title, description, sensor.getIcon(),
                        selectedSensors.isEmpty() || selectedSensors.contains(title)));
            defaultSelectedSensors.add(title);
        }

        for (DeviceModel model : deviceModels) {
            String typeHint = String.valueOf(model.getDevtypehint()).toLowerCase();
            if (model.getCaps() != null && typeHint.startsWith("motion")) {
                Joiner joiner = Joiner.on(" ");
                String modelType = Strings.isNullOrEmpty(model.getModel()) ? motionSensor : model.getModel();
                items.add(
                      new SantaListItemModel(
                            model.getName(),
                            joiner.join(modelType.split("(?=[A-Z])")).trim(),
                            model.getId(), getDeviceImageURL(model.getProductId()),
                            selectedSensors.contains(model.getId())
                      )
                );
            }
        }

        if (selectedSensors.isEmpty()) {
            selectedSensors.addAll(defaultSelectedSensors);
            christmasModel.setMotionSensors(selectedSensors);
            saveModelToFragment(christmasModel);
        }

        final SantaListAdapter santaListAdapter = new SantaListAdapter(getActivity(), items, true);
        santaListView.setAdapter(santaListAdapter);
        santaListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selection = santaListAdapter.getItem(position).getModelID();
                if (selectedSensors.size() == 1 && selectedSensors.contains(selection)) {
                    return; // Must have at least one....
                }

                if (santaListAdapter.toggleCheck(position)) {
                    selectedSensors.add(selection);
                }
                else {
                    selectedSensors.remove(selection);
                }

                christmasModel.setMotionSensors(selectedSensors);
                saveModelToFragment(christmasModel);
            }
        });
    }

    @Override
    public Integer getLayoutId() {
        return R.layout.santa_fragment_list_view;
    }
}
