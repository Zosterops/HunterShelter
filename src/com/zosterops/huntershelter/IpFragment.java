package com.zosterops.huntershelter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


/**
 */
public class IpFragment extends Fragment implements View.OnClickListener {
    private EditText editIp1,editIp2,editIp3,editIp4;

    private Button ok;

    private static final String IP1="ip1",IP2="ip2",IP3="ip3",IP4="ip4";


    public IpFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_ip, container, false);
        editIp1 = (EditText) v.findViewById(R.id.ip1);
        editIp2 = (EditText) v.findViewById(R.id.ip2);
        editIp3 = (EditText) v.findViewById(R.id.ip3);
        editIp4 = (EditText) v.findViewById(R.id.ip4);
        editIp4.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    IpFragment.this.onClick(ok);
                }
                return false;
            }
        });
        ok = (Button) v.findViewById(R.id.button);
        ok.setOnClickListener(this);

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        String ip1 = sharedPref.getString(IP1, "0");
        String ip2 = sharedPref.getString(IP2, "0");
        String ip3 = sharedPref.getString(IP3, "0");
        String ip4 = sharedPref.getString(IP4, "0");

        editIp1.setText(ip1);
        editIp2.setText(ip2);
        editIp3.setText(ip3);
        editIp4.setText(ip4);
        return v;

    }

    @Override
    public void onClick(View v) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IP1, editIp1.getText().toString());
        editor.putString(IP2, editIp2.getText().toString());
        editor.putString(IP3, editIp3.getText().toString());
        editor.putString(IP4, editIp4.getText().toString());
        editor.apply();
        ConfigActivty ca = (ConfigActivty) getActivity();
        ca.processIP(editIp1.getText().toString(), editIp2.getText().toString(), editIp3.getText().toString(), editIp4.getText().toString());

    }

}
