package com.ilsecondodasinistra.catchit;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by marco on 3/18/14.
 */
public class TestFragment extends Fragment {
    public static final String EXTRA_MESSAGE = "EXTRA_MESSAGE";

    public static final TestFragment newInstance(String message)
    {
        TestFragment f = new TestFragment ();
        Bundle bdl = new Bundle(1);
        bdl.putString(EXTRA_MESSAGE, message);
        f.setArguments(bdl);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String message = getArguments().getString(EXTRA_MESSAGE);
        message = "Ciao";
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        TextView messageTextView;
//        messageTextView = (TextView)v.findViewById(R.id.textview);
//        messageTextView.setText(message);

        return v;
    }
}
