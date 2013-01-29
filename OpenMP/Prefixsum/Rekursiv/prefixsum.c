/* 
 * File:   prefixsum.c
 * Author: Christian Beikov
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <omp.h>


#define ATYPE int //compile time type, can change
#define THREADS 54
#define ELEMENTS_STEP 25000000
#define ELEMENTS 200000000

void scanRecursive(ATYPE*, int);
void init(ATYPE*, int);

void scanRecursive(ATYPE *x, int n) {
    ATYPE *y;
    int from = 0;
    int to = n / 2;
    int i;

    if (n == 1) {
        return;
    }

    y = (ATYPE*) malloc(sizeof (ATYPE*) * (n / 2));

#pragma omp parallel for
    for (i = from; i < to; i++) {
        y[i] = x[2 * i] + x[2 * i + 1];
    }

    scanRecursive(y, n / 2);

    x[1] = y[0];

#pragma omp parallel for
    for (i = from + 1; i < to; i++) {
        x[2 * i] = y[i - 1] + x[2 * i];
        x[2 * i + 1] = y[i];
    }

    if (n % 2 != 0) {
        x[n - 1] = y[n / 2 - 1] + x[n - 1];
    }

    free(y);
}

int main(int argc, char** argv) {
    ATYPE *x;
    int i, j;
    double start;

    for (j = ELEMENTS_STEP; j <= ELEMENTS; j += ELEMENTS_STEP) {
        x = (ATYPE*) malloc(sizeof (ATYPE*) * j);
        init(x, j);

        for (i = 1; i <= THREADS; i++) {
            omp_set_num_threads(i);

            start = omp_get_wtime();
            scanRecursive(x, j);
            fprintf(stdout, "%d;%d;%f\n", j, i, omp_get_wtime() - start);
        }

        free(x);
    }

    fflush(stdout);
    return (EXIT_SUCCESS);
}

void init(ATYPE *x, int j) {
    int i;

#pragma omp parallel for
    for (i = 0; i < j; i++) {
        x[i] = i + 1;
    }
}
