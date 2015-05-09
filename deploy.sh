lein ring uberwar
rm war -r
    unzip -d war target/proxy-0.1.0-SNAPSHOT-standalone.war 
    ~/appengine-java-sdk-1.9.20/bin/appcfg.sh update war/
