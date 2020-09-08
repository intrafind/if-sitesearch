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

function sis_searchbar()
{
    echo '<div id="sitesearch-searchbar" class="searchbar" style="display: none;">
        <div id="ifs-searchbar" class="ifs-component ifs-sb"></div>
        </div>';
}

function sis_apply_transporter_cookies()
{
    setcookie("sis-siteId", get_option("if_sis_siteId"));
    $cookieSafeCssSelector = base64_encode(get_option("sis_cssSelector"));
    setrawcookie("sisDefaultWordPressSearchbarSelector", $cookieSafeCssSelector);
}

sis_apply_transporter_cookies();
add_action('wp_footer', 'sis_searchbar');