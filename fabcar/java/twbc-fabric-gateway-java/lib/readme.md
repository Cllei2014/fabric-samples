
需要先将lib文件夹下的sdk打进本地maven库
命令：
mvn install:install-file -DgroupId=org.hyperledger.fabric-sdk-java -DartifactId=fabric-sdk-java-gm -Dversion=1.4.7 -Dpackaging=jar -Dfile=./lib/fabric-sdk-java-1.4.7.jar
