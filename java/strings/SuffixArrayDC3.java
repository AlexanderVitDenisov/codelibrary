package strings;

import java.util.*;

// Suffix array DC3 O(n) algorithm from "Linear Work Suffix Array Construction"
public class SuffixArrayDC3 {
    static boolean leq(int a1, int a2, int b1, int b2) {
        return a1 < b1 || a1 == b1 && a2 <= b2;
    }

    static boolean leq(int a1, int a2, int a3, int b1, int b2, int b3) {
        return a1 < b1 || a1 == b1 && leq(a2, a3, b2, b3);
    }

    // stably sort a[0..n-1] to b[0..n-1] with keys in 0..K from r
    static void radixPass(int[] a, int[] b, int[] r, int n, int K) {
        // count occurrences
        int[] c = new int[K + 1]; // counter array
        for (int i = 0; i <= K; i++) c[i] = 0; // reset counters
        for (int i = 0; i < n; i++) c[r[a[i]]]++; // count occurrences
        for (int i = 0, sum = 0; i <= K; i++) // exclusive prefix sums
        {
            int t = c[i];
            c[i] = sum;
            sum += t;
        }
        for (int i = 0; i < n; i++) b[c[r[a[i]]]++] = a[i]; // sort
    }

    // find the suffix array SA of T[0..n-1] in {1..K}^n
    // require T[n]=T[n+1]=T[n+2]=0, n>=2
    private static void suffixArray(int[] T, int[] SA, int n, int K) {
        int n0 = (n + 2) / 3, n1 = (n + 1) / 3, n2 = n / 3, n02 = n0 + n2;
        int[] R = new int[n02 + 3];
        R[n02] = R[n02 + 1] = R[n02 + 2] = 0;
        int[] SA12 = new int[n02 + 3];
        SA12[n02] = SA12[n02 + 1] = SA12[n02 + 2] = 0;
        int[] R0 = new int[n0];
        int[] SA0 = new int[n0];

        //******* Step 0: Construct sample ********
        // generate positions of mod 1 and mod 2 suffixes
        // the "+(n0-n1)" adds a dummy mod 1 suffix if n%3 == 1
        for (int i = 0, j = 0; i < n + (n0 - n1); i++) if (i % 3 != 0) R[j++] = i;

        //******* Step 1: Sort sample suffixes ********
        // lsb radix sort the mod 1 and mod 2 triples
        radixPass(R, SA12, Arrays.copyOfRange(T, 2, T.length), n02, K);
        radixPass(SA12, R, Arrays.copyOfRange(T, 1, T.length), n02, K);
        radixPass(R, SA12, T, n02, K);

        // find lexicographic names of triples and
        // write them to correct places in R
        int name = 0, c0 = -1, c1 = -1, c2 = -1;
        for (int i = 0; i < n02; i++) {
            if (T[SA12[i]] != c0 || T[SA12[i] + 1] != c1 || T[SA12[i] + 2] != c2) {
                name++;
                c0 = T[SA12[i]];
                c1 = T[SA12[i] + 1];
                c2 = T[SA12[i] + 2];
            }
            if (SA12[i] % 3 == 1) {
                R[SA12[i] / 3] = name;
            } // write to R1
            else {
                R[SA12[i] / 3 + n0] = name;
            } // write to R2
        }

        // recurse if names are not yet unique
        if (name < n02) {
            suffixArray(R, SA12, n02, name);
            // store unique names in R using the suffix array
            for (int i = 0; i < n02; i++) R[SA12[i]] = i + 1;
        } else // generate the suffix array of R directly
            for (int i = 0; i < n02; i++) SA12[R[i] - 1] = i;
        //******* Step 2: Sort nonsample suffixes ********
        // stably sort the mod 0 suffixes from SA12 by their first character
        for (int i = 0, j = 0; i < n02; i++) if (SA12[i] < n0) R0[j++] = 3 * SA12[i];
        radixPass(R0, SA0, T, n0, K);

        //******* Step 3: Merge ********
        // merge sorted SA0 suffixes and sorted SA12 suffixes
        for (int p = 0, t = n0 - n1, k = 0; k < n; k++) {
            int i = SA12[t] < n0 ? SA12[t] * 3 + 1 : (SA12[t] - n0) * 3 + 2; // pos of current offset 12 suffix
            int j = SA0[p]; // pos of current offset 0 suffix
            if (SA12[t] < n0 ? // different compares for mod 1 and mod 2 suffixes
                    leq(T[i], R[SA12[t] + n0], T[j], R[j / 3]) :
                    leq(T[i], T[i + 1], R[SA12[t] - n0 + 1], T[j], T[j + 1], R[j / 3 + n0])) { // suffix from SA12 is smaller
                SA[k] = i;
                t++;
                if (t == n02) // done --- only SA0 suffixes left
                    for (k++; p < n0; p++, k++) SA[k] = SA0[p];
            } else { // suffix from SA0 is smaller
                SA[k] = j;
                p++;
                if (p == n0) // done --- only SA12 suffixes left
                    for (k++; t < n02; t++, k++) SA[k] = SA12[t] < n0 ? SA12[t] * 3 + 1 : (SA12[t] - n0) * 3 + 2;
            }
        }
    }

    public static int[] suffixArray(CharSequence s) {
        int n = s.length();
        if (n <= 1)
            return new int[n];
        int[] T = new int[n + 3];
        for (int i = 0; i < n; i++)
            T[i] = s.charAt(i);
        int[] sa = new int[n];
        suffixArray(T, sa, n, 256);
        return sa;
    }

    // longest common prefixes array in O(n)
    public static int[] lcp(int[] sa, CharSequence s) {
        int n = sa.length;
        int[] rank = new int[n];
        for (int i = 0; i < n; i++)
            rank[sa[i]] = i;
        int[] lcp = new int[n - 1];
        for (int i = 0, h = 0; i < n; i++) {
            if (rank[i] < n - 1) {
                for (int j = sa[rank[i] + 1]; Math.max(i, j) + h < s.length() && s.charAt(i + h) == s.charAt(j + h); ++h)
                    ;
                lcp[rank[i]] = h;
                if (h > 0)
                    --h;
            }
        }
        return lcp;
    }

    // Usage example
    public static void main(String[] args) {
        String s1 = "abcab";
        int[] sa1 = suffixArray(s1);

        // print suffixes in lexicographic order
        for (int p : sa1)
            System.out.println(s1.substring(p));

        System.out.println("lcp = " + Arrays.toString(lcp(sa1, s1)));

        // random test
        Random rnd = new Random(1);
        for (int step = 0; step < 100000; step++) {
            int n = rnd.nextInt(100) + 1;
            StringBuilder s = rnd.ints(n, 0, 10).collect(StringBuilder::new, (sb, i) -> sb.append((char) ('a' + i)), StringBuilder::append);
            int[] sa = suffixArray(s);
            int[] lcp = lcp(sa, s);
            for (int i = 0; i + 1 < n; i++) {
                String a = s.substring(sa[i]);
                String b = s.substring(sa[i + 1]);
                if (a.compareTo(b) >= 0
                        || !a.substring(0, lcp[i]).equals(b.substring(0, lcp[i]))
                        || (a + " ").charAt(lcp[i]) == (b + " ").charAt(lcp[i]))
                    throw new RuntimeException();
            }
        }
        System.out.println("Test passed");
    }
}
