/* 
 * File:   prefixsum.c
 * Author: Christian
 *
 * Created on 27. Jänner 2013, 00:15
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <mpi.h>

#define PREFIXSUM_TAG 1234 // tag for control messages

void init(int **x, int rows, int columns, int value);
void initVector(int *x, int size, int value);
void verify(int *actual, int rowCount, int columnCount, int vectorValue);
void multiplySequential(int **A, int *x, int *result, int rows, int columns);
void scatter(int **A, int *x, int *result, int rowCount, int columnCount, int **localA, int *localX, int *localResult, int localRowCount, int localColumnCount, int rank, int size);

void multiplySequential(int **A, int *x, int *result, int rows, int columns) {
    int i, j;

    for (i = 0; i < rows; i++) {
        for (j = 0; j < columns; j++) {
            result[i] += A[i][j] * x[j];
        }
    }
}

void scatter(int **A, int *x, int *result, int rowCount, int columnCount, int **localA, int *localX, int *localResult, int localRowCount, int localColumnCount, int rank, int size) {
    int *sendBuffer, *receiveBuffer, *receiveCounts, *localResultSub;
    int i, j, nextIndex;

    if (rank == 0) {
        sendBuffer = (int*) malloc(sizeof (int) * rowCount * columnCount);
        nextIndex = 0;

        for (i = 0; i < columnCount; i++) {
            for (j = 0; j < rowCount; j++) {
                sendBuffer[nextIndex++] = A[j][i];
            }
        }
    }

    receiveBuffer = (int*) malloc(sizeof (int) * localRowCount * localColumnCount);

    MPI_Scatter(sendBuffer, localColumnCount * localRowCount, MPI_INT,
            receiveBuffer, localColumnCount * localRowCount, MPI_INT,
            0, MPI_COMM_WORLD);

    receiveCounts = (int*) malloc(sizeof (int) * size);
    localResultSub = (int*) malloc(sizeof (int) * (localRowCount / size));
    nextIndex = 0;

    for (i = 0; i < size; i++) {
        receiveCounts[i] = localRowCount / size;
    }

    for (j = 0; j < localRowCount; j++) {
        localA[j] = (int*) malloc(sizeof (int) * localColumnCount);
        localA[j][0] = receiveBuffer[nextIndex++];
    }
    for (i = 1; i < localColumnCount; i++) {
        for (j = 0; j < localRowCount; j++) {
            localA[j][i] = receiveBuffer[nextIndex++];
        }
    }

    if (rank == 0) {
        free(sendBuffer);
    }

    free(receiveBuffer);

    MPI_Scatter(x, localColumnCount, MPI_INT,
            localX, localColumnCount, MPI_INT,
            0, MPI_COMM_WORLD);

    // local result is just partial
    multiplySequential(localA, localX, localResult, localRowCount, localColumnCount);
    
    MPI_Reduce_scatter(localResult, localResultSub, receiveCounts,
            MPI_INT, MPI_SUM, MPI_COMM_WORLD);
    
//    for(i = 0; i < localRowCount / size; i++){
//        fprintf(stdout, "%d ", localResultSub[i]);
//    }
//    fprintf(stdout, "\n");

    MPI_Gather(localResultSub, localRowCount / size, MPI_INT,
            result, localRowCount / size, MPI_INT,
            0, MPI_COMM_WORLD);

    free(receiveCounts);
    free(localResultSub);
}

void gather(int **A, int *x, int *result, int rowCount, int columnCount, int **localA, int *localX, int *localResult, int localRowCount, int localColumnCount, int rank, int size) {
    int *sendBuffer, *receiveBuffer, *localResultSub, *localXSub;
    int i, j, nextIndex;

    if (rank == 0) {
        sendBuffer = (int*) malloc(sizeof (int) * rowCount * columnCount);
        nextIndex = 0;

        for (i = 0; i < rowCount; i++) {
            for (j = 0; j < columnCount; j++) {
                sendBuffer[nextIndex++] = A[i][j];
            }
        }
    }

    receiveBuffer = (int*) malloc(sizeof (int) * localRowCount * localColumnCount);

    MPI_Scatter(sendBuffer, localColumnCount * localRowCount, MPI_INT,
            receiveBuffer, localColumnCount * localRowCount, MPI_INT,
            0, MPI_COMM_WORLD);

    localResultSub = (int*) malloc(sizeof (int) * (localColumnCount / size));
    nextIndex = 0;

    for (i = 0; i < localRowCount; i++) {
        localA[i] = (int*) malloc(sizeof (int) * localColumnCount);

        for (j = 0; j < columnCount; j++) {
            localA[i][j] = receiveBuffer[nextIndex++];
        }
    }

    if (rank == 0) {
        free(sendBuffer);
    }

    free(receiveBuffer);

    localXSub = (int*) malloc(sizeof (int) * columnCount / size);
    
    MPI_Scatter(x, columnCount / size, MPI_INT,
            localXSub, columnCount / size, MPI_INT,
            0, MPI_COMM_WORLD);
        
    MPI_Allgather(localXSub, columnCount / size, MPI_INT,
                  localX, columnCount / size, MPI_INT,
                  MPI_COMM_WORLD);
    
    free(localXSub);

    // local result is just partial
    multiplySequential(localA, localX, localResult, localRowCount, localColumnCount);

    MPI_Gather(localResult, localRowCount, MPI_INT,
            result, localRowCount, MPI_INT,
            0, MPI_COMM_WORLD);
    
    free(localResultSub);
}

int main(int argc, char** argv) {
    int rank, size;
    int **A, *x, *result, **localA, *localX, *localResult;
    int i, rowCount, columnCount, localRowCount, localColumnCount;
    double start, stop;
    int doScatter = 0;

    if (MPI_Init(&argc, &argv) != MPI_SUCCESS) {
        fprintf(stderr, "Unable to initialize MPI!\n");
        return -1;
    }

    if (argc != 4 || (rowCount = atoi(argv[1])) == 0 || (columnCount = atoi(argv[2])) == 0) {
        fprintf(stdout, "No rowCount and columnCount parameter given!\n");
        return -1;
    }

    if (strcmp("-scatter", argv[3]) == 0) {
        doScatter = 1;
    } else if (strcmp("-gather", argv[3]) == 0) {
        doScatter = 0;
    } else {
        fprintf(stdout, "Neither scatter nor gather are activated!\n");
        return -1;
    }

    // get rank and size from communicator
    MPI_Comm_size(MPI_COMM_WORLD, &size);
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    
    rowCount = rowCount - rowCount % size;
    columnCount = columnCount - columnCount % size;

    if (doScatter) {
        localColumnCount = columnCount / size;
        localRowCount = rowCount;
    } else {
        localRowCount = rowCount / size;
        localColumnCount = columnCount;
    }

    if (rank == 0) {
        A = (int**) malloc(sizeof (int*) * rowCount);
        x = (int*) malloc(sizeof (int) * columnCount);
        result = (int*) malloc(sizeof (int) * rowCount);

        for (i = 0; i < rowCount; i++) {
            A[i] = (int*) malloc(sizeof (int) * columnCount);
        }

        init(A, rowCount, columnCount, 1);
        initVector(x, columnCount, 2);
        initVector(result, rowCount, 0);
        start = MPI_Wtime();
    }

    localA = (int**) malloc(sizeof (int*) * localRowCount);
    localX = (int*) malloc(sizeof (int) * localColumnCount);
    localResult = (int*) malloc(sizeof (int) * localRowCount);

    initVector(result, localRowCount, 0);

    if (doScatter) {
        scatter(A, x, result, rowCount, columnCount, localA, localX, localResult, localRowCount, localColumnCount, rank, size);
    } else {
        gather(A, x, result, rowCount, columnCount, localA, localX, localResult, localRowCount, localColumnCount, rank, size);
    }

    if (rank == 0) {
        stop = MPI_Wtime();
        fprintf(stdout, "%d;%d;%d;%f\n", size, rowCount, columnCount, stop - start);
        verify(result, rowCount, columnCount, 2);
    }

    for (i = 0; i < localRowCount; i++) {
        free(localA[i]);
    }

    free(localA);
    free(localX);
    free(localResult);

    if (rank == 0) {
        for (i = 0; i < rowCount; i++) {
            free(A[i]);
        }

        free(A);
        free(x);
        free(result);
    }

    MPI_Finalize();
    fflush(stdout);
    return (EXIT_SUCCESS);
}

void init(int **x, int rows, int columns, int value) {
    int i, j;

    for (i = 0; i < rows; i++) {
        for (j = 0; j < columns; j++) {
            x[i][j] = value;
        }
    }
}

void initVector(int *x, int size, int value) {
    int i;

    for (i = 0; i < size; i++) {
        x[i] = value;
    }
}

void verify(int *actual, int rowCount, int columnCount, int vectorValue) {
    int i;
    int expectedValue = columnCount * vectorValue;

    for (i = 0; i < rowCount; i++) {
        if (expectedValue != actual[i]) {
            fprintf(stdout, "Error in array at index %d, value is %d but should be %d\n", i, actual[i], expectedValue);
            break;
        }

    }
}
