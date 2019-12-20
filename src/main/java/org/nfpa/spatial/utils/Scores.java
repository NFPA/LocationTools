package org.nfpa.spatial.utils;

/*
* Scores enum for address and search scoring
* */

public enum Scores {
    HOUSE_NUMBER (5),
    ROAD (10),
    CITY (30),
    POSTCODE (40),
    STATE (15);

    private final float weight;
    Scores(final float weight) {
        this.weight = weight;
    }

    public float getWeight(){
        return weight;
    }
}
