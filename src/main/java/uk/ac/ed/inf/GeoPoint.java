package uk.ac.ed.inf;

import java.util.List;

public record GeoPoint(String type , List<List<Double>> coordinates ){}