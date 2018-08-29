/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 * Copyright © 2015 Dmitry Karikh <the.dr.hax@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

(function() {
    const MO = window.MutationObserver || window.WebKitMutationObserver;

    function each(query, f) {
        [].forEach.call(document.querySelectorAll(query), f);
    }

    function click(query) {
        each(query, function(el) {
            if (el.offsetParent !== null) /* click only visible elements */
                el.click();
        });
    }

    function onMutation(m, o) {
        click('.join');
      	click('.cross');
        click('.mt-banner-fullscreen__button-close');
        click('.interaction_button__joke');
        click('.interaction_button');

        each('video', function(el) {
            el.pause();
        });
    }

    if (document.location.pathname.lastIndexOf('/auth', 0) !== 0) {
        return 'Wrong page: ' + document.location;
    }

    var o = new MO(onMutation);
    o.observe(document.body, {
        childList: true,
        attributes: true,
        subtree: true
    });

    onMutation(); /* In case if Observer is loaded too late */

    return "MosMetroV2.js loaded"; /* Do not touch this! */
})()