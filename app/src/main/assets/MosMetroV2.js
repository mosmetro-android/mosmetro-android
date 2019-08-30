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

(function () {
    const MO = window.MutationObserver || window.WebKitMutationObserver;

    const IGNORE = [
        'img.pixel'
    ];

    const CLICK = [
        '.join',
        '.cross',
        '.mt-banner-fullscreen__button-close',
        '.interaction_button__joke',
        '.interaction_button_quiz',
        '.interaction_button',
        '.button_blue'
    ];

    function log(msg) {
        console.log('MosMetroV2.js | ' + msg.replace(/\n/, ''))
    }

    function each(query, f) {
        [].forEach.call(document.querySelectorAll(query), f);
    }

    function click(el) {
        /* click only visible elements */
        var style = window.getComputedStyle(el);
        if (el.offsetParent !== null && style.visibility != 'hidden') {
            log("Click | " + el.outerHTML);
            el.click();
        }
    }

    function onNodeChange(el) {
        if (el.matches(IGNORE)) {
            return;
        }

        if (el.matches(CLICK)) {
            setTimeout(function () { click(el); }, 500);
        }

        el.querySelectorAll(CLICK).forEach(function (child) {
            setTimeout(function () { click(child); }, 500);
        });
    }

    function onMutation(ml, o) {
        ml.forEach(function (m) {
            if (m.target.outerHTML !== undefined) {
                log('Mutation (' + m.type + ') | ' + m.target.outerHTML);
            }
            onNodeChange(m.target);
        });

        each('video', function (el) {
            el.pause();
        });
    }

    if (document.location.pathname.lastIndexOf('/auth', 0) !== 0) {
        log('Wrong page: ' + document.location);
    }

    var o = new MO(onMutation);
    o.observe(document.body, {
        childList: true,
        attributes: true,
        subtree: true
    });

    log('Loaded successfully');
})()