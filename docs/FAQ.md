FAQ
=

# Site Search Service

* What is Site Search?
    * Site Search is an on-demand **SaaS** offering from IntraFind Software AG to enable website operators 
    to provide search functionality for their websites. Hereby Site Search is run and operated by IntraFind 
    whereas you simply integrate the provided Searchbar into your website to use our service.

* What ist the **advantage of using Site Search** vs using search backends like *Elasticsearch* directly? -
    * Site Search removes Elasticsearch as your direct dependency when introducing a search to a website. 
    In fact Site Search does not only rely on Elasticsearch but on additional components 
    from *IntraFind* that **improve the quality of search results**. Also Site Search is a **managed service** that 
    removes the burden of operations from your organization. Site Search provides you with a simple API
    that is **tailored** for the purpose to provide a website search with all its specifics. 

* What is the difference between **sites** and **pages**?
    * A **site** is a *website* that can contain thousands of **pages**. 
    You may have two **sites**: *en.example.com* and *de.example.com* 
    and each may contain thousands of pages in the respective language.

* What is a **tenant**?
    * A tenant is an **organizational entity** that can manage **dozens of independent sites**.
    You may have a company that is assigned to a tenant.

* What is Site Search' primary API endpoint URL?
    * https://api.sitesearch.cloud should be used for all Site Search API calls.
    
* How can I index a website and make it searchable?
  * Currently a website needs to expose an **RSS / Atom feed** to make it easily searchable.
  This is the fastest & easiest way to get up and running. Use this ***curl command***
  
        curl -X POST https://api.sitesearch.cloud/sites/rss?feedUrl=https%3A%2F%2Fintrafind.de%2Fshare%2Fenterprise-search-blog.xml
  
  ...to [call our API accordingly](https://api.sitesearch.cloud/swagger-ui.html#!/site45controller/indexNewRssFeedUsingPOST). 
  Still you can make a website searchable that does not expose any feeds by using our API and indexing a website page by page, 
  cf. "How can I index a page and make it searchable?".  
  
* How can I **create a new site** to add pages to this site and make them searchable? 
Or what should be the first thing, I need to do, to **get started** with Site Search?
    * After executing this ***curl command***, you will also obtain the *siteId* and *siteSecret* of the newly created site. 

            curl -v -X POST https://api.sitesearch.cloud/sites

* How can I **index a page**, within an **already existing site**, and make it searchable?
    * When a page with the provided URL already exists, the page will be updated with the given title & body.
    Execute this curl command with a *siteSecret* that corresponds to the provided *siteId*:
    
            curl -v -X PUT \
              'https://api.sitesearch.cloud/sites/22d7cbe6-78c3-4f54-ba40-79b881cbe568/pages?siteSecret=8a12c8db-7d49-4f03-b2e5-03870b4e1e48' \
              -H 'content-type: application/json' \
              -d '{
                "title": "Test Page",
                "body": "Test Content",
                "url": "https://example.com/page"
            }'

* How can I **update** an already indexed page?
    * When updating a page the URL in the payload assures that only the page with the provided URL is updated with a new tile and content.
    If the page with the provided URL does not exist in a site's index, the page is automatically added to the site index.
    Execute this curl command with a *siteSecret* that corresponds to the provided *siteId*:
    
            curl -v -X PUT \
              'https://api.sitesearch.cloud/sites/22d7cbe6-78c3-4f54-ba40-79b881cbe568/pages?siteSecret=8a12c8db-7d49-4f03-b2e5-03870b4e1e48' \
              -H 'content-type: application/json' \
              -d '{
                "title": "Test Page",
                "body": "Test Content",
                "url": "https://example.com/page"
            }'

* How can I **delete** an indexed page?
    * You need to provide a **siteSecret** that corresponds to the provided **siteId**,
    as well as a page's primary ID.
    
            curl -v -X DELETE \
              https://api.sitesearch.cloud/sites/d87fdcef-a84d-462d-ba84-61df9805536a/pages/0c42f3ef01536bd29510d7b4d178fc7e6cbc1d26095ac3a759bf638f80bfa3c9?siteSecret=b554a1e7-3e87-44ab-b353-f1fd8a423bbe 

* What is a **site secret**?
    * A site secret is a token that you should keep confidential and not publish it to any public repository. 
    The site secret enables you to **add new pages and update existing pages** within a site. 
    You need to provide a *siteSecret* as a query parameter to authorize operations that modify the index of a site.

* How can I make my website searchable?
    * The easiest & quickest way is to use the [Site Search Gadget](https://sitesearch.cloud/getting-started/)
    to **create an index** from an RSS / Atom feed and make it **instantly searchable** without even embedding it into a website.
    If your website does not expose any feeds, you need to use our open [API](https://api.sitesearch.cloud/swagger-ui.html#!/page45controller/indexNewSiteUsingPOST)
    to **index your website page by page** calling the API.
    
* Where can I find an API documentation and specification for Site Search?
    * [Site Search' API](https://api.sitesearch.cloud/swagger-ui.html) ist technically documented according 
    to the OpenAPI 3.0 specification, using Swagger. Not only, you can lookup the required schemas 
    but you can also, use the API right from your browser. 

* What does Site Search cost?
    * Site Search offers a [variety of subscription plans](https://sitesearch.cloud/pricing) that should fit sites of every size.

* I do not want to use Site Search anymore. Do I have to delete my data?
    * You do not have to do anything. We will automatically delete your indexed data after a certain time of inactivity.
    We would appreciate to [learn the reasons](mailto:feedback@sitesearch.cloud) behind your decision to stop using our service. 
    
* After using the *Site Search Gadget* I get search results that do not correspond to the specified *siteId*.
    * Delete your browser's cookies for *api.sitesearch.cloud* and specifically the "override-site" cookie.
 
* Does Site Search support categories / labels / tags?
    * Categories as they are supported by Site Search are a flat labels list, i.e. a page can belong to many
    categories and a category may contain many pages but no subcategories categories. A category is assigned 
    to a page at indexation time. There many ways to assign a page to a category. Currently we support assigning
    categories to a page via `robots.txt` declarations. Please reach out to us, to describe us the way, you want to
    assign categories to pages.
     
* Who is using Site Search?
    * The following website's search is powered by Site Search
        * [IntraFind Software AG](https://www.intrafind.de)
        * [IntraFind Inc.](https://www.intrafind.com)
        * [Analyze Law](https://www.analyzelaw.com)
        * ...and many more. Just look at the bottom of [this page](https://sitesearch.cloud).
   
# Crawler 

* Sitemaps support
    * The crawler can either crawl a site using its sitemaps or using its links. 
    Nested sitemaps are supported as well.
    
* What is the impact on my web server when Site Search crawls my site to index their pages?
    * The [Getting Started Gadget](https://sitesearch.cloud/getting-started/) 
    crawls a site with a speed of around *two pages per second*. 
    Even very moderate web servers are able to handle this load. The *regular recrawls for subscribed customers
    happen with around the same speed*, except for very huge websites, with more than 10k pages, where we can increase
    the recrawling speed to 100 pages per second or even more. However in this case we are going to notify 
    a service subscriber and provide our subscribers with a speed their web servers can handle without any disruption to their customers.     

* Content Extraction Selector
    * This is a feature that gives you the opportunity to provide a CSS selector that is used
    to extract a page's content. Instead of indexing everything in a page's body, one can limit
    the content extraction to a subsection of a page. If the selector cannot match anything,
    the crawling mechanism falls back to page `body` extraction.

* Does the crawler support **dynamic, JavaScript generated content**?
    * Like most (all?) other crawlers, the Site Search crawler cannot extract runtime-generated content.
    A common practice is to provide the crawler with **HTML snapshots** of the page content you want to be crawled.  
    
* Can the crawler access sites or pages that require **authentication**, e.g. Basic Auth protected sites?
    * **Not yet**, but if this feature is frequently requested, we might consider to implement this functionality.
   
# Search Bar

* Do I need the **search bar** to use Site Search?
    * No, not necessarily; the **search bar is a sample implementation** to quickly show you how 
    a search bar may look like and interact with the *Site Search service*.
    Still, the **search bar** is a production-grade implementation and ready for production deployments.

* Can I use my own search bar with Site Search?
    * Yes, you can use any search bar, written in any language that also runs on native platforms as long as it follows
    [Site Search' API specification](https://api.sitesearch.cloud/swagger-ui.html).

* Can I *customize* the layout and/or design of the search bar?   
    * Yes, you can *fully customize the appearance of the search bar* [using HBS templates](https://api.sitesearch.cloud/searchbar/Site%20Search%20Searchbar%205.2_tech_V1_ENG.pdf).
    * Alternative you can customize the search bar overriding its CSS classes which is appropriate for minor customizations only.

* Can I *customize* the highligthed words within the search snippet previews?
    * To customize the highligthed words, you need to override the **if-teaser-highlight** CSS class with you desired definitions.

* How do I integrate Site Search' search bar into my website?
    * Embed the following HTML fragment into your website and **adjust the siteId** provided in the snippet:
    
        
            <div id="sitesearch-searchbar" class="searchbar">
                <div id="ifs-searchbar" class="ifs-component ifs-sb"></div>
                <script src="https://cdn.sitesearch.cloud/searchbar/2018-07-18/app/js/app.js"></script>
                <script>
                    IFS.initClient({
                        customConfig: {
                            overwrite: {
                                "appLang": "en"
                            }
                        },
                        configurl: "https://cdn.sitesearch.cloud/searchbar/2018-07-18/config/sitesearch.json",
                        siteId: "4bcccea2-8bcf-4280-88c7-8736e9c3d15c"
                    });
                </script>
            </div> 

* I want to change the *tooltips* provided by the search bar. Does the search bar support **i18n / l10n**?
    * Yes, you need to download the [search bar](https://github.com/intrafind/if-sitesearch/tree/master/service/src/main/resources/static/searchbar/2019-01-31.zip), adjust the translations inside `searchbar/data/language/` and replace 
    `https://api.sitesearch.cloud` inside the [configuration folder](https://github.com/intrafind/if-sitesearch/tree/master/service/src/main/resources/static/searchbar/2019-01-31/config) with your new location. 
    
* How can I override search bar's *default language selection mechanism*?
    * The below example specifies German as search bar's user interface language. 
    
            <div id="sitesearch-searchbar" class="searchbar">
                <div id="ifs-searchbar" class="ifs-component ifs-sb"></div>
                <script src="https://cdn.sitesearch.cloud/searchbar/2018-07-18/app/js/app.js"></script>
                <script>
                    IFS.initClient({
                        customConfig: {
                            overwrite: {
                                "appLang": "de"
                            }
                        },
                        configurl: "https://cdn.sitesearch.cloud/searchbar/2018-07-18/config/sitesearch.json",
                        siteId: "4bcccea2-8bcf-4280-88c7-8736e9c3d15c"
                    });
                </script>
            </div>
    
