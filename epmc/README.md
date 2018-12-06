The scripts contained in this directory, used to run EPMC and parse the generated logs, expect to find
* epmc-standard.jar file in this directory
* logs directory in this directory
* all benchmarks listed in the experiments tables to be present in the benchmarks directory, with no sub-directories

The two JAVA projects generate the experiments benchmark tables and the table with the reference values.
Both expect to be called as ```java -jar qcomp.jar index.json...``` and generate their file(s) in the same directory where they have been called.

