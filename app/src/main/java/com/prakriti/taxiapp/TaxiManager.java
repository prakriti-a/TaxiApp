package com.prakriti.taxiapp;

import android.location.Location;

public class TaxiManager {
// MODEL -> data manager

    private Location destinationLocation;

    public void setDestinationLocation(Location destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public float returnDistanceToDestinationInMetres(Location currentLocation) {
        if( currentLocation != null && destinationLocation != null) {
            return currentLocation.distanceTo(destinationLocation); // method from Location class in metres
        }
        else { // if null
            return -100.0f;
        }
    }

    public String returnMilesBetweenCurrentAndDestination(Location currentLocation, int metresPerMile) {
        float miles = (returnDistanceToDestinationInMetres(currentLocation) / metresPerMile); // to get miles
        return miles + " mile(s)";
    }

    public String returnTimeLeftToReachDestination(Location currentLocation, float milesPerHour, int metresPerMile) {
        float distanceInMetres = returnDistanceToDestinationInMetres(currentLocation);
        float timeInHoursFloat = distanceInMetres / (milesPerHour * metresPerMile); // distance / speed = time
        int timeInHoursInteger = (int) timeInHoursFloat; // hours
        int minutesLeft = (int) (timeInHoursFloat - timeInHoursInteger) * 60; // mins
        String timeLeft = timeInHoursInteger + " hour(s), " + minutesLeft + " minute(s) remaining";
        if(timeInHoursInteger == 0 && minutesLeft == 0) {
            timeLeft = "You have reached the destination";
        }
        return timeLeft;
    }

}
