package com.example.bluetoothsensor;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NutritionFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NutritionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NutritionFragment extends Fragment {
    // Arg keys
    private static final String ARG_YEAR  = "YEAR";
    private static final String ARG_MONTH = "MONTH";
    private static final String ARG_DAY   = "DAY";
    private static final String ARG_HOUR  = "HOUR";

    public NutritionAdapter adapter;

    public void notifyDataSetChanged() {adapter.notifyDataSetChanged();}

    // Args
    private int year,month,day,hour;

    private OnFragmentInteractionListener mListener;

    public NutritionFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NutritionFragment.
     */
    public static NutritionFragment newInstance(int year, int month, int day, int hour) {
        NutritionFragment fragment = new NutritionFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_YEAR, year);
        args.putInt(ARG_MONTH, month);
        args.putInt(ARG_DAY, day);
        args.putInt(ARG_HOUR, hour);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.year = getArguments().getInt(ARG_YEAR);
            this.month = getArguments().getInt(ARG_MONTH);
            this.day = getArguments().getInt(ARG_DAY);
            this.hour = getArguments().getInt(ARG_HOUR);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View parent = inflater.inflate(R.layout.fragment_nutrition, container, false);
        if (this.day == -1) {
            // Monthly
            ((TextView) parent.findViewById(R.id.nutrition_header)).setText("Monthly Nutrition");
        }

        final FoodData data = FoodData.getInstance(getActivity());
        List<FoodDatum> dlist;
        try {
            dlist = data.getAll(hour, month, day, year);
        } catch (NullPointerException e) {
            dlist = new LinkedList<>();
        }


        adapter = new NutritionAdapter(inflater.getContext(), dlist);
        ListView list  = (ListView) parent.findViewById(R.id.nutrition_list_view);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i > 0) {
                    NutritionAdapter adapter = (NutritionAdapter) adapterView.getAdapter();
                    FoodDatum datum = (FoodDatum) adapter.getItem(i);

                    AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
                    b.setTitle("Food entry")
                            .setMessage(datum.description)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            });

                    b.create().show();
                }
            }
        });
        return parent;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public class NutritionAdapter extends BaseAdapter {

        private List<FoodDatum> data;
        private Context context;
        private LayoutInflater mInflater;

        public void setData(List<FoodDatum> d) { this.data = d; }

        public void addDatum(FoodDatum d) { data.add(d); }

        public NutritionAdapter(Context context, List<FoodDatum> data) {
            this.data = data;
            Collections.reverse(this.data);
            this.data.add(new FoodDatum("0","0","0","0","0","1:1","1/1/1"));
            Collections.reverse(this.data);
            this.context = context;
            this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            Log.e("NutritionFragment", "Size is " + Integer.toString(this.data.size()));
            return this.data.size();
        }

        @Override
        public Object getItem(int i) {
            return this.data.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View item = mInflater.inflate(R.layout.list_item_nutrition, viewGroup, false);
            if (i == 0 ){
                // Title
                ((TextView) item.findViewById(R.id.food_name)).setText("Food");
                ((TextView) item.findViewById(R.id.energy)).setText("Cal");
                ((TextView) item.findViewById(R.id.fat)).setText("Fat");
                ((TextView) item.findViewById(R.id.CHO)).setText("CHO");
                ((TextView) item.findViewById(R.id.protein)).setText("Protein");
                ((TextView) item.findViewById(R.id.time)).setText("Time");

            } else {
//            food_name,energy,fat,CHO,protein,time,date
                try {
                    ((TextView) item.findViewById(R.id.food_name)).setText(((FoodDatum) getItem(i)).description.substring(0, 20) + "...");
                } catch (StringIndexOutOfBoundsException e) {
                    ((TextView) item.findViewById(R.id.food_name)).setText(((FoodDatum) getItem(i)).description);
                }
                ((TextView) item.findViewById(R.id.energy)).setText(Integer.toString(Math.round(((FoodDatum) getItem(i)).energy)));
                ((TextView) item.findViewById(R.id.fat)).setText(Integer.toString(Math.round(((FoodDatum) getItem(i)).fat)));
                ((TextView) item.findViewById(R.id.CHO)).setText(Integer.toString(Math.round(((FoodDatum) getItem(i)).CHO)));
                ((TextView) item.findViewById(R.id.protein)).setText(Integer.toString(Math.round(((FoodDatum) getItem(i)).protein)));
                ((TextView) item.findViewById(R.id.time)).setText(((FoodDatum) getItem(i)).getTime());
            }

            return item;
        }
    }
}
