<img src="https://api.sitesearch.cloud/theme/logo.png" alt="Site Search" width="600" style="max-width:100%;">

[![Instant Feedback](https://badges.gitter.im/gitterHQ/gitter.svg)](https://gitter.im/sitesearch/Lobby)
[![Travis CI Build Status](https://travis-ci.org/intrafind/if-sitesearch.svg)](https://travis-ci.org/intrafind/if-sitesearch)
[![TeamCity CI Build Status](https://ci.sitesearch.cloud/app/rest/builds/buildType(id:IntraFind_Oss_Build)/statusIcon)](https://ci.sitesearch.cloud/viewType.html?buildTypeId=IntraFind_Oss_Build)


Site Search API Service
=
Site Search Dispatcher is an upstream service of [Site Search](https://api.sitesearch.cloud), 
an on-demand SaaS offering for providing websites with an externally managed search capability. 

# About

* [API](https://api.sitesearch.cloud/swagger-ui.html)
* [Documentation](https://github.com/intrafind/if-sitesearch/blob/master/docs/faq.md)
    
***> > > [Try out!](https://sitesearch.cloud/getting-started) < < <***    
    
# Local configuration

Add a [configuration profile](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-profile-specific-properties) 
to the `config` folder inside this project.   

# OSS community contributions are appreciated 🤓☺️ 

* **WordPress Connector** wanted
    * ...to index exported WordPress dumps (XML) via Site Search REST API  
* **Google Drive Connector** wanted
    * ...to find things better in this new kind of "share"

# Operations

## Run 
    ./set-local-dev-dummy-env.sh # to set dummy values for mandatory environment variables 
    ./gradlew build -x test # just upon initial execution 
    ./run.sh
    http://localhost:8001
    
## Test
    ./test.sh

## Test throughput
    ./load-test.sh

## Build browser clients
    ./gradlew build -x test # just upon initial execution
    ./js-app-build.sh

## Release
    ./release.sh
   
## Set CDN Metadata
* gsutil cors set cdn-cors-configuration.json gs://site-search-europe
* gsutil setmeta  -h "content-encoding"  gs://site-search-europe/searchbar/2018-04-06/app/css/app.css
* gsutil -m setmeta -r -h "Content-Encoding: gzip"  gs://site-search-europe/searchbar/2018-04-06
* gsutil -m acl -r ch -u AllUsers:READER gs://site-search-europe/searchbar/2018-04-06
* gsutil -m iam -r ch allUsers:objectViewer gs://site-search-europe/test5
* gsutil -m cp -r ./2018-04-06 gs://site-search-europe/test3
* gsutil -m rm -r gs://site-search-europe/test1
    
# Attribution
* Made with ♥ in Munich
