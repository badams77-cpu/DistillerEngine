//package cmp.HeapSort;

package DistillerEngine;

// Compare.java
// delegate object to hand to HeapSort for callback compare
public interface Compare
    {

    // compare effectively returns a-b;
    // e.g. +1 (or any +ve number) if a > b
    //       0                     if a == b
    //      -1 (or any -ve number) if a < b
    abstract int compare(Object a, Object b);
    // e.g. return ((String)a).compareTo((String)b);
} // end class Compare

