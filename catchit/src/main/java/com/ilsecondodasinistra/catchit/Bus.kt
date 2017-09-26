package com.ilsecondodasinistra.catchit

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date

import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable

class Bus(var departure: Date?, lineNumber: String, var departureStop: String?, var arrivalStop: String?, var arrival: Date?) : Serializable {
    var line: String = ""
    var color: Int = 0
    var textColor: Int = 0
    var toBePutLast: Boolean = false
    private val dateFormatter = SimpleDateFormat("H:mm")
    private val dateFormatterForComparison = SimpleDateFormat("kk:mm")

    init {
        this.line = lineNumber

        this.textColor = Color.BLACK
        this.toBePutLast = false

        resetColors()
    }

    fun resetColors() {
        //		if(this.lineNumber.equals("12"))
        //		{
        //			//Rosso più scuro
        //			this.busColor = Color.argb(40, 178, 34, 34);
        //		}
        //
        //		if(this.lineNumber.equals("12/"))
        //		{
        //			//Rosso
        //			this.busColor = Color.argb(30, 255, 20, 147);
        //		}
        //
        //		if(this.lineNumber.equals("12L"))
        //		{
        //			//Rosso meno intenso
        //			this.busColor = Color.argb(40, 255, 69, 0);
        //		}
        //
        //		if(this.lineNumber.equals("SCORZE'"))
        //		{
        //			//Azzurrino
        //			this.busColor = Color.argb(50, 185, 211, 238);
        //		}
        //
        //		if(this.lineNumber.equals("NOALE"))
        //		{
        //			//Giallino
        //			this.busColor = Color.argb(30, 255, 255, 0);
        //		}

        if (this.line == "T1") {
            //Azzurrino
            this.color = Color.argb(100, 204, 255, 255)
        }

        if (this.line == "N1") {
            //azzurrino
            this.color = Color.parseColor("#33518aff")
        }

        if (this.line == "N2") {
            //Giallino
            this.color = Color.argb(30, 30, 255, 255)
        }

        this.textColor = Color.BLACK
    }

    fun setTextColor(a: Int, r: Int, g: Int, b: Int) {
        this.textColor = Color.argb(a, r, g, b)
    }

    fun setColor(color: String) {
        this.color = Color.parseColor(color)
    }

    fun setColor(a: Int, r: Int, g: Int, b: Int) {
        this.color = Color.argb(a, r, g, b)
    }

    fun resetTextColor() {
        this.textColor = Color.BLACK
    }

    val departureString: String
        get() = dateFormatter.format(this.departure)

    val arrivalString: String
        get() = dateFormatter.format(this.arrival)

    val departureStringForComparison: String
        get() = dateFormatterForComparison.format(this.departure)

    fun getLineNumber(): String {
        return line
    }

    fun setLineNumber(lineNumber: String) {
        this.line = lineNumber
    }

    override fun toString(): String {
        return "Line: " + this.line +
                " Time: " + this.departure
        //				" going? " + this.leaving +
        //				" workdays? " + this.workDays +
        //				" saturdays? " + this.saturdays +
        //				" sundays? " + this.sundays;
    }
}
