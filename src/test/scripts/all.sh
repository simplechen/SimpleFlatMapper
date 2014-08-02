mkdir -p target/perf/benchmarks
src/test/scripts/dynamicJdbcMapperForEach.sh > target/perf/benchmarks/dynamicJdbcMapperForEach.txt
src/test/scripts/dynamicJdbcMapperMap.sh > target/perf/benchmarks/dynamicJdbcMapperMap.txt
src/test/scripts/hibernateStatefull.sh > target/perf/benchmarks/hibernateStatefull.txt
src/test/scripts/hibernateStatefullWithCache.sh > target/perf/benchmarks/hibernateStatefullWithCache.txt
src/test/scripts/hibernateStateless.sh > target/perf/benchmarks/hibernateStateless.txt
src/test/scripts/mybatis.sh > target/perf/benchmarks/mybatis.txt
src/test/scripts/reference.sh > target/perf/benchmarks/reference.txt
src/test/scripts/staticJdbcMapperMap.sh > target/perf/benchmarks/staticJdbcMapperMap.txt
