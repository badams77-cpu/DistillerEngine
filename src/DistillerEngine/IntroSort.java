package DistillerEngine;

// See http://www.cs.ubc.ca/spider/harrison/Java/sorting-demo.html
// and http://www.cs.rpi.edu/~musser/gp/index_1.html

import java.util.*;

public class IntroSort {

  public static void heapSort(Object a[], Compare com){
    int N= a.length;
    int mid = N/2;
    for(int k = mid; k>0; k--){
      downheap(a,k,N,mid,com);
    }
    do {
      N = N-1;
      Object T=a[0];
      a[0] = a[N];
      a[N] = T;
      downheap(a,1,N,N/2,com );
    } while(N>1);
  }

  public static void heapSort(Object a[], int b, int f, Compare com){
    int N=f;
    int b1 = b+1;
    int mid = (N+b)/2;
    for(int k = mid; k>b; k--){
      downheap1(a,k,N,mid,b,com);
    }
    do {
      N = N-1;
      Object T=a[b];
      a[b] = a[N];
      a[N] = T;
      downheap1(a,b1,N,(N+b)/2,b,com );
    } while(N>b1);
  }

  private static void downheap(Object a[], int k, int N, int mid, Compare com){
    Object T = a[k-1];
    while( k<= mid){
      int j=k+k;
      if ( (j<N) && com.compare(a[j-1],a[j])<0){ j++; }
      if ( com.compare(T,a[j-1])>=0 ){ 
        break;
      } else {
        a[k-1] = a[j-1];
        k = j;
      }
    }
    a[k-1] = T;
  }

  private static void downheap1(Object a[], int k, int N, int mid, int b, Compare com){
    Object T = a[k-1];
    while( k<= mid){
      int j=k+k-b;
      if ( (j<N) && com.compare(a[j-1],a[j])<0){ j++; }
      if ( com.compare(T,a[j-1])>=0 ){ 
        break;
      } else {
        a[k-1] = a[j-1];
        k = j;
      }
    }
    a[k-1] = T;
  }

  private static void quickSort(Object a[], int l, int r, Compare com){
    int M=4;
    int i,j;
    Object T,V;
    if ( (r-l)<=M){ return; }
      i = (r+l)/2;
      if (com.compare(a[l],a[i])>0){
        T = a[i];
        a[i] = a[l];
        a[l] = T;
      }
      if (com.compare(a[l],a[r])>0){
        T = a[r];
        a[r] = a[l];
        a[l] = T;        
      }    
      if (com.compare(a[i],a[r])>0){
        T = a[r];
        a[r] = a[i];
        a[i] = T;        
      }      
      j = r -1;
      T = a[i];
      a[i] = a[j];
      V = a[j] = T;
      i = l;
      for(;;){
        while( com.compare(a[++i],V)<0 );
        while( com.compare(a[--j],V)>0 );
        if (j<i){ break; }
        T = a[i];
        a[i] = a[j];
        a[j] = T;         
      }
      T = a[i];
      a[i] = a[r-1];
      a[r-1] = T;
      quickSort(a,l,j,com);
      quickSort(a,i+1,r,com);
    
  }  

  private static void introSort(Object a[], int l, int r, int depth_limit, Compare com){
    if (depth_limit == 0){ heapSort(a, l, r+1, com); return; }
    int M=4;
    int i,j;
    Object T,V;
    if ( (r-l)<=M){ return; }
      i = (r+l)/2;
      if (com.compare(a[l],a[i])>0){
        T = a[i];
        a[i] = a[l];
        a[l] = T;
      }
      if (com.compare(a[l],a[r])>0){
        T = a[r];
        a[r] = a[l];
        a[l] = T;        
      }    
      if (com.compare(a[i],a[r])>0){
        T = a[r];
        a[r] = a[i];
        a[i] = T;        
      }      
      j = r -1;
      T = a[i];
      a[i] = a[j];
      V = a[j] = T;
      i = l;
      for(;;){
        while( com.compare(a[++i],V)<0 );
        while( com.compare(a[--j],V)>0 );
        if (j<i){ break; }
        T = a[i];
        a[i] = a[j];
        a[j] = T;         
      }
      T = a[i];
      a[i] = a[r-1];
      a[r-1] = T;
      depth_limit--;
      introSort(a,l,j,depth_limit,com);
      introSort(a,i+1,r,depth_limit,com);
  }  


  private static void insertionSort(Object a[], int lo0, int hi0, Compare com){
    int i,j;
    Object V;
    for(i=lo0+1; i<= hi0; i++){
      V = a[i];
      j = i;
      while ( (j>lo0) && com.compare(a[j-1],V)>0 ){
        a[j] = a[j-1];
        j--;
      }
      a[j] = V; 
    }
  }

  public static void quickSort(Object a[], Compare com){
    quickSort(a, 0, a.length-1,com);
    insertionSort(a, 0, a.length-1,com);
  }

  public static void sort(Object a[], Compare com){
    int n = a.length;
    introSort(a, 0, n-1, 2*floor_lg(n), com);
    insertionSort(a, 0, a.length-1,com);    
  }



  private static int floor_lg(int a){
//   Floor of log_2 a, for positive a
    int i=-1;
    while( a>63){ a>>>=6; i+= 6; }
    while( a!=0){ i++; a>>>=1; }
    return i;
  }

  private static void displayArray(Integer in[]){
    for(int i=0; i<in.length;i++){
      System.out.print( in[i].intValue()+" ");
    }
    System.out.println();
  }

  public static boolean isSorted(Object a[], Compare com){
    if (a.length<2){ return true; }
    Object x = a[0];
    for(int i=1;i<a.length;i++){
      Object y = a[i];
      if (com.compare(x,y)>0){ return false; }
      x = y;
    }
    return true;
  }

  public static void main(String args[]){ 
//   Test routines
    java.util.Random rand = new java.util.Random();
    LICompare com = new LICompare();
    for(int goes = 0; goes<5000; goes++){
      int s = rand.nextInt(3000)+3000;
      int a = 3*s;
      Integer array[] = new Integer[s];
      for(int i=0; i<s; i++){
        array[i] = new Integer( rand.nextInt(a) );      
      }
      int q = rand.nextInt(10);
      if (q==4){
        array = meanOf3Killer(s);
        System.out.println("Mean of 3 Killer"); 
      } else if (q==5){
        array =  inOrder(s);
        System.out.println("Already Sorted"); 
      }
      Integer array1[] = (Integer[]) array.clone();
      Integer array2[] = (Integer[]) array.clone();
      Integer array3[] = (Integer[]) array.clone();
      Integer array4[] = (Integer[]) array.clone();
      Integer array5[] = (Integer[]) array.clone();
      System.out.println(array.length+" elements ");


      
      long start = System.currentTimeMillis();
      com.count = 0;
      try {
        heapSort(array,com);
      } catch (Exception e){
        System.out.println("Heapsort failed");
        e.printStackTrace(System.out);
      }
      System.out.println( "Heapsort: "+ (System.currentTimeMillis()-start)+" millis, "+com.count+" comparations");
      if (!isSorted(array,com)){ System.out.println(" Heapsort failed: "); displayArray(array); }
/*
      start = System.currentTimeMillis();
      com.count = 0;
      try {
        HeapSort.sort(array3,com);
      } catch (Exception e){
        System.out.println("HeapSort.sort (orignal failed");
        e.printStackTrace(System.out);
      }       
      System.out.println( "HeapSort.sort (orignal): "+ (System.currentTimeMillis()-start)+" millis, "+com.count+" comparations");
*/
      if (!isSorted(array3,com)){  System.out.println(" Heapsort (orignal failed: "); displayArray(array3); }
      start = System.currentTimeMillis();
      com.count = 0;
      try {
        quickSort(array1,com);
      } catch (Exception e){
        System.out.println("Quicksort failed");
        e.printStackTrace(System.out);
      }
      System.out.println( "Quicksort: "+ (System.currentTimeMillis()-start)+" millis, "+com.count+" comparations");
      if (!isSorted(array1,com)){ System.out.println(" Quicksort failed: "); displayArray(array1); }
      start = System.currentTimeMillis();
      com.count = 0; 
      try {
        sort(array2,com);
      } catch (Exception e){
        System.out.println("introsort failed");
        e.printStackTrace(System.out);
      }       
      System.out.println( "Introsort: "+ (System.currentTimeMillis()-start)+" millis, "+com.count+" comparations");
      if (!isSorted(array2,com)){ System.out.println(" Introsort failed: "); displayArray(array2); }
      start = System.currentTimeMillis();
      com.count = 0; 
      try {
        Arrays.sort(array4,com);
      } catch (Exception e){
        System.out.println("Internal Sort failed");
        e.printStackTrace(System.out);
      }       
      System.out.println( "Java.util.Arrays.sort: "+ (System.currentTimeMillis()-start)+" millis, "+com.count+" comparations");
      if (!isSorted(array4,com)){ System.out.println("java.util.Arrays.sort failed: "); displayArray(array4); }
      start = System.currentTimeMillis();
      com.count = 0; 
      try {
        mergeSort(array5,com);
      } catch (Exception e){
        System.out.println("Merge Sort failed");
        e.printStackTrace(System.out);
      }       
      System.out.println( "mergeSort: "+ (System.currentTimeMillis()-start)+" millis, "+com.count+" comparations");
      if (!isSorted(array5,com)){ System.out.println("mergeSort failed: "); displayArray(array5); }

      System.out.println("---------");
    }
  }




    private static final int BREAKPOINT = 16;

    // sort: reorders the elements of <b>elts</b> so that they are in
    // increasing order.
    public static void mergeSort(Object[] elts,Compare com) {
        int last;

        // First go through, doing insertion sort to sort the array in
        // pieces of BREAKPOINT elements.
        for(int first = 0; first < elts.length; first = last) {
            last = first + BREAKPOINT;
            if(last >= elts.length) last = elts.length;

            for(int i = first; i < last; i++) {
                Object k = elts[i]; // store element i in k
                int j;
                for(j = i - 1; j >= first && com.compare(elts[j],k)>0; j--) {
                    elts[j + 1] = elts[j]; // shift elts[j] up one slot
                }
                elts[j + 1] = k; // place k into vacated slot
            }
        }

        if(elts.length <= BREAKPOINT) return;

        // Allocate a second array, since Mergesort needs a second array
        // into which to merge.
        Object[] src = elts;
        Object[] dst = new Object[elts.length];

        // Now we'll iteratively double the sizes of the chunks of 
        // src which are sorted. We begin with BREAKPOINT, since we've
        // already handled those chunks with insertion sort.
        for(int len = BREAKPOINT; len < elts.length; len *= 2) {
            for(int first = 0; first < elts.length; first = last) {
                int second = first + len;
                if(second >= elts.length) {
                    System.arraycopy(src, first, dst, first,
                        elts.length - first);
                    break;
                }
                last = second + len;
                if(last > elts.length) last = elts.length;
                merge(dst, src, first, second, last,com);
            }

            // dst has what we want. We swap src and dst so that, in the
            // next iteration, src has the sorted elements, and we
            // re-use dst (previously src) as the destination array.
            Object[] tmp = src;
            src = dst;
            dst = tmp;
        }

        // If it happens that we've sorted into the wrong array, we copy
        // back into the original.
        if(src != elts) {
            System.arraycopy(src, 0, elts, 0, elts.length);
        }
    }

    // merge: merges arrays <b>a</b> and <b>b</b>, placing the result into the
    // array <b>dest</b>. This only works if both <b>a</b> and <b>b</b> are already in
    // increasing order.
    private static void merge(Object[] dst, Object[] src, int i, int j, int j_last, Compare com) {
        int i_last = j;
        int k = i;

        if(i < i_last && j < j_last) {
            while(true) {
                if(com.compare(src[i],src[j])<0 ) {
                    dst[k++] = src[i++];
                    if(i >= i_last) break;
                } else {
                    dst[k++] = src[j++];
                    if(j >= j_last) break;
                }
            }
        }

        if(i < i_last) {
            System.arraycopy(src, i, dst, k, i_last - i);
        }
        if(j < j_last) {
            System.arraycopy(src, j, dst, k, j_last - j);
        }
    }


  private static Integer[] meanOf3Killer(int k){
    int n=2*k;
    Integer a[] = new Integer[n];
    for(int i=0;i<n;i++){
      int x= i/2;
      if (i<k){
        if ( (x+x) == i){ 
          a[i] = new Integer(i+1);
        } else {
          a[i] = new Integer(k+i);
        }
      } else {
        a[i] = new Integer(2*(i-k+1));
      }
    }
    return a;
  }
  
  private static Integer[] inOrder(int n){
    Integer a[] = new Integer[n];
    for(int i=0;i<n;i++){
          a[i] = new Integer(i+1);
    }
    return a;
  }
  

}

class LICompare implements Compare,Comparator {

  public int count = 0;    

  public LICompare(){
    count = 0;
  }

  public int compare(Object a, Object b){
    count++;
    return ((Integer) a).intValue() - ((Integer) b).intValue();
  }

  public boolean equals(Object a, Object b){
    count++;
    return ((Integer) a).intValue() == ((Integer) b).intValue();
  }

}