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

void scanIterative(ATYPE*, int);
void init(ATYPE*, int);

void scanIterative(ATYPE *x, int n) {
    int from;
    int to;
    int i;
    ATYPE *y, xSum;

    if (n == 1) {
        return;
    }

    y = (ATYPE*) malloc(sizeof (ATYPE*) * omp_get_max_threads());

#pragma omp parallel private(xSum, i, from, to)
    {
        from = (n / omp_get_num_threads()) * omp_get_thread_num();

        if (omp_get_thread_num() + 1 == omp_get_num_threads()) {
            to = n;
        } else {
            to = (n / omp_get_num_threads()) * (omp_get_thread_num() + 1);
        }

        for (i = from + 1; i < to; i++) {
            x[i] += x[i - 1];
        }

        if (omp_get_thread_num() + 1 != omp_get_num_threads()) {
            y[omp_get_thread_num()] = x[to - 1];
        }

#pragma omp barrier
        
        if (omp_get_thread_num() != 0) {
            xSum = 0;

            for (i = 0; i <= omp_get_thread_num() - 1; i++) {
                xSum += y[i];
            }

            for (i = from; i < to; i++) {
                x[i] += xSum;
            }
        }
    }

    free(y);
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
            scanIterative(x, j);
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
