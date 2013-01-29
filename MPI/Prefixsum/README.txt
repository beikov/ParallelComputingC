Compile with

/opt/NECmpi/gcc/inst/bin64/mpicc -o prefixsum -O3 prefixsum.c

Run with

/opt/NECmpi/gcc/inst/bin64/mpirun -np 4 ./prefixsum

Compile and Test with

./test.sh