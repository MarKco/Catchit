package com.ilsecondodasinistra.catchit;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.graphics.Color;

public class Bus {
	private Date departure;
	private String lineNumber;
    private String departureStop;
    private String arrivalStop;
    private Date arrival;
//	private boolean leaving;
//	private boolean workDays;
//	private boolean saturdays;
//	private boolean sundays;
	private int busColor;
	private int textColor;
	private boolean toBePutLast;
	private SimpleDateFormat dateFormatter = new SimpleDateFormat("H:mm");

    public Bus(Date departure, String lineNumber, String departureStop, String arrivalStop, Date arrival) {
        this.departure = departure;
        this.lineNumber = lineNumber;
        this.departureStop = departureStop;
        this.arrivalStop = arrivalStop;
        this.arrival = arrival;

        this.textColor = Color.BLACK;
        this.toBePutLast = false;
    }

    public boolean getToBePutLast()
	{
		return this.toBePutLast;
	}
	
	public void setToBePutLast(boolean value)
	{
		this.toBePutLast = value;
	}
	
	public void resetColors()
	{
		if(this.lineNumber.equals("12"))
		{
			//Rosso più scuro
			this.busColor = Color.argb(40, 178, 34, 34);
		}
		
		if(this.lineNumber.equals("12/"))
		{
			//Rosso
			this.busColor = Color.argb(30, 255, 20, 147);
		}
		
		if(this.lineNumber.equals("12L"))
		{
			//Rosso meno intenso
			this.busColor = Color.argb(40, 255, 69, 0);
		}
		
		if(this.lineNumber.equals("SCORZE'"))
		{
			//Azzurrino
			this.busColor = Color.argb(50, 185, 211, 238);
		}
		
		if(this.lineNumber.equals("NOALE"))
		{
			//Giallino
			this.busColor = Color.argb(30, 255, 255, 0);
		}

        if(this.lineNumber.equals("T1"))
        {
            //Azzurrino
            this.busColor = Color.argb(100, 204, 255, 255);
        }

        if(this.lineNumber.equals("N1"))
        {
            //azzurrino
            this.busColor = Color.parseColor("#44518aff");
        }

        if(this.lineNumber.equals("N2"))
        {
            //Giallino
            this.busColor = Color.argb(30, 30, 255, 255);
        }


        this.textColor = Color.BLACK;
	}
	
	public int getTextColor() {
		return this.textColor;
	}
	
	public void setTextColor(int a, int r, int g, int b) {
		this.textColor = Color.argb(a, r, g, b);
	}
	
	public void setColor(int a, int r, int g, int b)
	{
		this.busColor = Color.argb(a, r, g, b);
	}
	
	public void resetTextColor()
	{
		this.textColor = Color.BLACK;
	}
	
	public int getColor()
	{
		return this.busColor;
	}
	
//	public boolean doesItPassOnSundays() {
//		return this.sundays;
//	}
//
//	public boolean doesItPassOnSaturdays() {
//		return this.saturdays;
//	}
//
//	public boolean doesItPassOnWorkdays() {
//		return this.workDays;
//	}
	
	public String getLine()
	{
		return this.lineNumber;
	}
	
	public String getDepartureString()
	{
		return dateFormatter.format(this.departure);
	}

    public String getArrivalString() {
        return dateFormatter.format(this.arrival);
    }
	
	public Date getDeparture()
	{
		return this.departure;
	}

    public void setDeparture(Date departure) {
        this.departure = departure;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getDepartureStop() {
        return departureStop;
    }

    public void setDepartureStop(String departureStop) {
        this.departureStop = departureStop;
    }

    public String getArrivalStop() {
        return arrivalStop;
    }

    public void setArrivalStop(String arrivalStop) {
        this.arrivalStop = arrivalStop;
    }

    public Date getArrival() {
        return arrival;
    }

    public void setArrival(Date arrival) {
        this.arrival = arrival;
    }

    public String toString()
	{
		return  "Line: " + this.lineNumber +
				" Time: " + this.departure;
//				" going? " + this.leaving +
//				" workdays? " + this.workDays +
//				" saturdays? " + this.saturdays +
//				" sundays? " + this.sundays;
	}
	
}
