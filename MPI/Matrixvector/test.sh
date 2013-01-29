/opt/NECmpi/gcc/inst/bin64/mpicc -o matrixvector -O3 matrixvector.c
/bin/cp /dev/null data-scatter.csv
/bin/cp /dev/null data-gather.csv

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
		
		for rows in {100,500,1000,5000}
		do
			for cols in {100,500,1000,5000}
			do
				"/opt/NECmpi/gcc/inst/bin64/mpirun" $PARAMS matrixvector $rows $cols -scatter >> data-scatter.csv
				if [ "$?" != "0" ]; then
					break
				fi
			done
		done
	done
done

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
		
		for rows in {100,500,1000,5000}
		do
			for cols in {100,500,1000,5000}
			do
				"/opt/NECmpi/gcc/inst/bin64/mpirun" $PARAMS matrixvector $rows $cols -gather >> data-gather.csv
				if [ "$?" != "0" ]; then
					break
				fi
			done
		done
	done
done