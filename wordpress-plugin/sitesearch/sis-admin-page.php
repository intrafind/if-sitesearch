<?php
/**
 * Copyright 2018 IntraFind Software AG. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

$if_sis_url_for_crawling = get_site_url();
// actions
if (isset($_POST['crawl'])) {
    update_option("if_sis_url_for_crawling", $if_sis_url_for_crawling);
    update_option("if_sis_siteId", $if_sis_siteId);
    update_option("if_sis_siteSecret", $if_sis_siteSecret);
}
if (isset($_POST['create'])) {
    createSiS_Options_WP_DB();
}
if (isset($_POST['read'])) {
    readSiS_Options_WP_DB();
}
if (isset($_POST['update'])) {
    updateSiS_Options_WP_DB();
}
if (isset($_POST['delete'])) {
    deleteSiS_Options_WP_DB();
}

// functions
function createSiS_Options_WP_DB()
{
    $if_sis_siteId = "3bbf4db0-85ab-11e8-8c2f-3fec88e4efa0";
    $if_sis_siteSecret = "4671e29a-85ab-11e8-9206-4b12904e274a";
    if (!get_option("if_sis_url_for_crawling")) {
        update_option("if_sis_url_for_crawling", $if_sis_url_for_crawling);
    }
    if (!get_option("if_sis_siteId")) {
        update_option("if_sis_siteId", $if_sis_siteId);
    }
    if (!get_option("if_sis_siteSecret")) {
        update_option("if_sis_siteSecret", $if_sis_siteSecret);
    }
}

function readSiS_Options_WP_DB()
{
    $if_sis_url_for_crawling = get_option("if_sis_url_for_crawling");
    $if_sis_siteId = get_option("if_sis_siteId");
    $if_sis_siteSecret = get_option("if_sis_siteSecret");
    echo "Aktuellen Werte:" . $if_sis_url_for_crawling . "<br>" . $if_sis_siteId . "<br>" . $if_sis_siteSecret;
}

function updateSiS_Options_WP_DB()
{
    $if_sis_siteId = "my-site-id";
    $if_sis_siteSecret = "my-site-secret";
    // update db with new values
    update_option("if_sis_siteId", $if_sis_siteId);
    update_option("if_sis_siteSecret", $if_sis_siteSecret);
    $if_sis_siteId = get_option("if_sis_siteId");
    $if_sis_siteSecret = get_option("if_sis_siteSecret");
    echo "Neue Werte:" . $if_sis_url_for_crawling . "<br>" . $if_sis_siteId . "<br>" . $if_sis_siteSecret;
}

function deleteSiS_Options_WP_DB()
{
    delete_option("if_sis_url_for_crawling");
    delete_option("if_sis_siteId");
    delete_option("if_sis_siteSecret");
}

?>

<script src="https://api.sitesearch.cloud/external/wordpress-plugin/admin-client.js"></script>

<!-- TODO load this script everywhere the default WordPress searchbar is loaded -->
<!--<script src="https://api.sitesearch.cloud/external/wordpress-plugin/searchbar-injection.js"></script>-->

<div class="form-wrapper">
    <style>
        .form-wrapper {
            width: 500px;
            clear: both;
        }

        .form-wrapper input {
            width: 100%;
            clear: both;
        }
    </style>
    <h1>Configuration</h1>
    Website URL: <input type="text" id="sis-url" name="sis-url" value="<?php echo $if_sis_url_for_crawling; ?>">
    <br><br>
    Site ID: <input type="text" id="sis-siteId" name="sis-siteId" value="<?php echo $if_sis_siteId; ?>">
    <br><br>
    Site Secret: <input type="text" id="sis-siteSecret" name="sis-siteSecret"
                        value="<?php echo $if_sis_siteSecret; ?>">
    <br><br>
    <input type="submit"
           name="crawl" value="Add Site Search searchbar to your site &amp; crawl your site's content."
           onclick="registerSiteInSiS();">
    <br><br>
    <p id="sis-status"></p>
    <!--    <input type="submit" name="create" value="Create DB Fields and initialize ...">-->
    <!--    <br><br>-->
    <!--    <input type="submit" name="read" value="Read site credentials">-->
    <!--    <input type="submit" name="update" value="Update site credentials">-->
    <!--    <input type="submit" name="delete" value="Delete DB Fields">-->


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
                siteId: "3a5dfd07-a463-45f8-863b-dfc3c9f09152"
            });
        </script>
    </div>
</div>