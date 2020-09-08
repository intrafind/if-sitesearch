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

require_once 'searchbar.php';

/**
 * Get site url
 *
 * @return site url
 */
function sis_get_site_url()
{
    if (get_option("if_sis_url_for_crawling")) {
        $if_sis_url_for_crawling = esc_url_raw(get_option("if_sis_url_for_crawling"));
    } else {
        $if_sis_url_for_crawling = get_site_url();
    }
    return $if_sis_url_for_crawling;
}

// actions
if( current_user_can( 'manage_options' ) && isset( $_POST[ 'submit-form' ],  $_POST[ 'create-saveSetup' ] ) && wp_verify_nonce( $_POST[ 'submit-form' ], 'sis-nonce' ) ){
    sis_save_options();
}

if( current_user_can( 'manage_options' ) && isset( $_POST[ 'submit-form' ],  $_POST[ 'sis-removeSetup' ] ) && wp_verify_nonce( $_POST[ 'submit-form' ], 'sis-nonce' ) ){
    sis_delete_options();
}

/**
 * Create & update database settings fields
 *
 * @return sis-url, sis-siteId and sis-siteSecret
 */
function sis_save_options()
{
    //get data from fields
    $if_sis_url_for_crawling = esc_url_raw($_POST['sis-url']);
    $if_sis_siteId = sanitize_text_field($_POST['sis-siteId']);
    $if_sis_siteSecret = sanitize_text_field($_POST['sis-siteSecret']);
    $sis_cssSelector = sanitize_text_field($_POST['sis-cssSelector']);
    if (!get_option("if_sis_url_for_crawling")) {
        update_option("if_sis_url_for_crawling", $if_sis_url_for_crawling);
    } else {
        update_option("if_sis_url_for_crawling", $if_sis_url_for_crawling);
    }
    if (!get_option("if_sis_siteId")) {
        update_option("if_sis_siteId", $if_sis_siteId);
    } else {
        update_option("if_sis_siteId", $if_sis_siteId);
    }
    if (!get_option("if_sis_siteSecret")) {
        update_option("if_sis_siteSecret", $if_sis_siteSecret);
    } else {
        update_option("if_sis_siteSecret", $if_sis_siteSecret);
    }
    if (!get_option("sis_cssSelector")) {
        update_option("sis_cssSelector", $sis_cssSelector);
    } else {
        update_option("sis_cssSelector", $sis_cssSelector);
    }
	
	echo '<meta http-equiv="refresh" content="0">';
}

function sis_delete_options()
{
    delete_option("if_sis_url_for_crawling");
    delete_option("if_sis_siteId");
    delete_option("if_sis_siteSecret");
    delete_option("sis_cssSelector");
}

function sis_set_safe_css_selector()
{
    if (get_option("sis_cssSelector")) {
        $sis_cssSelector = get_option("sis_cssSelector");
    } else {
        $sis_cssSelector = "#search-2";
    }
    return $sis_cssSelector;
}
?>


<style>
    .form-wrapper input {
        width: 500px;
    }
</style>
<div class="form-wrapper" style="width: 500px;">
    <form method="POST">
        <h1>Site Search Setup</h1>
        Website URL: <input type="text" id="sis-url" name="sis-url" value="<?php echo sis_get_site_url(); ?>">
        <br><br>
        Site ID: <input type="text" id="sis-siteId" name="sis-siteId" 
                        value="<?php echo get_option("if_sis_siteId"); ?>">
        <br><br>
        Site Secret: <input type="text" id="sis-siteSecret" name="sis-siteSecret" readonly
                            value="<?php echo get_option("if_sis_siteSecret"); ?>">
        <br><br>
        <p>Append the Site Search searchbar to the following CSS selector.
            It is recommended to <strong>provide the CSS selector of the container of the default WordPress
                searchbar</strong>
            which might depend on the theme that is used.
            This CSS selector can be updated anytime, to reflect your theme's specific layout.
            Providing an invalid CSS selector disables the searchbar.
        </p>
        <input type="text" id="sis-cssSelector" name="sis-cssSelector"
               value="<?php echo sis_set_safe_css_selector(); ?>">
        <input type="submit" id="sis-save-setup" name="create-saveSetup" 
               value="Save Site Search Setup"
               style="display: none;">
        <!--        TODO add "Reset Site Search setup" button-->
        <input type="submit" id="sis-remove-setup" name="sis-removeSetup"
               value="Remove Site Search Setup"
               style="display: none;">
		<?php
			wp_nonce_field( 'sis-nonce', 'submit-form' );
		?>
    </form>
    <br><br>
    <p>Using this Site Search plugin, you accept our
        <a href="https://sitesearch.cloud/terms" title="Terms &amp; Conditions" target="_blank">terms &amp;
            conditions</a>.
    </p>
    <input type="submit"
           name="crawl" value="Crawl your site and save your Site Search setup"
           onclick="SiSregisterSite();"
        <?php if (get_option("if_sis_siteId")) echo "style='display: none'"; ?>>
    <div id="triggerCrawler">
        <p id="sis-status"></p>
    </div>
    <input type="submit"
           value="Update CSS selector for the searchbar"
           onclick="document.getElementById('sis-save-setup').click();"
        <?php if (!get_option("if_sis_siteId")) echo "style='display: none'"; ?>>
    <br><br>
    <p>Search here before your visitors start searching...</p>
    <div id="searchbar"><?php echo sis_searchbar(); ?></div>
    <br><br>
    <div>
        <p
            <?php if (!get_option("if_sis_siteId")) echo "style='display: none'"; ?>>
            To <strong>start over and recrawl</strong> a new site, click the <strong>reset</strong> button below.
            This will purge all Site Search settings from WordPress, without affecting any other WordPress settings.
            If you just want to adjust the CSS selector, you do not need to reset these configuration settings
            but just update the CSS selector above.
        </p>
        <input type="submit" 
               value="Reset Site Search setup" 
               onclick="document.getElementById('sis-remove-setup').click();" 
            <?php if (!get_option("if_sis_siteId")) echo "style='display: none'"; ?>>
    </div>    
    <script>
        var hiddenSiSsearchbar = document.querySelector("#sitesearch-searchbar");
        hiddenSiSsearchbar.style.display = "block";
    </script>
</div>