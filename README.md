# Web Proxies

Web proxies for Google App Engine and local machine. 

## Deploy

* Google App Engine Proxy
    * Fill in the app id and version fields in *war-resources/appengine-web.xml*
    * sh deploy.sh
* Local Proxy    
    * lein uberjar
    * java -jar *target/proxy-X-X.jar* [port-number (default 9999)]

## Usage

* Send any requests (GET, POST, PUT, etc.) to /proxy?url={THE DESIRED URL}
* All the headers and body will be redirected to and from the target web page.

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
