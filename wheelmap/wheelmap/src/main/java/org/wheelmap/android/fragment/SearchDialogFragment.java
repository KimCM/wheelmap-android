/*
 * #%L
 * Wheelmap - App
 * %%
 * Copyright (C) 2011 - 2012 Michal Harakal - Michael Kroez - Sozialhelden e.V.
 * %%
 * Wheelmap App based on the Wheelmap Service by Sozialhelden e.V.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wheelmap.android.fragment;

import java.util.ArrayList;

import org.wheelmap.android.adapter.TypesAdapter;
import org.wheelmap.android.model.CategoryOrNodeType;
import org.wheelmap.android.model.Extra;
import org.wheelmap.android.online.R;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.WazaBe.HoloEverywhere.HoloAlertDialogBuilder;
import com.actionbarsherlock.app.SherlockDialogFragment;

import de.akquinet.android.androlog.Log;

public class SearchDialogFragment extends SherlockDialogFragment implements
		OnItemSelectedListener, OnClickListener, OnEditorActionListener {
	public final static String TAG = SearchDialogFragment.class.getSimpleName();

	public final static int PERFORM_SEARCH = 1;

	private EditText mKeywordText;

	private int mCategorySelected = Extra.UNKNOWN;
	private int mNodeTypeSelected = Extra.UNKNOWN;
	private float mDistance = Extra.UNKNOWN;
	private boolean mEnableBoundingBoxSearch = false;

	public interface OnSearchDialogListener {
		public void onSearch(Bundle bundle);
	}

	public final static SearchDialogFragment newInstance(boolean showDistance,
			boolean showMapHint) {
		SearchDialogFragment dialog = new SearchDialogFragment();
		Bundle b = new Bundle();

		b.putBoolean(Extra.SHOW_DISTANCE, showDistance);
		b.putBoolean(Extra.SHOW_MAP_HINT, showMapHint);
		dialog.setArguments(b);
		return dialog;

	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		HoloAlertDialogBuilder builder = new HoloAlertDialogBuilder(
				getActivity());
		builder.setTitle(R.string.title_search);
		builder.setIcon(R.drawable.ic_menu_search_wm_holo_light);
		builder.setNeutralButton(R.string.search_execute, this);

		View view = createView();
		builder.setView(view);
		bindViews(view);

		Dialog d = builder.create();
		return d;
	}

	protected View createView() {
		return LayoutInflater.from(getActivity()).inflate(
				R.layout.fragment_dialog_search, null);
	}

	protected void bindViews(View v) {

		mKeywordText = (EditText) v.findViewById(R.id.search_keyword);
		mKeywordText.setOnEditorActionListener(this);

		Spinner categorySpinner = (Spinner) v
				.findViewById(R.id.search_spinner_categorie_nodetype);

		ArrayList<CategoryOrNodeType> searchTypes = CategoryOrNodeType
				.createTypesList(getActivity(), true);
		categorySpinner.setAdapter(new TypesAdapter(getActivity(), searchTypes,
				TypesAdapter.SEARCH_MODE));
		categorySpinner.setOnItemSelectedListener(this);

		Spinner distanceSpinner = (Spinner) v
				.findViewById(R.id.search_spinner_distance);

		ArrayAdapter<CharSequence> distanceSpinnerAdapter = ArrayAdapter
				.createFromResource(getActivity(), R.array.distance_array,
						android.R.layout.simple_spinner_item);
		distanceSpinnerAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		distanceSpinner.setAdapter(distanceSpinnerAdapter);
		distanceSpinner.setOnItemSelectedListener(this);
		distanceSpinner.setPromptId(R.string.search_distance);
		int initialPosition = 3;
		distanceSpinner.setSelection(initialPosition);
		String distance = (String) distanceSpinner
				.getItemAtPosition(initialPosition);
		try {
			mDistance = Float.valueOf(distance);
		} catch (NumberFormatException e) {
			mDistance = Extra.UNKNOWN;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		enableContainerVisibility();
	}

	private View mapHintContainer;
	private View distanceContainer;

	protected void enableContainerVisibility() {
		mapHintContainer = getDialog().findViewById(R.id.search_map_hint);
		if (getArguments().getBoolean(Extra.SHOW_MAP_HINT))
			mapHintContainer.setVisibility(View.VISIBLE);

		distanceContainer = getDialog().findViewById(
				R.id.search_spinner_distance_container);
		if (getArguments().getBoolean(Extra.SHOW_DISTANCE))
			distanceContainer.setVisibility(View.VISIBLE);
	}

	protected void setSearchMode(boolean enableBoundingBoxSearch) {
		Log.d(TAG, "enableBoundingBoxSearch = " + enableBoundingBoxSearch);
		mEnableBoundingBoxSearch = enableBoundingBoxSearch;
		mapHintContainer.setEnabled(mEnableBoundingBoxSearch);
		distanceContainer.setEnabled(!mEnableBoundingBoxSearch);
	}

	public void onItemSelected(AdapterView<?> adapterView, View view,
			int position, long id) {
		int viewId = adapterView.getId();

		switch (viewId) {
		case R.id.search_spinner_categorie_nodetype: {
			CategoryOrNodeType search = (CategoryOrNodeType) adapterView
					.getAdapter().getItem(position);
			switch (search.type) {
			case CATEGORY:
				mCategorySelected = search.id;
				break;
			case NODETYPE:
				mNodeTypeSelected = search.id;
				break;
			default:
				// noop
			}
			break;
		}
		case R.id.search_spinner_distance: {
			String distance = (String) adapterView.getItemAtPosition(position);
			try {
				mDistance = Float.valueOf(distance);
			} catch (NumberFormatException e) {
				mDistance = Extra.UNKNOWN;
			}
			break;
		}
		default:
			// noop
		}

	}

	public void onNothingSelected(AdapterView<?> parent) {

	}

	private void sendSearchInstructions() {
		Bundle b = createSearchBundle();

		OnSearchDialogListener listener = (OnSearchDialogListener) getTargetFragment();
		listener.onSearch(b);
	}

	protected Bundle createSearchBundle() {
		Bundle bundle = new Bundle();

		String keyword = mKeywordText.getText().toString();
		if (keyword.length() > 0)
			bundle.putString(SearchManager.QUERY, keyword);

		Log.d(TAG, "mCategory = " + mCategorySelected + " mNodeType = "
				+ mNodeTypeSelected);
		if (mCategorySelected != Extra.UNKNOWN)
			bundle.putInt(Extra.CATEGORY, mCategorySelected);
		else if (mNodeTypeSelected != Extra.UNKNOWN)
			bundle.putInt(Extra.NODETYPE, mNodeTypeSelected);
		else
			bundle.putInt(Extra.CATEGORY, Extra.UNKNOWN);

		if (mEnableBoundingBoxSearch)
			bundle.putBoolean(Extra.ENABLE_BOUNDING_BOX, true);
		else if (mDistance != Extra.UNKNOWN)
			bundle.putFloat(Extra.DISTANCE_LIMIT, mDistance);

		return bundle;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		sendSearchInstructions();
		dismiss();
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (EditorInfo.IME_ACTION_DONE == actionId) {
			sendSearchInstructions();
			dismiss();
			return true;
		}

		return false;
	}

}
