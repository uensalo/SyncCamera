package com.moveit.synccamera;

import java.util.Observer;

public interface ProgressObservable {
    public void addObserver(ProgressObserver o);

    public void notifyObservers();
}
