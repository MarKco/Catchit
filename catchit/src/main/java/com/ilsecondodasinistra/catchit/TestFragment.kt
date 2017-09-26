package com.ilsecondodasinistra.catchit

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * Created by marco on 3/18/14.
 */
class TestFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        var message: String = arguments.getString(EXTRA_MESSAGE)
        message = "Ciao"
        val v = inflater!!.inflate(R.layout.fragment_main, container, false)
        val messageTextView: TextView
        //        messageTextView = (TextView)v.findViewById(R.id.textview);
        //        messageTextView.setText(message);

        return v
    }

    companion object {
        val EXTRA_MESSAGE = "EXTRA_MESSAGE"

        fun newInstance(message: String): TestFragment {
            val f = TestFragment()
            val bdl = Bundle(1)
            bdl.putString(EXTRA_MESSAGE, message)
            f.arguments = bdl
            return f
        }
    }
}
