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

package pw.thedrhax.mosmetro.updater;

public class URLs {
    // GitHub Pages
    public static final String GITHUB = "https://thedrhax.github.io/mosmetro-android";
    public static final String NEWS_URL = GITHUB + "/news.json";
    public static final String API_URL_SOURCE = GITHUB + "/base-url";

    // Default stat URL
    public static final String API_URL_DEFAULT = "http://wi-fi.metro-it.com";

    // Relative URLs for stat server
    public static final String API_REL_STATISTICS = "/api/v1/statistics.php";
    public static final String API_REL_BRANCHES = "/api/v1/branches.php";
}
