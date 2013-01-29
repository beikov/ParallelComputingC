/opt/NECmpi/gcc/inst/bin64/mpicc -o prefixsum -O3 prefixsum.c
/bin/cp /dev/null data.csv

ELEMENTS_STEP=25000000
ELEMENTS=200000000

for PROCESSES in {1,8,16}
do
	for NODES in {1,18,36}
	do
		PARAMS=""
		for (( j = 0; j < NODES; j++ ))
		do
			PARAMS+=" -host jupiter"
			PARAMS+=$j
			PARAMS+=" -np "
			PARAMS+=$PROCESSES
		done
		INFO=""
		INFO+=$NODES
		INFO+=" nodes, "
		INFO+=$PROCESSES
		INFO+=" processes"
		echo $INFO >> data.csv
		
		for (( e = ELEMENTS_STEP; e <= ELEMENTS; e+=ELEMENTS_STEP ))
		do
			"/opt/NECmpi/gcc/inst/bin64/mpirun" $PARAMS prefixsum $e >> data.csv
			if [ "$?" != "0" ]; then
				exit 1
			fi
		done
	done
done