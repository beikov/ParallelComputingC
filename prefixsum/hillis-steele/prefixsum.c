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

void scanHillisSteele(ATYPE*, int);
void init(ATYPE*, int);

void scanHillisSteele(ATYPE *x, int n) {
    int i, j, k;
    ATYPE *y, *temp;

    if (n == 1) {
        return;
    }

    y = (ATYPE*) malloc(sizeof (ATYPE*) * n);

    for (k = 1, j = 0; k < n; k <<= 1, j++) {
#pragma omp parallel for private(i)
        for (i = 0; i < k; i++) {
            y[i] = x[i];
        }

#pragma omp parallel for private(i)
        for (i = k; i < n; i++) {
            y[i] = x[i - k] + x[i];
        }

        temp = x;
        x = y;
        y = temp;
    }

    // Do this so the free opertion outside of scan can work properly
    if (j % 2 != 0) {
        free(x);
    } else {
        free(y);
    }
}

int main(int argc, char** argv) {
    ATYPE *x;
    int i, j;
    double start;

    for (j = ELEMENTS_STEP; j <= ELEMENTS; j += ELEMENTS_STEP) {
        x = (ATYPE*) malloc(sizeof (ATYPE*) * j);

        for (i = 1; i <= THREADS; i++) {
            omp_set_num_threads(i);
            init(x, j);

            start = omp_get_wtime();
            scanHillisSteele(x, j);
            fprintf(stdout, "%d;%d;%lf\n", j, i, omp_get_wtime() - start);
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
