/* 
 * File:   matrixvector.c
 * Author: Christian Beikov
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <omp.h>


#define ATYPE int //compile time type, can change
#define THREADS 48
#define ROWS_STEP 250
#define ROWS 3000
#define COLUMNS_STEP 250
#define COLUMNS 3000
#define BLOCKSIZE_ELEMENTS 10
#define BLOCKSIZES {1, 5, 10, 25, 50, 100, 250, 500, 1000, 1500}

void init(ATYPE**, int, int, ATYPE);
void initVector(ATYPE*, int, ATYPE);
void multiply(ATYPE**, ATYPE*, ATYPE*, int, int, int);

void multiply(ATYPE **A, ATYPE *x, ATYPE *result, int rows, int columns, int blockSize) {
    int i, j;

#pragma omp parallel for private(j) schedule(static, blockSize)
    for (i = 0; i < rows; i++) {
        for (j = 0; j < columns; j++) {
            result[i] += A[i][j] * x[j];
        }
    }
}

int main(int argc, char** argv) {
    ATYPE **A, *x, *result;
    int i, rowCount, columnCount, blockSize;
    int blockSizes[] = BLOCKSIZES;
    double start;

    for (rowCount = ROWS_STEP; rowCount <= ROWS; rowCount += ROWS_STEP) {
        A = (ATYPE**) malloc(sizeof (ATYPE*) * rowCount);
        result = (ATYPE*) malloc(sizeof (ATYPE) * rowCount);

        for (columnCount = COLUMNS_STEP; columnCount <= COLUMNS; columnCount += COLUMNS_STEP) {
            for (i = 0; i < rowCount; i++) {
                A[i] = (ATYPE*) malloc(sizeof (ATYPE) * columnCount);
            }

            x = (ATYPE*) malloc(sizeof (ATYPE) * columnCount);

            for (blockSize = 0; blockSize < BLOCKSIZE_ELEMENTS; blockSize++) {
                omp_set_num_threads(THREADS);

                init(A, rowCount, columnCount, 1);
                initVector(x, columnCount, 2);
                initVector(result, rowCount, 0);

                start = omp_get_wtime();
                multiply(A, x, result, rowCount, columnCount, blockSizes[blockSize]);
                fprintf(stdout, "%d;%d%d;%lf\n", rowCount, columnCount, blockSizes[blockSize], omp_get_wtime() - start);
            }

            for (i = 0; i < rowCount; i++) {
                free(A[i]);
            }

            free(x);
        }

        free(A);
        free(result);
    }

    fflush(stdout);
    return (EXIT_SUCCESS);
}

void init(ATYPE **x, int rows, int columns, ATYPE value) {
    int i, j;

#pragma omp parallel for
    for (i = 0; i < rows; i++) {
        for (j = 0; j < columns; j++) {
            x[i][j] = value;
        }
    }
}

void initVector(ATYPE *x, int size, ATYPE value) {
    int i;

#pragma omp parallel for
    for (i = 0; i < size; i++) {
        x[i] = value;
    }
}
