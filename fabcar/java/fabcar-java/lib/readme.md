
需要先将lib文件夹下的sdk打进本地maven库
命令：
mvn install:install-file -DgroupId=org.hyperledger.fabric-sdk-java -DartifactId=fabric-sdk-java-gm -Dversion=1.4.7 -Dpackaging=jar -Dfile=./lib/fabric-sdk-java-gm-1.4.7.jar

mvn install:install-file -DgroupId=org.hyperledger.fabric -DartifactId=fabric-gateway-java-gm -Dversion=1.4.2 -Dpackaging=jar -Dfile=./lib/fabric-gateway-java-gm-1.4.2.jar

mvn dependency:purge-local-repository -DmanualInclude="org.hyperledger.fabric-sdk-java:fabric-sdk-java-gm,org.hyperledger.fabric.fabric-gateway-java-gm"
//第二步,阻止Maven对已删除的jar进行reResolve
mvn dependency:purge-local-repository -DreResolve=false