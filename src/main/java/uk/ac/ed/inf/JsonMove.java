package uk.ac.ed.inf;

public record JsonMove(String orderNo,
                         double fromLongitude,
                         double fromLatitude,
                         double angle,
                         double toLongitude,
                         double toLatitude) {}
