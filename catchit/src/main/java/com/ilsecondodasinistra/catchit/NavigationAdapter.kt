package com.ilsecondodasinistra.catchit

/**
 * Created by marco on 29/01/17.
 */

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by hp1 on 28-12-2014.
 */
class NavigationAdapter internal constructor(private val mNavTitles: Array<String> // String Array to store the passed titles Value from MainActivity.java
                                             , private val mIcons: IntArray       // Int Array to store the passed icons resource value from MainActivity.java
                                             , private val name: String        //String Resource for header View Name
                                             , private val email: String       //String Resource for header view email
                                             , private val profile: Int        //int Resource for header view profile picture
)// NavigationAdapter Constructor with titles and icons parameter
// titles, icons, name, email, profile pic are passed from the main activity as we
//have seen earlier
//here we assign those passed values to the values we declared here
//in adapter
    : RecyclerView.Adapter<NavigationAdapter.ViewHolder>() {


    // Creating a ViewHolder which extends the RecyclerView View Holder
    // ViewHolder are used to to store the inflated views in order to recycle them

    class ViewHolder(itemView: View, ViewType: Int) : RecyclerView.ViewHolder(itemView) {
        internal var Holderid: Int = 0

        internal lateinit var textView: TextView
        internal lateinit var imageView: ImageView
        internal lateinit var profile: ImageView
        internal lateinit var Name: TextView
        internal lateinit var email: TextView


        init {                 // Creating ViewHolder Constructor with View and viewType As a parameter


            // Here we set the appropriate view in accordance with the the view type as passed when the holder object is created

            if (ViewType == TYPE_ITEM) {
                textView = itemView.findViewById<View>(R.id.rowText) as TextView // Creating TextView object with the id of textView from item_row.xml
                imageView = itemView.findViewById<View>(R.id.rowIcon) as ImageView// Creating ImageView object with the id of ImageView from item_row.xml
                Holderid = 1                                               // setting holder id as 1 as the object being populated are of type item row
            } else {


                Name = itemView.findViewById<View>(R.id.name) as TextView         // Creating Text View object from header.xml for name
                email = itemView.findViewById<View>(R.id.email) as TextView       // Creating Text View object from header.xml for email
                profile = itemView.findViewById<View>(R.id.circleView) as ImageView// Creating Image view object from header.xml for profile pic
                Holderid = 0                                                // Setting holder id = 0 as the object being populated are of type header view
            }
        }


    }


    //Below first we ovverride the method onCreateViewHolder which is called when the ViewHolder is
    //Created, In this method we inflate the item_row.xml layout if the viewType is Type_ITEM or else we inflate header.xml
    // if the viewType is TYPE_HEADER
    // and pass it to the view holder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        if (viewType == TYPE_ITEM) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.navigation_drawer_item_row, parent, false) //Inflating the layout

            val vhItem = ViewHolder(v, viewType) //Creating ViewHolder and passing the object of type view

            return vhItem // Returning the created object

            //inflate your layout and pass it to view holder

        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.navigation_drawer_header, parent, false) //Inflating the layout

            val vhHeader = ViewHolder(v, viewType) //Creating ViewHolder and passing the object of type view

            return vhHeader //returning the object created
        }
    }

    //Next we override a method which is called when the item in a row is needed to be displayed, here the int position
    // Tells us item at which position is being constructed to be displayed and the holder id of the holder object tell us
    // which view type is being created 1 for item row
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder.Holderid == 1) {                              // as the list view is going to be called after the header view so we decrement the
            // position by 1 and pass it to the holder while setting the text and image
            holder.textView.text = mNavTitles[position - 1] // Setting the Text with the array of our Titles
            holder.imageView.setImageResource(mIcons[position - 1])// Settimg the image with array of our icons
        } else {

            holder.profile.setImageResource(profile)           // Similarly we set the resources for header view
            holder.Name.text = name
            holder.email.text = email
        }
    }

    // This method returns the number of items present in the list
    override fun getItemCount(): Int {
        return mNavTitles.size + 1 // the number of items in the list will be +1 the titles including the header view.
    }


    // Witht the following method we check what type of view is being passed
    override fun getItemViewType(position: Int): Int {
        if (isPositionHeader(position))
            return TYPE_HEADER

        return TYPE_ITEM
    }

    private fun isPositionHeader(position: Int): Boolean {
        return position == 0
    }

    companion object {

        private val TYPE_HEADER = 0  // Declaring Variable to Understand which View is being worked on
        // IF the view under inflation and population is header or Item
        private val TYPE_ITEM = 1
    }
}