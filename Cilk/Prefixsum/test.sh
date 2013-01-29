cp /dev/null data.csv
for i in {1..52}; do ./prefixsum --nproc $i >> data.csv; done