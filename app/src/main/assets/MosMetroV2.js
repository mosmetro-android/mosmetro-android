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

    /* https://stackoverflow.com/a/4588211 */
    function fullPath(el) {
        var names = [];
        while (el.parentNode) {
            if (el.id) {
                names.unshift('#' + el.id);
            } else {
                if (el == el.ownerDocument.documentElement) {
                    names.unshift(el.tagName.toLowerCase());
                } else {
                    var sel = el.tagName.toLowerCase();
                    var siblings = Array.apply([], el.parentElement.children).filter(function (e) {
                        return e.tagName == el.tagName;
                    }).length;
                    if (el.className) {
                        sel += "." + el.className.replace(/\s+/g, '.');
                    } else if (siblings > 1) {
                        for (var c = 1, e = el; e.previousElementSibling; e = e.previousElementSibling, c++);
                        sel += ":nth-child(" + c + ")";
                    }
                    names.unshift(sel);
                }
            }
            el = el.parentNode;
        }
        return names.join(" > ");
    }

    const IGNORE = [
        'img.pixel',
        '.disabled'
    ];

    const CLICK = [
        '.join',
        '.cross',
        '.mt-banner-fullscreen__button-close',
        '.interaction_button__joke',
        '.interaction_button_joke',
        '.interaction_button_quiz',
        '.interaction_button',
        '.button_blue',
        '.btn-join',
        '.mtt-fullscreen-skip',
        '.mtt-fullscreen-skip-btn'
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
            log("Click | " + fullPath(el));
            el.click();
        }
    }

    function onNodeChange(el) {
        if (el.matches(IGNORE)) {
            return;
        }

        var text = el.innerText.replace(/\n/g, ' ').trim();
        if (text.length > 0) log(text);

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
                if (m.type == 'attributes') {
                    log('Mutation (' + m.type + ') | ' + fullPath(m.target) +
                        ' | "' + m.attributeName + '": "' + m.oldValue + '" > "' +
                        m.target.getAttribute(m.attributeName) + '"');
                } else {
                    log('Mutation (' + m.type + ') | ' + fullPath(m.target));
                }
            }
            onNodeChange(m.target);
        });

        each('video', function (el) {
            el.pause();
        });
    }

    var o = new MO(onMutation);
    o.observe(document.body, {
        childList: true,
        attributes: true,
        subtree: true,
        attributeOldValue: true
    });

    log('Loaded successfully');
})();