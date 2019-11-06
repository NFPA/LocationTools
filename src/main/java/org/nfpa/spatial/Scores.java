package org.nfpa.spatial;

public enum Scores {
    HOUSE_NUMBER (5),
    ROAD (10),
    CITY (15),
    POSTCODE (40),
    STATE (30);

    private final float weight;
    Scores(final float weight) {
        this.weight = weight;
    }

    public float getWeight(){
        return weight;
    }
}
