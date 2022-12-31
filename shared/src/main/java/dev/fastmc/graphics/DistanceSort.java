package dev.fastmc.graphics;

public class DistanceSort {
    private static final int INSERTION_SORT_SIZE = 64;

    public static void insertionSort(int[] x, float[] dist, int from, int to) {
        for (int i1 = from; i1 < to; i1++) {
            int v1 = x[i1];
            float d1 = dist[v1];
            int i2 = i1;
            int v2;
            while (i2 > from && d1 < dist[(v2 = x[i2 - 1])]) {
                x[i2] = v2;
                i2--;
            }
            x[i2] = v1;
        }
    }

    private static void siftDown(int[] x, float[] dist, int p, int value, int from, int to) {
        for (int k; ; x[p] = x[p = k]) {
            k = (p << 1) - from + 2; // Index of the right child

            if (k > to) {
                break;
            }
            if (k == to || dist[x[k]] < dist[x[k - 1]]) {
                --k;
            }
            if (dist[x[k]] <= dist[value]) {
                break;
            }
        }
        x[p] = value;
    }

    public static void heapsort(int[] a, float[] dist, int from, int to) {
        for (int k = (from + to) >>> 1; k > from; ) {
            siftDown(a, dist, --k, a[k], from, to);
        }
        while (--to > from) {
            int max = a[from];
            siftDown(a, dist, from, a[to], from, to);
            a[to] = max;
        }
    }

    private static void swap(int[] a, int i1, int i2) {
        int t = a[i1];
        a[i1] = a[i2];
        a[i2] = t;
    }

    @SuppressWarnings({ "StatementWithEmptyBody", "unused" })
    public static void sort(int[] x, float[] dist, int from, int to, int depth) {
        int n = to - from;

        if (n < INSERTION_SORT_SIZE) {
            insertionSort(x, dist, from, to);
            return;
        }

        if (depth == 0) {
            heapsort(x, dist, from, to);
            return;
        }

        int step = (n >> 3) * 3 + 3;

        int last = to - 1;
        int s1 = from + step;
        int s5 = last - step;
        int s3 = (s1 + s5) >>> 1;
        int s2 = (s1 + s3) >>> 1;
        int s4 = (s3 + s5) >>> 1;

        int s3v = x[s3];
        float s3d = dist[s3v];

        if (dist[x[s5]] < dist[x[s2]]) {
            swap(x, s5, s2);
        }
        if (dist[x[s4]] < dist[x[s1]]) {
            swap(x, s4, s1);
        }
        if (dist[x[s5]] < dist[x[s4]]) {
            swap(x, s5, s4);
        }
        if (dist[x[s2]] < dist[x[s1]]) {
            swap(x, s2, s1);
        }
        if (dist[x[s4]] < dist[x[s2]]) {
            swap(x, s4, s2);
        }

        if (dist[s3v] < dist[x[s2]]) {
            if (dist[s3v] < dist[x[s1]]) {
                x[s3] = x[s2];
                x[s2] = x[s1];
                x[s1] = s3v;
            } else {
                x[s3] = x[s2];
                x[s2] = s3v;
            }
        } else if (dist[s3v] > dist[x[s4]]) {
            if (dist[s3v] > dist[x[s5]]) {
                x[s3] = x[s4];
                x[s4] = x[s5];
                x[s5] = s3v;
            } else {
                x[s3] = x[s4];
                x[s4] = s3v;
            }
        }

        if (dist[x[s1]] < dist[x[s2]] && dist[x[s2]] < dist[x[s3]] && dist[x[s3]] < dist[x[s4]] && dist[x[s4]] < dist[x[s5]]) {
            swap(x, from, s1);
            swap(x, last, s5);
            int p = x[from];
            float pd = dist[p];
            int q = x[last];
            float qd = dist[q];

            int left = from;
            int right = to - 1;

            for (int mid = left; mid < right; mid++) {
                int m = x[mid];
                float md = dist[m];
                if (md > qd) {
                    int r;
                    float rd;
                    do {
                        r = x[--right];
                        rd = dist[r];
                    } while (rd > qd && right > mid);
                    if (rd < pd) {
                        x[mid] = x[++left];
                        x[left] = r;
                    } else {
                        x[mid] = r;
                    }
                    x[right] = m;
                } else if (md < pd) {
                    x[mid] = x[++left];
                    x[left] = m;
                }
            }

            x[from] = x[left];
            x[left] = p;
            x[last] = x[right];
            x[right] = q;

            sort(x, dist, from, left, depth - 1);
            sort(x, dist, left + 1, right, depth - 1);
            sort(x, dist, right + 1, to, depth - 1);
        } else {
            swap(x, from, s3);

            int p = x[from];
            float pd = dist[p];
            int right = to;

            for (int left = from + 1; left < right; left++) {
                int l = x[left];
                float ld = dist[l];
                if (ld > pd) {
                    int r;
                    float rd;
                    do {
                        r = x[--right];
                        rd = dist[r];
                    } while (rd >= pd && right > left);
                    x[left] = r;
                    x[right] = l;
                }
            }

            x[from] = x[--right];
            x[right] = p;

            sort(x, dist, from, right, depth - 1);
            sort(x, dist, right + 1, to, depth - 1);
        }
    }

    public static void sort(int[] array, float[] dist, int from, int to) {
        int n = to - from;
        int depth = (int) (Math.log(n) / Math.log(2)) * 2;
        sort(array, dist, from, to, depth);
    }
}
