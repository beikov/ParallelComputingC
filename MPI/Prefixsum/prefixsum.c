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

void init(int *x, int j);
void verify(int *x, int j);
int scanIterativeLocal(int *x, int size);
void sumIterativeLocal(int *x, int *y, int taskId, int size);

int scanIterativeLocal(int *x, int size) {
    int i;

    for (i = 1; i < size; i++) {
        x[i] += x[i - 1];
    }

    return x[size - 1];
}

void sumIterativeLocal(int *x, int *y, int rank, int size) {
    int i;
    int xSum = 0;

    for (i = 0; i <= rank - 1; i++) {
        xSum += y[i];
    }

    for (i = 0; i < size; i++) {
        x[i] += xSum;
    }
}

int main(int argc, char** argv) {
    int rank, size;
    int arraySize, chunkSize;
    int *array, *chunk, *y;
    int localHigh, i, elements;
    double start, stop;
    MPI_Request *requests;
    MPI_Status *status;

    if (MPI_Init(&argc, &argv) != MPI_SUCCESS) {
        fprintf(stderr, "Unable to initialize MPI!\n");
        return -1;
    }
    
    if(argc != 2 || (elements = atoi(argv[1])) == 0) {
        fprintf(stdout, "No element count parameter given!\n");
        return -1;
    }

    // get rank and size from communicator
    MPI_Comm_size(MPI_COMM_WORLD, &size);
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);



    MPI_Barrier(MPI_COMM_WORLD);
    arraySize = elements - elements % size;
    chunkSize = arraySize / size;

    if (rank == 0) {
        requests = (MPI_Request*) malloc(sizeof (MPI_Request) * (size - 1));
        status = (MPI_Status*) malloc(sizeof (MPI_Status) * (size - 1));
        array = (int*) malloc(sizeof (int) * arraySize);
        init(array, arraySize);
        start = MPI_Wtime();
    }

    y = (int*) malloc(sizeof (int) * size);
    chunk = (int*) malloc(sizeof (int) * chunkSize);

    MPI_Scatter(array, chunkSize, MPI_INT,
            chunk, chunkSize, MPI_INT, 0, MPI_COMM_WORLD);

    // Compute local prefix sums
    localHigh = scanIterativeLocal(chunk, chunkSize);

    if (rank != 0) {
        MPI_Send(&localHigh, 1, MPI_INT, 0, PREFIXSUM_TAG, MPI_COMM_WORLD);
    } else {
        y[0] = localHigh;

        for (i = 1; i < size; i++) {
            MPI_Irecv(&y[i], 1, MPI_INT, i, PREFIXSUM_TAG, MPI_COMM_WORLD, &requests[i - 1]);
        }

        MPI_Waitall(size - 1, requests, status);
    }

    MPI_Bcast(y, size, MPI_INT, 0, MPI_COMM_WORLD);

    if (rank != 0) {
        sumIterativeLocal(chunk, y, rank, chunkSize);
    }

    MPI_Gather(chunk, chunkSize, MPI_INT,
            array, chunkSize, MPI_INT, 0, MPI_COMM_WORLD);

    if (rank == 0) {
        stop = MPI_Wtime();
        fprintf(stdout, "%d;%d;%f\n", size, arraySize, stop - start);
        verify(array, arraySize);
    }

    free(chunk);

    if (rank == 0) {
        free(requests);
        free(array);
    }

    MPI_Finalize();
    fflush(stdout);
    return (EXIT_SUCCESS);
}

void init(int *x, int j) {
    int i;

    for (i = 0; i < j; i++) {
        x[i] = i + 1;
    }
}

void verify(int *x, int j) {
    int i;
    int sum = 0;

    for (i = 0; i < j; i++) {
        sum += i + 1;

        if (x[i] != sum) {
            fprintf(stdout, "Error in array at index %d, value is %d but should be %d\n", i, x[i], sum);
            break;
        }

    }
}
